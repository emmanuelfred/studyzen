package com.fredrickemmanuel.studyzen.data.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.fredrickemmanuel.studyzen.core.time.currentTimeMillis
import com.fredrickemmanuel.studyzen.data.KeyValueStore
import com.fredrickemmanuel.studyzen.data.StoreSnapshot
import com.fredrickemmanuel.studyzen.data.StudyStore
import com.fredrickemmanuel.studyzen.data.platformKeyValueStore
import com.fredrickemmanuel.studyzen.data.remote.ApiException
import com.fredrickemmanuel.studyzen.domain.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

private const val FULL_PULL_EVERY_MS = 24L * 60 * 60 * 1000
private const val QUIET_SYNC_MS = 60_000L
private const val BATCH = 500
private const val OFFLINE = "You are offline. Your changes are saved here and will sync when you are back online."

/** Remembers the sync bookkeeping on the device. */
class SyncStateStore(private val storage: KeyValueStore = platformKeyValueStore()) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): SyncState =
        try {
            storage.get(KEY)?.let { json.decodeFromString(SyncState.serializer(), it) } ?: SyncState()
        } catch (e: Exception) {
            SyncState()
        }

    fun save(state: SyncState) {
        try {
            storage.put(KEY, json.encodeToString(SyncState.serializer(), state))
        } catch (e: Exception) {
            // storage blocked: sync simply starts over next time
        }
    }

    private companion object { const val KEY = "sync.state.v1" }
}

/**
 * Keeps everything the learner has created in step with the server, so logging in on another phone
 * brings it all back. The app never depends on it: the phone always works from its own copy, and
 * this runs quietly whenever there is a login and a connection.
 *
 *  - Changes are found by comparing each item with how it looked at the last sync.
 *  - For each item the most recent edit wins; deletions travel as well.
 *  - Sample data is never sent.
 */
class SyncEngine(
    private val store: StudyStore,
    private val transport: SyncTransport,
    private val signedInUserId: () -> String?,
    private val states: SyncStateStore = SyncStateStore(),
    private val clock: () -> Long = ::currentTimeMillis,
) {
    var status by mutableStateOf<SyncStatus>(SyncStatus.Idle)
        private set

    /** How many edits on this phone have not reached the server yet. */
    var pendingChanges by mutableStateOf(0)
        private set

    private val mutex = Mutex()
    private var state: SyncState = states.load()
    private var lastExchangeAt = 0L

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // --- Account switching ---------------------------------------------------------------------

    /**
     * Call right after a login, before showing the app. Sample data is dropped, and if a different
     * account's data is still on this phone it is cleared so the two never mix.
     */
    fun prepareForLogin(userId: String) {
        val owner = state.owner
        if (owner != null && owner != userId) {
            store.clearAll()
            state = SyncState()
        } else if (owner == null && store.demoDataLoaded) {
            store.clearAll()
        }
        state = state.copy(owner = userId)
        states.save(state)
    }

    /** Removes this account's data from the phone (after logging out or deleting the account). */
    fun forgetLocalData() {
        store.clearAll()
        state = SyncState()
        states.save(state)
        pendingChanges = 0
        status = SyncStatus.Idle
    }

    // --- Syncing -------------------------------------------------------------------------------

    /** Syncs once. Returns true when nothing is left waiting on this phone. */
    suspend fun sync(force: Boolean = false): Boolean = mutex.withLock {
        val userId = signedInUserId()
        if (userId == null) {
            status = SyncStatus.Idle
            return@withLock false
        }
        if (state.owner != userId) prepareForLogin(userId)

        val now = clock()
        val firstTime = state.lastFullPullAt == 0L
        pendingChanges = countPending()
        if (!force && !firstTime && pendingChanges == 0 && now - lastExchangeAt < QUIET_SYNC_MS) return@withLock true

        status = SyncStatus.Syncing
        try {
            // A phone that has never synced first LISTENS: otherwise its blank settings could replace
            // the ones already saved by the learner's other phone.
            if (firstTime) {
                var more: Boolean
                do {
                    val response = transport.exchange(SyncRequest(state.cursor, emptyList()))
                    applyRemote(response.changes)
                    state = state.copy(cursor = response.cursor)
                    more = response.hasMore
                } while (more)
                state = state.copy(lastFullPullAt = now)
                states.save(state)
            } else if (now - state.lastFullPullAt > FULL_PULL_EVERY_MS) {
                state = state.copy(cursor = 0, lastFullPullAt = now) // a periodic full check catches anything missed
            }

            var entries = detectChanges(clock())
            var cursor = state.cursor
            do {
                val sending = state.items.filter { it.value.dirty }.entries.take(BATCH)
                val request = SyncRequest(
                    cursor = cursor,
                    changes = sending.map { (key, meta) -> toChange(key, meta, entries[key]) },
                )
                val response = transport.exchange(request)

                // what was sent is now on the server (unless it is edited again before the next check)
                val items = state.items.toMutableMap()
                sending.forEach { (key, meta) -> items[key] = meta.copy(dirty = false) }
                state = state.copy(items = items)

                applyRemote(response.changes)
                cursor = response.cursor
                state = state.copy(cursor = cursor)
                states.save(state)
                lastExchangeAt = clock()
                if (response.hasMore || state.items.any { it.value.dirty }) entries = detectChanges(clock())
            } while (response.hasMore || state.items.any { it.value.dirty })

            pendingChanges = countPending()
            status = SyncStatus.Synced(clock())
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            pendingChanges = countPending()
            status = if (e.isNetworkError) SyncStatus.Failed(OFFLINE, offline = true) else SyncStatus.Failed(e.message.orEmpty(), offline = false)
            false
        } catch (e: Exception) {
            pendingChanges = countPending()
            status = SyncStatus.Failed("Sync didn't finish. It will try again.", offline = false)
            false
        }
    }

    /** One last sync before logging out, so nothing on this phone is lost. */
    suspend fun finalSync(): Boolean = sync(force = true)

    // --- Finding changes -----------------------------------------------------------------------

    private class Entry(val collection: String, val id: String, val json: String)

    private fun keyOf(collection: String, id: String) = "$collection/$id"

    /** Compares the phone's data with the last sync and marks new, edited and deleted items. */
    private fun detectChanges(now: Long): Map<String, Entry> {
        val entries = flatten(store.snapshot()).associateBy { keyOf(it.collection, it.id) }
        val items = state.items.toMutableMap()
        for ((key, entry) in entries) {
            val h = hash(entry.json)
            val meta = items[key]
            if (meta == null || meta.deleted || meta.hash != h) {
                items[key] = SyncMeta(updatedAt = now, hash = h, deleted = false, dirty = true)
            }
        }
        for ((key, meta) in state.items) {
            if (key !in entries && !meta.deleted) items[key] = SyncMeta(updatedAt = now, hash = meta.hash, deleted = true, dirty = true)
        }
        state = state.copy(items = items)
        return entries
    }

    /** How many items differ from the last sync, without changing anything. */
    private fun countPending(): Int {
        val entries = flatten(store.snapshot()).associateBy { keyOf(it.collection, it.id) }
        var count = 0
        for ((key, entry) in entries) {
            val meta = state.items[key]
            if (meta == null || meta.deleted || meta.dirty || meta.hash != hash(entry.json)) count++
        }
        for ((key, meta) in state.items) {
            if (key !in entries && (!meta.deleted || meta.dirty)) count++
        }
        return count
    }

    private fun toChange(key: String, meta: SyncMeta, entry: Entry?): SyncChange {
        val (collection, id) = key.split('/', limit = 2)
        return if (meta.deleted || entry == null) {
            SyncChange(collection, id, data = null, updatedAt = meta.updatedAt, deleted = true)
        } else {
            SyncChange(collection, id, data = entry.json, updatedAt = meta.updatedAt)
        }
    }

    private fun isSample(id: String) = id.startsWith("demo-")

    private fun flatten(s: StoreSnapshot): List<Entry> {
        val out = mutableListOf<Entry>()
        s.studies.filter { !isSample(it.id) }.forEach { out += Entry("studies", it.id, json.encodeToString(com.fredrickemmanuel.studyzen.domain.Study.serializer(), it)) }
        s.sessions.filter { !it.isSample && !isSample(it.id) }.forEach { out += Entry("sessions", it.id, json.encodeToString(com.fredrickemmanuel.studyzen.domain.StudySession.serializer(), it)) }
        s.plans.filter { !isSample(it.id) }.forEach { out += Entry("plans", it.id, json.encodeToString(com.fredrickemmanuel.studyzen.domain.StudyPlan.serializer(), it)) }
        s.notes.filter { !isSample(it.id) }.forEach { out += Entry("notes", it.id, json.encodeToString(com.fredrickemmanuel.studyzen.domain.Note.serializer(), it)) }
        s.results.filter { !isSample(it.id) }.forEach { out += Entry("results", it.id, json.encodeToString(com.fredrickemmanuel.studyzen.domain.AssessmentResult.serializer(), it)) }
        s.research.filter { !isSample(it.id) }.forEach { out += Entry("research", it.id, json.encodeToString(com.fredrickemmanuel.studyzen.domain.ResearchItem.serializer(), it)) }
        s.chats.filter { !isSample(it.message.id) }.forEach { out += Entry("chats", it.message.id, json.encodeToString(com.fredrickemmanuel.studyzen.data.ChatEntry.serializer(), it)) }
        s.assessments.filter { !isSample(it.id) }.forEach { out += Entry("assessments", it.id, json.encodeToString(com.fredrickemmanuel.studyzen.domain.Assessment.serializer(), it)) }
        s.scans.filter { !isSample(it.id) }.forEach { out += Entry("scans", it.id, json.encodeToString(com.fredrickemmanuel.studyzen.domain.ScanRecord.serializer(), it)) }
        s.studyLog.filter { !isSample(it.id) }.forEach { out += Entry("log", it.id, json.encodeToString(com.fredrickemmanuel.studyzen.domain.StudyLogEntry.serializer(), it)) }
        s.favoriteWords.forEach { out += Entry("words", it.id, json.encodeToString(com.fredrickemmanuel.studyzen.domain.FavoriteWord.serializer(), it)) }
        // name and e-mail belong to the account, so only the preferences travel
        out += Entry("profile", PROFILE_ID, json.encodeToString(UserProfile.serializer(), s.profile.copy(name = "", email = "")))
        return out
    }

    // --- Applying changes from the server ------------------------------------------------------

    private fun applyRemote(changes: List<SyncChange>) {
        val items = state.items.toMutableMap()
        for (change in changes) {
            val key = keyOf(change.collection, change.id)
            val meta = items[key]
            // keep this phone's version when it is newer (it is sent on the next round)
            if (meta != null && meta.updatedAt >= change.updatedAt) continue
            val normalized = try {
                applyOne(change)
            } catch (e: Exception) {
                continue // an item this version of the app cannot read: leave it alone
            }
            items[key] = SyncMeta(
                updatedAt = change.updatedAt,
                hash = if (normalized == null) (meta?.hash ?: 0L) else hash(normalized),
                deleted = normalized == null,
                dirty = false,
            )
        }
        state = state.copy(items = items)
    }

    /** Puts one remote change into the store. Returns the item as this phone writes it, or null if deleted. */
    private fun applyOne(change: SyncChange): String? = when (change.collection) {
        "studies" -> upsert(store.studies, change, com.fredrickemmanuel.studyzen.domain.Study.serializer()) { it.id }
        "sessions" -> upsert(store.sessions, change, com.fredrickemmanuel.studyzen.domain.StudySession.serializer()) { it.id }
        "plans" -> upsert(store.plans, change, com.fredrickemmanuel.studyzen.domain.StudyPlan.serializer()) { it.id }
        "notes" -> upsert(store.notes, change, com.fredrickemmanuel.studyzen.domain.Note.serializer()) { it.id }
        "results" -> upsert(store.results, change, com.fredrickemmanuel.studyzen.domain.AssessmentResult.serializer()) { it.id }
        "research" -> upsert(store.research, change, com.fredrickemmanuel.studyzen.domain.ResearchItem.serializer()) { it.id }
        "words" -> upsert(store.favoriteWords, change, com.fredrickemmanuel.studyzen.domain.FavoriteWord.serializer()) { it.id }
        "assessments" -> upsert(store.assessments, change, com.fredrickemmanuel.studyzen.domain.Assessment.serializer()) { it.id }
        "scans" -> upsert(store.scans, change, com.fredrickemmanuel.studyzen.domain.ScanRecord.serializer()) { it.id }
        "log" -> upsert(store.studyLog, change, com.fredrickemmanuel.studyzen.domain.StudyLogEntry.serializer()) { it.id }
        "chats" -> upsert(store.chats, change, com.fredrickemmanuel.studyzen.data.ChatEntry.serializer()) { it.message.id }
        "profile" -> {
            val data = change.data
            if (change.deleted || data == null) {
                null
            } else {
                val remote = json.decodeFromString(UserProfile.serializer(), data)
                store.profile = remote.copy(name = store.profile.name, email = store.profile.email)
                json.encodeToString(UserProfile.serializer(), remote.copy(name = "", email = ""))
            }
        }
        else -> throw IllegalArgumentException("unknown collection")
    }

    private fun <T> upsert(
        list: SnapshotStateList<T>,
        change: SyncChange,
        serializer: KSerializer<T>,
        idOf: (T) -> String,
    ): String? {
        val index = list.indexOfFirst { idOf(it) == change.id }
        val data = change.data
        if (change.deleted || data == null) {
            if (index >= 0) list.removeAt(index)
            return null
        }
        val item = json.decodeFromString(serializer, data)
        if (index >= 0) list[index] = item else list.add(item)
        return json.encodeToString(serializer, item)
    }

    private fun hash(text: String): Long {
        var h = -3750763034362895579L // FNV-1a, 64 bit
        for (c in text) {
            h = h xor c.code.toLong()
            h *= 1099511628211L
        }
        return h
    }

    private companion object {
        const val PROFILE_ID = "me"
    }
}

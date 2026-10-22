package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.data.MemoryKeyValueStore
import com.fredrickemmanuel.studyzen.data.StudyStore
import com.fredrickemmanuel.studyzen.data.remote.ApiException
import com.fredrickemmanuel.studyzen.data.sync.SyncChange
import com.fredrickemmanuel.studyzen.data.sync.SyncEngine
import com.fredrickemmanuel.studyzen.data.sync.SyncRequest
import com.fredrickemmanuel.studyzen.data.sync.SyncResponse
import com.fredrickemmanuel.studyzen.data.sync.SyncStateStore
import com.fredrickemmanuel.studyzen.data.sync.SyncStatus
import com.fredrickemmanuel.studyzen.data.sync.SyncTransport
import com.fredrickemmanuel.studyzen.domain.KnowledgeLevel
import com.fredrickemmanuel.studyzen.domain.Note
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.domain.StudySession
import com.fredrickemmanuel.studyzen.domain.Subject
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The server's rules, in memory: newest edit wins, deletions are remembered, changes are numbered. */
private class FakeServer : SyncTransport {
    private val items = mutableMapOf<String, Pair<SyncChange, Long>>()
    private var seq = 0L
    var online = true

    val keys: Set<String> get() = items.keys

    override suspend fun exchange(request: SyncRequest): SyncResponse {
        if (!online) throw ApiException(null, "NETWORK", "offline")
        val accepted = mutableSetOf<String>()
        for (c in request.changes) {
            val key = "${c.collection}/${c.id}"
            val existing = items[key]
            if (existing != null && existing.first.updatedAt > c.updatedAt) continue
            seq++
            items[key] = c to seq
            accepted += key
        }
        val newer = items.filter { (key, v) -> v.second > request.cursor && key !in accepted }
        val cursor = items.values.filter { it.second > request.cursor }.maxOfOrNull { it.second } ?: request.cursor
        return SyncResponse(cursor, newer.values.map { it.first })
    }
}

class SyncEngineTest {
    private var time = 1_000L
    private fun tick(): Long { time += 1_000; return time }

    private fun study(id: String, title: String = "Biology") =
        Study(id, title, "", "", Subject.Science, KnowledgeLevel.Beginner, createdAtEpochDay = 20_000)

    private class Phone(val store: StudyStore, val engine: SyncEngine)

    private fun phone(server: FakeServer, user: String? = "u1", clock: () -> Long): Phone {
        val store = StudyStore().also { it.clearAll() }
        val engine = SyncEngine(store, server, { user }, SyncStateStore(MemoryKeyValueStore()), clock)
        if (user != null) engine.prepareForLogin(user)
        return Phone(store, engine)
    }

    @Test
    fun aNewPhoneGetsEverythingAfterLoggingIn() = runTest {
        val server = FakeServer()
        val a = phone(server) { tick() }
        a.store.addStudy(study("s1"))
        a.store.saveSession(StudySession("x1", "Cells", "s1", Subject.Science, 20_001, 18 * 60, 60, hasAssignment = true))
        a.store.saveNote(Note("n1", "s1", "Mitosis", "Cell division"))
        assertTrue(a.engine.sync(force = true))
        assertEquals(SyncStatus.Synced(time), a.engine.status)

        val b = phone(server) { tick() }
        assertTrue(b.engine.sync(force = true))
        assertEquals(listOf("s1"), b.store.studies.map { it.id })
        assertEquals("Cells", b.store.sessions.single().title)
        assertTrue(b.store.sessions.single().hasAssignment)
        assertEquals("Cell division", b.store.notes.single().body)
        assertEquals(0, b.engine.pendingChanges)
    }

    @Test
    fun editsAndDeletionsReachTheOtherPhone() = runTest {
        val server = FakeServer()
        val a = phone(server) { tick() }
        val b = phone(server) { tick() }
        a.store.addStudy(study("s1"))
        a.engine.sync(force = true); b.engine.sync(force = true)

        a.store.updateStudy(study("s1", "Biology 2"))
        a.engine.sync(force = true); b.engine.sync(force = true)
        assertEquals("Biology 2", b.store.studies.single().title)

        a.store.deleteStudy("s1")
        a.engine.sync(force = true); b.engine.sync(force = true)
        assertTrue(b.store.studies.isEmpty())
    }

    @Test
    fun theLatestEditWinsEvenIfSyncedLater() = runTest {
        val server = FakeServer()
        val a = phone(server) { tick() }
        val b = phone(server) { tick() }
        a.store.saveNote(Note("n1", "s", "T", "first"))
        a.engine.sync(force = true); b.engine.sync(force = true)

        b.store.saveNote(Note("n1", "s", "T", "from B, later"))
        a.store.saveNote(Note("n1", "s", "T", "from A, earlier"))
        // B edited at a later time than A, but A reaches the server last
        a.engine.sync(force = true) // detects A's edit now
        b.engine.sync(force = true) // detects B's edit afterwards (later clock) and wins
        a.engine.sync(force = true)
        assertEquals("from B, later", a.store.notes.single().body)
        assertEquals("from B, later", b.store.notes.single().body)
    }

    @Test
    fun offlineChangesWaitAndSendLater() = runTest {
        val server = FakeServer()
        val a = phone(server) { tick() }
        server.online = false
        a.store.addStudy(study("s1"))
        assertFalse(a.engine.sync(force = true))
        assertEquals(true, (a.engine.status as SyncStatus.Failed).offline)
        assertTrue(a.engine.pendingChanges > 0)
        assertFalse("studies/s1" in server.keys)

        server.online = true
        assertTrue(a.engine.sync(force = true))
        assertTrue("studies/s1" in server.keys)
        assertEquals(0, a.engine.pendingChanges)
    }

    @Test
    fun sampleDataIsNeverSentAndDoesNotStopRealData() = runTest {
        val server = FakeServer()
        val a = phone(server) { tick() }
        a.store.loadDemoData()
        a.store.addStudy(study("mine"))
        a.engine.sync(force = true)
        assertTrue(server.keys.none { it.contains("demo") })
        assertTrue("studies/mine" in server.keys)
        assertTrue(server.keys.none { it.startsWith("sessions/") }, "sample sessions stay on the phone")
    }

    @Test
    fun anotherAccountStartsClean() = runTest {
        val server = FakeServer()
        val a = phone(server) { tick() }
        a.store.saveNote(Note("n1", "s", "Private", "mine"))
        a.engine.sync(force = true)

        a.engine.prepareForLogin("someone-else")
        assertTrue(a.store.notes.isEmpty())
        assertEquals(0, a.engine.pendingChanges)

        a.engine.forgetLocalData()
        assertEquals(SyncStatus.Idle, a.engine.status)
    }

    @Test
    fun loggedOutNeverSyncsAndPreferencesTravelButNotTheName() = runTest {
        val server = FakeServer()
        val out = phone(server, user = null) { tick() }
        out.store.addStudy(study("s1"))
        assertFalse(out.engine.sync(force = true))
        assertTrue(server.keys.isEmpty())

        val a = phone(server) { tick() }
        a.store.profile = a.store.profile.copy(name = "Ada", quietHoursEnabled = true)
        a.engine.sync(force = true)
        val b = phone(server) { tick() }
        b.store.profile = b.store.profile.copy(name = "Bob")
        b.engine.sync(force = true)
        assertTrue(b.store.profile.quietHoursEnabled)
        assertEquals("Bob", b.store.profile.name)
    }
}

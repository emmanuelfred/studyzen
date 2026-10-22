package com.fredrickemmanuel.studyzen.data

import com.fredrickemmanuel.studyzen.domain.Assessment
import com.fredrickemmanuel.studyzen.domain.AssessmentResult
import com.fredrickemmanuel.studyzen.domain.FavoriteWord
import com.fredrickemmanuel.studyzen.domain.Note
import com.fredrickemmanuel.studyzen.domain.ResearchItem
import com.fredrickemmanuel.studyzen.domain.ScanRecord
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.domain.StudyLogEntry
import com.fredrickemmanuel.studyzen.domain.StudyPlan
import com.fredrickemmanuel.studyzen.domain.StudySession
import com.fredrickemmanuel.studyzen.domain.UserProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything the learner has created, in one piece that can be written to storage. */
@Serializable
data class StoreSnapshot(
    val studies: List<Study> = emptyList(),
    val sessions: List<StudySession> = emptyList(),
    val plans: List<StudyPlan> = emptyList(),
    val notes: List<Note> = emptyList(),
    val results: List<AssessmentResult> = emptyList(),
    val research: List<ResearchItem> = emptyList(),
    val chats: List<ChatEntry> = emptyList(),
    val favoriteWords: List<FavoriteWord> = emptyList(),
    val assessments: List<Assessment> = emptyList(),
    val scans: List<ScanRecord> = emptyList(),
    val studyLog: List<StudyLogEntry> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val demoDataLoaded: Boolean = false,
)

/**
 * Keeps the learner's data on the device so plans, sessions and notes survive closing the app.
 * Stored in app-private storage; a failed read simply starts fresh.
 */
class StorePersistence(private val store: KeyValueStore = platformKeyValueStore()) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): StoreSnapshot? {
        val text = store.get(KEY) ?: return null
        return try {
            json.decodeFromString(StoreSnapshot.serializer(), text)
        } catch (e: Exception) {
            null
        }
    }

    fun save(snapshot: StoreSnapshot) {
        try {
            store.put(KEY, json.encodeToString(StoreSnapshot.serializer(), snapshot))
        } catch (e: Exception) {
            // storage full or blocked: the app keeps working, it just won't remember this change
        }
    }

    private companion object {
        const val KEY = "store.snapshot.v1"
    }
}

package com.fredrickemmanuel.studyzen.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fredrickemmanuel.studyzen.core.Ids
import com.fredrickemmanuel.studyzen.core.time.currentTimeMillis
import com.fredrickemmanuel.studyzen.core.time.nowMinuteOfDay
import com.fredrickemmanuel.studyzen.core.time.todayEpochDay
import com.fredrickemmanuel.studyzen.data.mock.DemoData
import com.fredrickemmanuel.studyzen.domain.AssessmentResult
import com.fredrickemmanuel.studyzen.domain.ChatMessage
import com.fredrickemmanuel.studyzen.domain.LessonResource
import com.fredrickemmanuel.studyzen.domain.LessonMaterial
import com.fredrickemmanuel.studyzen.domain.Lesson
import com.fredrickemmanuel.studyzen.domain.StudyLogEntry
import com.fredrickemmanuel.studyzen.domain.ScanRecord
import com.fredrickemmanuel.studyzen.domain.Assessment
import com.fredrickemmanuel.studyzen.domain.FavoriteWord
import com.fredrickemmanuel.studyzen.domain.LearningModule
import com.fredrickemmanuel.studyzen.domain.Note
import com.fredrickemmanuel.studyzen.domain.PlanGenerator
import com.fredrickemmanuel.studyzen.domain.ResearchItem
import com.fredrickemmanuel.studyzen.domain.SessionStatus
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.domain.StudyPlan
import com.fredrickemmanuel.studyzen.domain.StudySession
import com.fredrickemmanuel.studyzen.domain.StudyTopic
import com.fredrickemmanuel.studyzen.domain.UserProfile
import kotlinx.serialization.Serializable

/**
 * In-memory app data used by every screen. It stands in for local storage plus the Node API:
 * screens only call these functions, so replacing the internals later (database, network)
 * does not change any screen.
 *
 * Data is lost when the app closes for now; persistence is a later step.
 */
@Stable
class StudyStore(private val persistence: StorePersistence? = null) {
    val studies = mutableStateListOf<Study>()
    val sessions = mutableStateListOf<StudySession>()
    val plans = mutableStateListOf<StudyPlan>()
    val notes = mutableStateListOf<Note>()
    val favoriteWords = mutableStateListOf<FavoriteWord>()
    val results = mutableStateListOf<AssessmentResult>()
    val research = mutableStateListOf<ResearchItem>()
    val chats = mutableStateListOf<ChatEntry>()
    val assessments = mutableStateListOf<Assessment>()
    val scans = mutableStateListOf<ScanRecord>()
    val studyLog = mutableStateListOf<StudyLogEntry>()

    var profile by mutableStateOf(UserProfile())

    /** True while sample data is loaded (so the app can be shown without typing everything in). */
    var demoDataLoaded by mutableStateOf(false)
        private set

    /** The session being studied right now, kept so it can be resumed if the app is reopened. */
    var activeRun by mutableStateOf<SessionRun?>(null)

    init {
        val saved = persistence?.load()
        if (saved != null) restore(saved) else loadDemoData()
    }

    // --- Saving ------------------------------------------------------------------------------

    /** Everything worth keeping, copied so it can be compared and written to storage. */
    fun snapshot() = StoreSnapshot(
        studies = studies.toList(), sessions = sessions.toList(), plans = plans.toList(),
        notes = notes.toList(), results = results.toList(), research = research.toList(),
        chats = chats.toList(), favoriteWords = favoriteWords.toList(),
        assessments = assessments.toList(), scans = scans.toList(), studyLog = studyLog.toList(),
        profile = profile, demoDataLoaded = demoDataLoaded,
    )

    fun restore(saved: StoreSnapshot) {
        studies.clear(); studies.addAll(saved.studies)
        sessions.clear(); sessions.addAll(saved.sessions)
        plans.clear(); plans.addAll(saved.plans)
        notes.clear(); notes.addAll(saved.notes)
        results.clear(); results.addAll(saved.results)
        research.clear(); research.addAll(saved.research)
        chats.clear(); chats.addAll(saved.chats)
        favoriteWords.clear(); favoriteWords.addAll(saved.favoriteWords)
        assessments.clear(); assessments.addAll(saved.assessments)
        scans.clear(); scans.addAll(saved.scans)
        studyLog.clear(); studyLog.addAll(saved.studyLog)
        profile = saved.profile
        demoDataLoaded = saved.demoDataLoaded
        activeRun = null
    }

    fun saveNow() { persistence?.save(snapshot()) }

    // --- Demo data ---------------------------------------------------------------------------

    fun loadDemoData() {
        clearAll()
        DemoData.populate(this)
        demoDataLoaded = true
    }

    fun clearAll() {
        studies.clear(); sessions.clear(); plans.clear(); notes.clear()
        results.clear(); research.clear(); chats.clear(); favoriteWords.clear()
        assessments.clear(); scans.clear(); studyLog.clear()
        activeRun = null
        demoDataLoaded = false
    }

    // --- Lookups -----------------------------------------------------------------------------

    fun study(id: String?): Study? = studies.firstOrNull { it.id == id }
    fun session(id: String?): StudySession? = sessions.firstOrNull { it.id == id }
    fun plan(id: String?): StudyPlan? = plans.firstOrNull { it.id == id }
    fun note(id: String?): Note? = notes.firstOrNull { it.id == id }

    fun activeStudies(): List<Study> = studies.filter { !it.archived }
    fun archivedStudies(): List<Study> = studies.filter { it.archived }

    fun notesFor(studyId: String): List<Note> =
        notes.filter { it.studyId == studyId }.sortedByDescending { it.updatedAtEpochDay }

    fun resultsFor(studyId: String): List<AssessmentResult> = results.filter { it.studyId == studyId }

    fun sessionsOn(epochDay: Long): List<StudySession> =
        sessions.filter { it.epochDay == epochDay }.sortedBy { it.startMinute }

    /** The next scheduled session from now on (today later, or a future day). */
    fun nextSession(todayEpochDay: Long, nowMinute: Int): StudySession? =
        sessions
            .filter { it.status == SessionStatus.Scheduled }
            .filter { it.epochDay > todayEpochDay || (it.epochDay == todayEpochDay && it.endMinute > nowMinute) }
            .minWithOrNull(compareBy<StudySession>({ it.epochDay }, { it.startMinute }))

    // --- Studies -----------------------------------------------------------------------------

    fun addStudy(study: Study) { studies.add(0, study) }

    fun updateStudy(study: Study) {
        val i = studies.indexOfFirst { it.id == study.id }
        if (i >= 0) studies[i] = study
    }

    fun archiveStudy(id: String, archived: Boolean) {
        study(id)?.let { updateStudy(it.copy(archived = archived)) }
    }

    /** Removes the Study and everything that belongs to it. Sessions stay but lose the link. */
    fun deleteStudy(id: String) {
        studies.removeAll { it.id == id }
        notes.removeAll { it.studyId == id }
        results.removeAll { it.studyId == id }
        research.removeAll { it.studyId == id }
        chats.removeAll { it.studyId == id }
        assessments.removeAll { it.studyId == id }
        scans.removeAll { it.studyId == id }
        studyLog.removeAll { it.studyId == id }
        for (i in plans.indices) {
            if (id in plans[i].studyIds) plans[i] = plans[i].copy(studyIds = plans[i].studyIds.filterNot { it == id })
        }
        for (i in sessions.indices) {
            if (sessions[i].studyId == id) sessions[i] = sessions[i].copy(studyId = null)
        }
    }

    fun setModules(studyId: String, modules: List<LearningModule>) {
        study(studyId)?.let { updateStudy(it.copy(modules = modules)) }
    }

    fun setLessonCompleted(studyId: String, lessonId: String, completed: Boolean) {
        val s = study(studyId) ?: return
        val modules = s.modules.map { m ->
            m.copy(lessons = m.lessons.map { if (it.id == lessonId) it.copy(completed = completed) else it })
        }
        updateStudy(s.copy(modules = modules, lastLessonId = lessonId))
    }

    fun setLastLesson(studyId: String, lessonId: String) {
        study(studyId)?.let { updateStudy(it.copy(lastLessonId = lessonId)) }
    }

    /** Changes one lesson without touching the learner's place in the study. */
    fun updateLesson(studyId: String, lessonId: String, change: (Lesson) -> Lesson) {
        val s = study(studyId) ?: return
        val modules = s.modules.map { m -> m.copy(lessons = m.lessons.map { if (it.id == lessonId) change(it) else it }) }
        updateStudy(s.copy(modules = modules))
    }

    fun setLessonMaterial(studyId: String, lessonId: String, material: LessonMaterial) =
        updateLesson(studyId, lessonId) { it.copy(material = material) }

    fun toggleBookmark(studyId: String, lessonId: String, resourceId: String) =
        updateLesson(studyId, lessonId) { lesson ->
            val m = lesson.material ?: return@updateLesson lesson
            lesson.copy(material = m.copy(resources = m.resources.map { if (it.id == resourceId) it.copy(bookmarked = !it.bookmarked) else it }))
        }

    /** Every bookmarked source in a study, with the lesson it came from. */
    fun bookmarksFor(studyId: String): List<Pair<Lesson, LessonResource>> =
        study(studyId)?.allLessons.orEmpty().flatMap { lesson ->
            lesson.material?.resources.orEmpty().filter { it.bookmarked }.map { lesson to it }
        }

    // --- Study plans: a study belongs to at most one plan -----------------------------------

    /** The plan this study is attached to, if any. */
    fun planFor(studyId: String): StudyPlan? = plans.firstOrNull { studyId in it.studyIds }

    /** Attaches a study to a plan. If it was in another plan it moves: it is never in two at once. */
    fun attachStudyToPlan(studyId: String, planId: String) {
        val target = plan(planId) ?: return
        savePlan(target.copy(studyIds = (target.studyIds + studyId).distinct()))
    }

    fun detachStudyFromPlan(studyId: String) {
        for (i in plans.indices) {
            if (studyId in plans[i].studyIds) plans[i] = plans[i].copy(studyIds = plans[i].studyIds.filterNot { it == studyId })
        }
    }

    /** Upcoming and past sessions of a study, soonest first. */
    fun sessionsForStudy(studyId: String): List<StudySession> =
        sessions.filter { it.studyId == studyId }.sortedWith(compareBy({ it.epochDay }, { it.startMinute }))

    // --- Assessments, scans and the tool log -------------------------------------------------

    fun assessment(id: String?): Assessment? = assessments.firstOrNull { it.id == id }
    fun assessmentsFor(studyId: String): List<Assessment> =
        assessments.filter { it.studyId == studyId }.sortedByDescending { it.createdAtEpochDay }

    fun saveAssessment(assessment: Assessment) {
        val i = assessments.indexOfFirst { it.id == assessment.id }
        if (i >= 0) assessments[i] = assessment else assessments.add(0, assessment)
    }

    fun scan(id: String?): ScanRecord? = scans.firstOrNull { it.id == id }
    fun scansFor(studyId: String): List<ScanRecord> =
        scans.filter { it.studyId == studyId }.sortedByDescending { it.updatedAtEpochDay }

    fun saveScan(scan: ScanRecord) {
        val i = scans.indexOfFirst { it.id == scan.id }
        if (i >= 0) scans[i] = scan else scans.add(0, scan)
    }

    fun deleteScan(id: String) { scans.removeAll { it.id == id } }

    fun logFor(studyId: String): List<StudyLogEntry> = studyLog.filter { it.studyId == studyId }

    /** Remembers a tool use. The same entry twice in a row is only kept once. */
    fun addLog(entry: StudyLogEntry) {
        val last = studyLog.firstOrNull { it.studyId == entry.studyId }
        if (last != null && last.kind == entry.kind && last.text == entry.text) return
        studyLog.add(0, entry)
        // keep each study's history from growing forever
        val mine = studyLog.filter { it.studyId == entry.studyId }
        if (mine.size > 200) studyLog.removeAll(mine.drop(200).toSet())
    }

    // --- Notes -------------------------------------------------------------------------------

    fun saveNote(note: Note) {
        val i = notes.indexOfFirst { it.id == note.id }
        if (i >= 0) notes[i] = note else notes.add(0, note)
    }

    fun deleteNote(id: String) { notes.removeAll { it.id == id } }

    /** Pinned notes first, then the most recently edited. */
    fun allNotes(): List<Note> =
        notes.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAtEpochDay })

    // --- Favourite words ---------------------------------------------------------------------

    fun isFavoriteWord(word: String): Boolean = favoriteWords.any { it.id == word.trim().lowercase() }

    fun addFavoriteWord(word: FavoriteWord) {
        val i = favoriteWords.indexOfFirst { it.id == word.id }
        if (i >= 0) favoriteWords[i] = word else favoriteWords.add(0, word)
    }

    fun removeFavoriteWord(id: String) { favoriteWords.removeAll { it.id == id } }

    // --- Chat and research -------------------------------------------------------------------

    fun chatFor(studyId: String): List<ChatEntry> = chats.filter { it.studyId == studyId }

    fun addChat(studyId: String, message: ChatMessage, lessonId: String? = null) {
        chats.add(ChatEntry(studyId, message, lessonId))
    }

    fun markChatSaved(messageId: String) {
        val i = chats.indexOfFirst { it.message.id == messageId }
        if (i >= 0) chats[i] = chats[i].copy(message = chats[i].message.copy(saved = true))
    }

    fun saveResearch(item: ResearchItem) {
        val i = research.indexOfFirst { it.id == item.id }
        if (i >= 0) research[i] = item else research.add(0, item)
    }

    fun addResult(result: AssessmentResult) { results.add(0, result) }

    // --- Sessions and plans ------------------------------------------------------------------

    fun saveSession(session: StudySession) {
        val i = sessions.indexOfFirst { it.id == session.id }
        if (i >= 0) sessions[i] = session else sessions.add(session)
    }

    fun deleteSession(id: String) {
        sessions.removeAll { it.id == id }
        if (activeRun?.sessionId == id) activeRun = null
    }

    fun skipSession(id: String) {
        session(id)?.let { saveSession(it.copy(status = SessionStatus.Skipped)) }
    }

    fun postponeSession(id: String, days: Long = 1) {
        session(id)?.let { saveSession(it.copy(epochDay = it.epochDay + days, status = SessionStatus.Scheduled)) }
    }

    fun completeSession(id: String, actualMinutes: Int, reflection: String = "") {
        session(id)?.let {
            saveSession(it.copy(status = SessionStatus.Completed, actualMinutes = actualMinutes, reflection = reflection))
        }
    }

    fun savePlan(plan: StudyPlan) {
        val clean = plan.copy(studyIds = plan.studyIds.distinct())
        val i = plans.indexOfFirst { it.id == clean.id }
        if (i >= 0) plans[i] = clean else plans.add(0, clean)
        // a study may belong to only one plan: this plan now owns the studies it lists
        for (j in plans.indices) {
            if (plans[j].id != clean.id && plans[j].studyIds.any { it in clean.studyIds }) {
                plans[j] = plans[j].copy(studyIds = plans[j].studyIds.filterNot { it in clean.studyIds })
            }
        }
    }

    /** Deleting a plan also removes its sessions that have not happened yet. */
    fun deletePlan(id: String) {
        plans.removeAll { it.id == id }
        sessions.removeAll { it.planId == id && it.status == SessionStatus.Scheduled }
    }

    fun suggestSessions(plan: StudyPlan, topics: List<StudyTopic>): List<StudySession> =
        PlanGenerator.suggest(
            plan = plan,
            topics = topics,
            fromEpochDay = todayEpochDay(),
            existing = sessions.filter { it.planId != plan.id || it.status != SessionStatus.Scheduled },
            firstDayAfterMinute = nowMinuteOfDay(),
            newId = { Ids.next("ses") },
        )

    /** Saves a plan together with the (reviewed) sessions, replacing its earlier upcoming sessions. */
    fun commitPlan(plan: StudyPlan, reviewed: List<StudySession>) {
        sessions.removeAll { it.planId == plan.id && it.status == SessionStatus.Scheduled }
        savePlan(plan)
        sessions.addAll(reviewed)
    }

    /** Turns a plan's reminder on or off and applies it to the sessions that have not happened yet. */
    fun setPlanReminder(planId: String, reminder: com.fredrickemmanuel.studyzen.domain.Reminder) {
        val plan = plan(planId) ?: return
        savePlan(plan.copy(reminder = reminder))
        val today = todayEpochDay()
        for (i in sessions.indices) {
            val s = sessions[i]
            if (s.planId == planId && s.status == SessionStatus.Scheduled && s.epochDay >= today) {
                sessions[i] = s.copy(reminder = reminder.takeIf { it.enabled })
            }
        }
    }

    /** A session the learner starts right now, outside any plan. Returns it so the timer can open. */
    fun startQuickSession(study: Study, minutes: Int): StudySession {
        val session = StudySession(
            id = Ids.next("ses"),
            title = study.title,
            studyId = study.id,
            subject = study.subject,
            epochDay = todayEpochDay(),
            startMinute = nowMinuteOfDay(),
            durationMinutes = minutes,
        )
        saveSession(session)
        startRun(session.id)
        return session
    }

    // --- Session timer -----------------------------------------------------------------------

    fun startRun(sessionId: String) {
        val existing = activeRun
        if (existing != null && existing.sessionId == sessionId) return
        activeRun = SessionRun(sessionId = sessionId, accumulatedMillis = 0L, resumedAtMillis = currentTimeMillis())
    }

    fun pauseRun() {
        val run = activeRun ?: return
        if (run.resumedAtMillis != null) {
            activeRun = run.copy(
                accumulatedMillis = run.accumulatedMillis + (currentTimeMillis() - run.resumedAtMillis),
                resumedAtMillis = null,
            )
        }
    }

    fun resumeRun() {
        val run = activeRun ?: return
        if (run.resumedAtMillis == null) activeRun = run.copy(resumedAtMillis = currentTimeMillis())
    }

    fun clearRun() { activeRun = null }
}

/** One chat message plus the Study it belongs to. */
@Serializable
data class ChatEntry(val studyId: String, val message: ChatMessage, val lessonId: String? = null)

/**
 * Tracks study time for a running session. Only time between resume and pause counts,
 * so paused time is never added to active study time.
 */
data class SessionRun(
    val sessionId: String,
    val accumulatedMillis: Long,
    /** When the timer last started running; null while paused. */
    val resumedAtMillis: Long?,
) {
    val isRunning: Boolean get() = resumedAtMillis != null

    fun elapsedMillis(nowMillis: Long): Long =
        accumulatedMillis + (if (resumedAtMillis != null) (nowMillis - resumedAtMillis).coerceAtLeast(0) else 0L)
}

val LocalStudyStore = compositionLocalOf<StudyStore> { error("StudyStore not provided") }

@Composable
fun rememberStudyStore(): StudyStore = remember { StudyStore(StorePersistence()) }

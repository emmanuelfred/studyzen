package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.data.MemoryKeyValueStore
import com.fredrickemmanuel.studyzen.data.StorePersistence
import com.fredrickemmanuel.studyzen.data.StudyStore
import com.fredrickemmanuel.studyzen.domain.Assessment
import com.fredrickemmanuel.studyzen.domain.AssessmentKind
import com.fredrickemmanuel.studyzen.domain.AssessmentQuestion
import com.fredrickemmanuel.studyzen.domain.AssignmentCadence
import com.fredrickemmanuel.studyzen.domain.KnowledgeLevel
import com.fredrickemmanuel.studyzen.domain.LearningModule
import com.fredrickemmanuel.studyzen.domain.Lesson
import com.fredrickemmanuel.studyzen.domain.LessonMaterial
import com.fredrickemmanuel.studyzen.domain.LessonResource
import com.fredrickemmanuel.studyzen.domain.LogKind
import com.fredrickemmanuel.studyzen.domain.ResourceSource
import com.fredrickemmanuel.studyzen.domain.ScanRecord
import com.fredrickemmanuel.studyzen.domain.ScanTurnRecord
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.domain.StudyLogEntry
import com.fredrickemmanuel.studyzen.domain.StudyPlan
import com.fredrickemmanuel.studyzen.domain.StudyPurpose
import com.fredrickemmanuel.studyzen.domain.Subject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudyStoreRulesTest {
    private fun store() = StudyStore().also { it.clearAll() }

    private fun study(id: String) = Study(
        id = id, title = "Study $id", description = "", objective = "", subject = Subject.Science,
        level = KnowledgeLevel.Beginner, createdAtEpochDay = 0,
        modules = listOf(LearningModule("m", "Module", listOf(Lesson("$id-l1", "One", ""), Lesson("$id-l2", "Two", "")))),
    )

    private fun plan(id: String, vararg studies: String) = StudyPlan(
        id = id, name = id, goal = "", studyIds = studies.toList(), startEpochDay = 0, endEpochDay = 10,
        weeklyBlocks = emptyMap(), assignment = AssignmentCadence.None,
    )

    @Test fun aStudyBelongsToAtMostOnePlan() {
        val s = store()
        s.addStudy(study("a")); s.addStudy(study("b"))
        s.savePlan(plan("p1", "a", "b"))
        assertEquals("p1", s.planFor("a")?.id)

        // a second plan claiming "a" takes it over: it is never in two plans
        s.savePlan(plan("p2", "a"))
        assertEquals("p2", s.planFor("a")?.id)
        assertEquals(listOf("b"), s.plan("p1")?.studyIds)
        assertEquals(1, s.plans.count { "a" in it.studyIds })
    }

    @Test fun attachingMovesAndDetachingFrees() {
        val s = store()
        s.addStudy(study("a"))
        s.savePlan(plan("p1", "a")); s.savePlan(plan("p2"))
        s.attachStudyToPlan("a", "p2")
        assertEquals("p2", s.planFor("a")?.id)
        assertEquals(emptyList(), s.plan("p1")?.studyIds)
        s.attachStudyToPlan("a", "p2") // twice is harmless
        assertEquals(listOf("a"), s.plan("p2")?.studyIds)
        s.detachStudyFromPlan("a")
        assertNull(s.planFor("a"))
    }

    @Test fun aStandaloneStudyNeedsNoPlan() {
        val s = store()
        s.addStudy(study("solo"))
        assertNull(s.planFor("solo"))
        assertEquals(emptyList(), s.sessionsForStudy("solo"))
    }

    @Test fun deletingAStudyRemovesItsWorkEverywhere() {
        val s = store()
        s.addStudy(study("a")); s.addStudy(study("b"))
        s.savePlan(plan("p", "a", "b"))
        s.saveAssessment(Assessment("x1", "a", null, "Quiz", AssessmentKind.MultipleChoice, 0, listOf(AssessmentQuestion("q", "?"))))
        s.saveAssessment(Assessment("x2", "b", null, "Quiz", AssessmentKind.MultipleChoice, 0, listOf(AssessmentQuestion("q", "?"))))
        s.saveScan(ScanRecord("sc1", "a", "Scan", listOf(ScanTurnRecord(true, "hi")), null, 0))
        s.addLog(StudyLogEntry("l1", "a", LogKind.Dictionary, "word", epochDay = 0))
        s.deleteStudy("a")
        assertEquals(listOf("x2"), s.assessments.map { it.id })
        assertTrue(s.scans.isEmpty())
        assertTrue(s.studyLog.isEmpty())
        assertEquals(listOf("b"), s.plan("p")?.studyIds)
    }

    @Test fun workStaysInsideItsOwnStudy() {
        val s = store()
        s.addStudy(study("a")); s.addStudy(study("b"))
        s.saveScan(ScanRecord("sc1", "a", "Scan A", listOf(ScanTurnRecord(true, "hi")), null, 1))
        s.saveScan(ScanRecord("sc2", "b", "Scan B", listOf(ScanTurnRecord(true, "yo")), null, 2))
        s.addLog(StudyLogEntry("l1", "a", LogKind.Calculator, "2+2", epochDay = 0))
        assertEquals(listOf("sc1"), s.scansFor("a").map { it.id })
        assertEquals(listOf("sc2"), s.scansFor("b").map { it.id })
        assertEquals(1, s.logFor("a").size)
        assertEquals(0, s.logFor("b").size)
    }

    @Test fun lessonMaterialsAndBookmarksAreKept() {
        val s = store()
        s.addStudy(study("a"))
        val res = LessonResource("r1", "Cell", "https://en.wikipedia.org/wiki/Cell", ResourceSource.Wikipedia, "snippet")
        s.setLessonMaterial("a", "a-l1", LessonMaterial("Explanation", "Gemini", listOf(res), 5))
        assertEquals("Explanation", s.study("a")?.lesson("a-l1")?.material?.explanation)
        assertNull(s.study("a")?.lesson("a-l2")?.material)

        s.toggleBookmark("a", "a-l1", "r1")
        assertEquals(1, s.bookmarksFor("a").size)
        s.toggleBookmark("a", "a-l1", "r1")
        assertEquals(0, s.bookmarksFor("a").size)
    }

    @Test fun repeatedLogEntriesAreKeptOnce() {
        val s = store()
        s.addLog(StudyLogEntry("1", "a", LogKind.Dictionary, "photosynthesis", epochDay = 0))
        s.addLog(StudyLogEntry("2", "a", LogKind.Dictionary, "photosynthesis", epochDay = 0))
        s.addLog(StudyLogEntry("3", "a", LogKind.Dictionary, "osmosis", epochDay = 0))
        assertEquals(listOf("osmosis", "photosynthesis"), s.logFor("a").map { it.text })
    }

    @Test fun everythingSurvivesClosingTheApp() {
        val storage = MemoryKeyValueStore()
        val first = StudyStore(StorePersistence(storage)).also { it.clearAll() }
        first.addStudy(study("a").copy(purpose = StudyPurpose.Exam, curriculum = "WAEC", deadlineEpochDay = 99, guided = true, aiStructured = true))
        first.savePlan(plan("p", "a"))
        first.saveAssessment(Assessment("x", "a", "a-l1", "Quiz", AssessmentKind.Interview, 3, listOf(AssessmentQuestion("q", "Tell me about yourself"))))
        first.saveNow()

        val second = StudyStore(StorePersistence(storage))
        val s = second.study("a")
        assertNotNull(s)
        assertEquals(StudyPurpose.Exam, s.purpose)
        assertEquals("WAEC", s.curriculum)
        assertEquals(99L, s.deadlineEpochDay)
        assertTrue(s.guided && s.aiStructured)
        assertEquals("p", second.planFor("a")?.id)
        assertEquals(AssessmentKind.Interview, second.assessment("x")?.kind)
    }

    @Test fun aQuickSessionIsRunningAndBelongsToItsStudy() {
        val st = store()
        val s = study("a")
        st.addStudy(s)
        val session = st.startQuickSession(s, 25)
        assertEquals("a", session.studyId)
        assertEquals(25, session.durationMinutes)
        assertEquals(session.id, st.activeRun?.sessionId)
        assertNotNull(st.session(session.id))
    }

    @Test fun switchingAPlanReminderUpdatesItsFutureSessionsOnly() {
        val st = store()
        st.addStudy(study("a"))
        val p = plan("p1", "a").copy(reminder = com.fredrickemmanuel.studyzen.domain.Reminder(enabled = true))
        st.savePlan(p)
        val future = com.fredrickemmanuel.studyzen.domain.StudySession(
            id = "f", title = "F", studyId = "a", subject = Subject.Science, epochDay = Long.MAX_VALUE / 4, startMinute = 600,
            durationMinutes = 30, planId = "p1", reminder = p.reminder,
        )
        val past = future.copy(id = "o", epochDay = 0)
        st.saveSession(future)
        st.saveSession(past.copy(reminder = p.reminder))

        st.setPlanReminder("p1", p.reminder.copy(enabled = false))
        assertEquals(false, st.plan("p1")?.reminder?.enabled)
        assertNull(st.session("f")?.reminder)
        assertNotNull(st.session("o")?.reminder)

        st.setPlanReminder("p1", p.reminder.copy(enabled = true))
        assertNotNull(st.session("f")?.reminder)
    }
}

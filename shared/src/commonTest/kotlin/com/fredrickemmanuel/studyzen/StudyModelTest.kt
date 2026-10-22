package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.domain.Assessment
import com.fredrickemmanuel.studyzen.domain.AssessmentAnswer
import com.fredrickemmanuel.studyzen.domain.AssessmentKind
import com.fredrickemmanuel.studyzen.domain.AssessmentQuestion
import com.fredrickemmanuel.studyzen.domain.KnowledgeLevel
import com.fredrickemmanuel.studyzen.domain.LearningModule
import com.fredrickemmanuel.studyzen.domain.Lesson
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.domain.Subject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StudyModelTest {
    private fun study(guided: Boolean, done: Int, last: String? = null): Study {
        val lessons = (1..5).map { Lesson("l$it", "Lesson $it", "", completed = it <= done) }
        return Study(
            id = "s", title = "T", description = "", objective = "", subject = Subject.Science,
            level = KnowledgeLevel.Beginner,
            modules = listOf(LearningModule("m1", "A", lessons.take(3)), LearningModule("m2", "B", lessons.drop(3))),
            lastLessonId = last, createdAtEpochDay = 0, guided = guided,
        )
    }

    @Test fun lessonsAreFoundAcrossModulesInOrder() {
        val s = study(false, 0)
        assertEquals("l4", s.nextOf("l3")?.id)
        assertEquals("l3", s.previousOf("l4")?.id)
        assertNull(s.previousOf("l1"))
        assertNull(s.nextOf("l5"))
        assertEquals("m2", s.moduleOf("l5")?.id)
    }

    @Test fun continueGoesWhereTheLearnerStoppedOtherwiseToTheFirstUnfinished() {
        assertEquals("l3", study(false, 2).currentLesson?.id)
        assertEquals("l2", study(false, 2, last = "l2").currentLesson?.id)
        // everything finished: reopen the first lesson
        assertEquals("l1", study(false, 5).currentLesson?.id)
    }

    @Test fun flexibleStudiesOpenAnyLesson() {
        val s = study(false, 0)
        assertTrue(s.allLessons.all { s.canOpen(it.id) })
    }

    @Test fun guidedStudiesOpenDoneLessonsAndTheNextOne() {
        val s = study(true, 2)
        assertTrue(s.canOpen("l1"))
        assertTrue(s.canOpen("l2"))
        assertTrue(s.canOpen("l3")) // the next one
        assertFalse(s.canOpen("l4"))
        assertFalse(s.canOpen("nope"))
        assertTrue(study(true, 5).allLessons.all { study(true, 5).canOpen(it.id) })
    }

    @Test fun progressCountsFinishedLessons() {
        assertEquals(0.4f, study(false, 2).progress)
    }

    @Test fun objectiveQuestionsAreMarkedAndWrittenOnesAreNot() {
        val mcq = AssessmentQuestion("q1", "2+2?", options = listOf("3", "4"), correctIndex = 1)
        val mcq2 = AssessmentQuestion("q2", "Capital?", options = listOf("A", "B", "C"), correctIndex = 0)
        val open = AssessmentQuestion("q3", "Explain")
        val a = Assessment(
            id = "a", studyId = "s", title = "t", kind = AssessmentKind.MultipleChoice, createdAtEpochDay = 0,
            questions = listOf(mcq, mcq2, open),
            answers = listOf(AssessmentAnswer("q1", choice = 1), AssessmentAnswer("q2", choice = 2), AssessmentAnswer("q3", text = "x")),
            submitted = true,
        )
        assertEquals(1, a.correct)
        assertEquals(2, a.total)
        assertEquals(50, a.percent)
        assertEquals(0, a.copy(questions = listOf(open)).percent)
    }
}

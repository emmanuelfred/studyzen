package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.data.ai.StudyAiPrompts
import com.fredrickemmanuel.studyzen.domain.Assessment
import com.fredrickemmanuel.studyzen.domain.AssessmentAnswer
import com.fredrickemmanuel.studyzen.domain.AssessmentKind
import com.fredrickemmanuel.studyzen.domain.AssessmentQuestion
import com.fredrickemmanuel.studyzen.domain.KnowledgeLevel
import com.fredrickemmanuel.studyzen.domain.LearningModule
import com.fredrickemmanuel.studyzen.domain.Lesson
import com.fredrickemmanuel.studyzen.domain.LessonKind
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.domain.StudyPurpose
import com.fredrickemmanuel.studyzen.domain.Subject
import com.fredrickemmanuel.studyzen.domain.isComplete
import com.fredrickemmanuel.studyzen.domain.recommendKind
import com.fredrickemmanuel.studyzen.domain.weakTopicsOf
import com.fredrickemmanuel.studyzen.domain.withAnswer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssessmentLogicTest {
    private fun study(purpose: StudyPurpose, subject: Subject = Subject.Science) = Study(
        id = "s", title = "Biology", description = "", objective = "", subject = subject, level = KnowledgeLevel.Beginner,
        modules = listOf(LearningModule("m", "M", listOf(Lesson("l1", "Cells", "")))), createdAtEpochDay = 0, purpose = purpose,
    )

    private fun quiz(): Assessment = Assessment(
        id = "a", studyId = "s", title = "Quiz", kind = AssessmentKind.MultipleChoice, createdAtEpochDay = 0,
        questions = listOf(
            AssessmentQuestion("q1", "P1", "Cells", listOf("a", "b"), 0),
            AssessmentQuestion("q2", "P2", "Genetics", listOf("a", "b"), 1),
            AssessmentQuestion("q3", "P3", "Cells", listOf("a", "b"), 1),
        ),
    )

    @Test fun recommendationFollowsThePurposeAndTheLesson() {
        assertEquals(AssessmentKind.MultipleChoice, recommendKind(study(StudyPurpose.Exam), null))
        assertEquals(AssessmentKind.Interview, recommendKind(study(StudyPurpose.Interview), null))
        assertEquals(AssessmentKind.Coding, recommendKind(study(StudyPurpose.Interview, Subject.Programming), null))
        assertEquals(AssessmentKind.Practice, recommendKind(study(StudyPurpose.Skill), Lesson("x", "x", "", kind = LessonKind.Practice)))
        assertEquals(AssessmentKind.ShortAnswer, recommendKind(study(StudyPurpose.Skill), Lesson("x", "x", "", kind = LessonKind.Revision)))
        assertEquals(AssessmentKind.Written, recommendKind(study(StudyPurpose.Coursework), null))
    }

    @Test fun answersAreMarkedAndWeakTopicsListed() {
        val a = quiz().withAnswer(AssessmentAnswer("q1", choice = 0)).withAnswer(AssessmentAnswer("q2", choice = 0))
        assertEquals(1, a.correct)
        assertEquals(3, a.total)
        // q2 wrong, q3 unanswered: topics are listed once each
        assertEquals(listOf("Genetics", "Cells"), weakTopicsOf(a))
    }

    @Test fun changingAnAnswerReplacesItAndCompletenessIsChecked() {
        var a = quiz()
        assertFalse(isComplete(a))
        a = a.withAnswer(AssessmentAnswer("q1", choice = 1)).withAnswer(AssessmentAnswer("q1", choice = 0))
        assertEquals(1, a.answers.size)
        a = a.withAnswer(AssessmentAnswer("q2", choice = 1)).withAnswer(AssessmentAnswer("q3", choice = 1))
        assertTrue(isComplete(a))
        assertEquals(3, a.correct)
    }

    @Test fun writtenQuestionsNeedTextToBeComplete() {
        val w = Assessment(
            id = "w", studyId = "s", title = "W", kind = AssessmentKind.Written, createdAtEpochDay = 0,
            questions = listOf(AssessmentQuestion("q1", "Explain", criteria = "mention x")),
        )
        assertFalse(isComplete(w.withAnswer(AssessmentAnswer("q1", text = "  "))))
        assertTrue(isComplete(w.withAnswer(AssessmentAnswer("q1", text = "because"))))
    }

    @Test fun promptsCarryTheGoalAndNeverAskForMadeUpLinks() {
        val s = study(StudyPurpose.Exam).copy(curriculum = "WAEC", focus = "genetics")
        val lesson = s.allLessons.first()
        val req = StudyAiPrompts.assessmentRequest(s, lesson, AssessmentKind.MultipleChoice, 6)
        assertTrue("Cells" in req)
        assertTrue("genetics" in req)
        assertTrue("6" in req)
        assertTrue("WAEC" in StudyAiPrompts.assessmentSystem(s))
        assertTrue("not" in StudyAiPrompts.FEEDBACK_SYSTEM && "grades" in StudyAiPrompts.FEEDBACK_SYSTEM)
        val fb = StudyAiPrompts.feedbackRequest(quiz().copy(kind = AssessmentKind.Written))
        assertTrue("q1" in fb && "(left blank)" in fb)
    }
}

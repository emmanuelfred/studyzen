package com.fredrickemmanuel.studyzen.data.ai

import com.fredrickemmanuel.studyzen.domain.Assessment
import com.fredrickemmanuel.studyzen.domain.AssessmentKind
import com.fredrickemmanuel.studyzen.domain.KnowledgeLevel
import com.fredrickemmanuel.studyzen.domain.Lesson
import com.fredrickemmanuel.studyzen.domain.Study
import com.fredrickemmanuel.studyzen.domain.StudyPurpose

/** What the learner told us when creating a study. */
data class StudyBrief(
    val title: String,
    val description: String,
    val purpose: StudyPurpose,
    val curriculum: String,
    val focus: String,
    val level: KnowledgeLevel,
    val daysUntilDeadline: Int?,
    /** Study sessions the plan offers, when there is one. */
    val sessions: Int?,
    val minutesPerSession: Int?,
)

/** The wording sent to the AI for each Study Space job. Kept apart from the network code so it can be tested. */
object StudyAiPrompts {

    /** How many lessons to aim for: one per available session, within a sensible range. */
    fun targetLessons(brief: StudyBrief): Int {
        val fromSessions = brief.sessions?.takeIf { it > 0 }
        val fromDays = brief.daysUntilDeadline?.takeIf { it > 0 }
        return (fromSessions ?: fromDays ?: 10).coerceIn(6, 36)
    }

    const val STRUCTURE_SYSTEM = "You design study structures for students. Reply with ONLY one JSON object and nothing else: " +
        "no introduction, no code fences. Shape: " +
        "{\"modules\":[{\"title\":\"...\",\"lessons\":[{\"title\":\"...\",\"summary\":\"one sentence\"," +
        "\"objectives\":[\"what the student can do afterwards\"],\"kind\":\"learn|practice|revision|checkpoint\"}]}]}. " +
        "Rules: put lessons in a logical learning order; use 3 to 6 modules; give every lesson 2 to 4 objectives; " +
        "keep titles under 60 characters; include practice, revision and checkpoint lessons at sensible points; " +
        "never invent facts. If you are not sure of an exam's or curriculum's exact syllabus, use the standard topics " +
        "for that subject and level."

    fun structureRequest(brief: StudyBrief): String = buildString {
        appendLine("Design a study structure for this student.")
        appendLine("Topic: ${brief.title}")
        appendLine("Purpose: ${brief.purpose.label}")
        if (brief.curriculum.isNotBlank()) appendLine("${brief.purpose.curriculumLabel ?: "Curriculum"}: ${brief.curriculum}")
        appendLine("Current level: ${brief.level.label}")
        if (brief.description.isNotBlank()) appendLine("What the student wrote: ${brief.description}")
        if (brief.focus.isNotBlank()) appendLine("Areas to focus on: ${brief.focus}")
        brief.daysUntilDeadline?.let { appendLine("Days until the exam / interview / due date: $it") }
        if (brief.sessions != null && brief.minutesPerSession != null) {
            appendLine("Study sessions available: ${brief.sessions}, about ${brief.minutesPerSession} minutes each.")
        }
        appendLine("Aim for about ${targetLessons(brief)} lessons in total, one per study session.")
        when (brief.purpose) {
            StudyPurpose.Exam -> appendLine("Cover the real exam topics, include past-question practice and a mock test near the end.")
            StudyPurpose.Interview -> appendLine("Cover the topics, technical questions and behavioural questions an interviewer would ask, and mock interviews near the end.")
            StudyPurpose.Revision -> appendLine("The student already learned this: focus on revision, weak spots and practice.")
            StudyPurpose.Coursework -> appendLine("Help the student finish the course or assignment: break the work into clear steps.")
            else -> Unit
        }
    }

    const val STRUCTURE_RETRY = "That was not valid JSON. Reply again with ONLY the JSON object described, nothing else."

    /** [references] are plain-text extracts that were really retrieved (Wikipedia). */
    fun lessonSystem(study: Study): String = buildString {
        append("You are a patient teacher inside the StudyZen app. ")
        append("Write lesson notes for a ${study.level.label.lowercase()} student studying \"${study.title}\"")
        if (study.curriculum.isNotBlank()) append(" for ${study.curriculum}")
        append(". Use clear, simple language and Markdown: ## headings, numbered lists, bullet lists, **bold** for key terms. ")
        append("Do not use LaTeX or dollar signs: write maths in plain text with symbols such as × ÷ √ ² π and fractions like a/b. ")
        append("Never invent quotations, statistics, links or sources. If you are unsure of something, say so.")
    }

    fun lessonRequest(study: Study, lesson: Lesson, references: List<WikiArticle>): String = buildString {
        appendLine("Write the lesson \"${lesson.title}\" (part of \"${study.title}\").")
        if (lesson.summary.isNotBlank()) appendLine("Summary: ${lesson.summary}")
        if (lesson.objectives.isNotEmpty()) {
            appendLine("By the end the student should be able to:")
            lesson.objectives.forEach { appendLine("- $it") }
        }
        if (study.focus.isNotBlank()) appendLine("The student wants to focus on: ${study.focus}")
        appendLine()
        appendLine("Structure: ## Overview, ## Key ideas (numbered), ## Worked example, ## Check yourself (3 short questions, answers at the end), ## Summary. About 400 to 700 words.")
        if (references.isNotEmpty()) {
            appendLine()
            appendLine("Reference material that was retrieved from Wikipedia. Use it for facts where it helps, and do not contradict it:")
            references.forEach { appendLine("[${it.title}] ${it.extract.take(1200)}") }
        }
    }

    // --- Assessments ---------------------------------------------------------------------------

    fun assessmentSystem(study: Study): String = buildString {
        append("You write assessments for a ${study.level.label.lowercase()} student studying \"${study.title}\"")
        append(" (purpose: ${study.purpose.label.lowercase()}")
        if (study.curriculum.isNotBlank()) append(", ${study.curriculum}")
        append("). Questions must be accurate, unambiguous and suited to that goal. ")
        append("Reply with ONLY one JSON object, no Markdown fences and no commentary. Plain text only inside strings: no LaTeX.")
    }

    fun assessmentRequest(study: Study, lesson: Lesson?, kind: AssessmentKind, count: Int): String = buildString {
        if (lesson != null) {
            appendLine("Write ${kind.label.lowercase()} on the lesson \"${lesson.title}\".")
            if (lesson.summary.isNotBlank()) appendLine("Lesson summary: ${lesson.summary}")
            lesson.objectives.forEach { appendLine("Objective: $it") }
            lesson.material?.explanation?.takeIf { it.isNotBlank() }?.let { appendLine("Lesson text the student read:\n${it.take(1800)}") }
        } else {
            appendLine("Write ${kind.label.lowercase()} covering the whole study \"${study.title}\".")
            val titles = study.allLessons.map { it.title }.take(40)
            if (titles.isNotEmpty()) appendLine("Lessons: ${titles.joinToString("; ")}")
        }
        if (study.focus.isNotBlank()) appendLine("Emphasise: ${study.focus}")
        appendLine("Number of questions: $count.")
        appendLine()
        if (kind.objective) {
            appendLine("""Format: {"questions":[{"prompt":"...","options":["A","B","C","D"],"answer":0,"explanation":"why it is right","topic":"short topic name"}]}""")
            appendLine("Exactly 4 options each; \"answer\" is the zero-based index of the correct option; vary the position of the correct option.")
        } else {
            appendLine("""Format: {"questions":[{"prompt":"...","topic":"short topic name","criteria":"what a good answer includes","model_answer":"a good example answer"}]}""")
            when (kind) {
                AssessmentKind.Interview -> appendLine("Ask the way a real interviewer would for this goal, from warm-up to harder follow-ups.")
                AssessmentKind.Coding -> appendLine("Each prompt is a small coding task with clear inputs and outputs; the model answer is working code.")
                AssessmentKind.Written -> appendLine("Give a clear brief with a word count and marking criteria.")
                else -> Unit
            }
        }
    }

    const val FEEDBACK_SYSTEM = "You are a supportive tutor giving feedback on a student's answers. Be specific and kind, say what was good, " +
        "what is missing, and how to improve. You are not the examiner: never give marks or grades. Plain text only, no LaTeX. " +
        "Reply with ONLY one JSON object, no Markdown fences."

    fun feedbackRequest(assessment: Assessment): String = buildString {
        appendLine("Assessment: ${assessment.title}")
        appendLine("""Format: {"overall":"2-3 sentences","weak_topics":["topic to revise"],"feedback":[{"id":"question id","feedback":"..."}]}""")
        appendLine()
        assessment.questions.forEach { q ->
            appendLine("id: ${q.id}")
            appendLine("Question: ${q.prompt}")
            if (q.criteria.isNotBlank()) appendLine("A good answer includes: ${q.criteria}")
            if (q.explanation.isNotBlank()) appendLine("Example answer: ${q.explanation.take(800)}")
            appendLine("Student answer: ${assessment.answerFor(q.id)?.text?.ifBlank { "(left blank)" } ?: "(left blank)"}")
            appendLine()
        }
    }

    // --- Research ------------------------------------------------------------------------------

    const val RESEARCH_SYSTEM = "You summarise reference passages for a student. Use ONLY the passages given: do not add facts from memory. " +
        "Cite passages like [1]. If they do not answer the question, say so plainly. Plain text with short paragraphs or bullets, no LaTeX."

    fun researchRequest(question: String, articles: List<WikiArticle>): String = buildString {
        appendLine("Question: $question")
        appendLine()
        articles.forEachIndexed { i, a -> appendLine("[${i + 1}] ${a.title}: ${a.extract.take(1500)}") }
    }
}

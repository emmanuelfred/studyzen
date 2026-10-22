package com.fredrickemmanuel.studyzen.domain

import kotlinx.serialization.Serializable

// Everything the Study Space adds on top of a Study: why it is being studied, lesson materials,
// assessments, and the history of work done inside it.

/** Why the learner is studying. Decides which questions the creation flow asks. */
@Serializable
enum class StudyPurpose(
    val label: String,
    /** Title of the optional "which exam / curriculum" question; null means it is not asked. */
    val curriculumLabel: String?,
    val curriculumHint: String,
    /** Title of the optional date question; null means it is not asked. */
    val deadlineLabel: String?,
) {
    Skill("Learn a new skill or subject", null, "", null),
    Exam("Prepare for an examination", "Which exam?", "e.g. WAEC, JAMB, IGCSE, SAT, a university course exam", "Exam date"),
    Curriculum("Follow a curriculum or syllabus", "Curriculum or syllabus", "e.g. Cambridge A-Level Biology, Year 9 Maths", "Target date"),
    Interview("Prepare for a job interview", "Role or company", "e.g. Junior Android developer at a fintech", "Interview date"),
    Coursework("Finish a course or assignment", "Course or institution", "e.g. CSC 201, University of Lagos", "Due date"),
    Revision("Revise what I already learned", null, "", null),
    Other("Something else", null, "", null),
}

@Serializable
enum class LessonKind(val label: String) {
    Learn("Lesson"),
    Practice("Practice"),
    Revision("Revision"),
    Assessment("Checkpoint"),
}

@Serializable
enum class ResourceSource(val label: String) {
    /** Fetched from Wikipedia while the learner was online: a real article. */
    Wikipedia("Wikipedia"),
    /** A search the learner opens themselves (YouTube, the web). Not a specific video or page. */
    Search("Search link"),
}

@Serializable
data class LessonResource(
    val id: String,
    val title: String,
    val url: String,
    val source: ResourceSource,
    val snippet: String = "",
    val bookmarked: Boolean = false,
)

/** What the learner asked for on a lesson: an AI explanation plus sources that were really retrieved. */
@Serializable
data class LessonMaterial(
    /** Written by the AI (Markdown). Labelled as AI-generated wherever it is shown. */
    val explanation: String = "",
    val explanationBy: String = "",
    val resources: List<LessonResource> = emptyList(),
    val generatedAtEpochDay: Long = 0,
)

// --- Assessments ---------------------------------------------------------------------------------

@Serializable
enum class AssessmentKind(val label: String, val objective: Boolean, val description: String) {
    MultipleChoice("Multiple-choice quiz", true, "Pick the right answer. Marked instantly with explanations."),
    ShortAnswer("Short-answer questions", false, "Write a sentence or two. You get AI feedback."),
    Practice("Practice exercises", false, "Work problems out. You get AI feedback and a worked answer."),
    Written("Written assignment", false, "A longer piece of writing, with AI feedback against clear criteria."),
    Interview("Interview practice", false, "Answer questions like an interviewer would ask."),
    Coding("Coding challenge", false, "Write code for a small task. You get AI feedback."),
}

@Serializable
data class AssessmentQuestion(
    val id: String,
    val prompt: String,
    val topic: String = "",
    /** Choices for objective questions; empty for written answers. */
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    /** Why the answer is right (objective) or a model answer (written). */
    val explanation: String = "",
    /** What a good answer should include (written). */
    val criteria: String = "",
) {
    val isObjective: Boolean get() = options.isNotEmpty() && correctIndex in options.indices
}

@Serializable
data class AssessmentAnswer(
    val questionId: String,
    /** The learner's written answer. */
    val text: String = "",
    /** The option picked for an objective question, or -1. */
    val choice: Int = -1,
    /** AI feedback for a written answer. Not an official grade. */
    val feedback: String = "",
)

@Serializable
data class Assessment(
    val id: String,
    val studyId: String,
    val lessonId: String? = null,
    val title: String,
    val kind: AssessmentKind,
    val createdAtEpochDay: Long,
    val questions: List<AssessmentQuestion>,
    val answers: List<AssessmentAnswer> = emptyList(),
    val submitted: Boolean = false,
    /** AI comments on the whole attempt, and topics to revise. */
    val overallFeedback: String = "",
    val weakTopics: List<String> = emptyList(),
    val generatedBy: String = "",
) {
    fun answerFor(questionId: String): AssessmentAnswer? = answers.firstOrNull { it.questionId == questionId }

    private val marked: List<AssessmentQuestion> get() = questions.filter { it.isObjective }

    /** Number of objective questions answered correctly. */
    val correct: Int get() = marked.count { q -> answerFor(q.id)?.choice == q.correctIndex }
    val total: Int get() = marked.size
    val percent: Int get() = if (total == 0) 0 else correct * 100 / total
}

// --- History -----------------------------------------------------------------------------------

@Serializable
data class ScanTurnRecord(
    val fromUser: Boolean,
    val text: String,
    val shown: String = text,
    val typed: String? = null,
    val provider: String? = null,
)

/** A scan-to-solve conversation, kept so the learner can reopen it from the study. */
@Serializable
data class ScanRecord(
    val id: String,
    val studyId: String?,
    val title: String,
    val turns: List<ScanTurnRecord>,
    /** The photo, when the learner saved one with it. */
    val imageFile: String? = null,
    val updatedAtEpochDay: Long,
    /** The lesson the learner was on when they started, so it can be offered as context again. */
    val lessonId: String? = null,
)

@Serializable
enum class LogKind(val label: String) {
    Dictionary("Dictionary"),
    Calculator("Calculator"),
}

/** A small record of tool use inside a study, such as a word looked up or a calculation. */
@Serializable
data class StudyLogEntry(
    val id: String,
    val studyId: String,
    val kind: LogKind,
    val text: String,
    val detail: String = "",
    val epochDay: Long,
)

package com.fredrickemmanuel.studyzen.domain

import com.fredrickemmanuel.studyzen.core.time.DayOfWeek
import kotlinx.serialization.Serializable

/** What a Study is about. Drives the icon and color shown for it. */
@Serializable
enum class Subject(val label: String) {
    Programming("Programming"),
    Mathematics("Mathematics"),
    Science("Science"),
    Languages("Languages"),
    Business("Business"),
    Arts("Arts"),
    Other("Other"),
}

@Serializable
enum class KnowledgeLevel(val label: String) {
    Beginner("Beginner"),
    Intermediate("Intermediate"),
    Advanced("Advanced"),
}

@Serializable
enum class Priority(val label: String) { Low("Low"), Medium("Medium"), High("High") }

/** The label shown under a session ("Study", "Review", "Practice"). */
@Serializable
enum class SessionKind(val label: String) {
    Study("Study"),
    Review("Review"),
    Practice("Practice"),
}

@Serializable
enum class SessionStatus { Scheduled, Completed, Skipped }

// ---------------------------------------------------------------------------------------------
// Study workspace
// ---------------------------------------------------------------------------------------------

@Serializable
data class Lesson(
    val id: String,
    val title: String,
    val summary: String,
    val completed: Boolean = false,
    /** What the learner should be able to do after this lesson. */
    val objectives: List<String> = emptyList(),
    val kind: LessonKind = LessonKind.Learn,
    /** Explanation and sources the learner asked for. Null until they ask for materials. */
    val material: LessonMaterial? = null,
)

@Serializable
data class LearningModule(
    val id: String,
    val title: String,
    val lessons: List<Lesson>,
)

@Serializable
data class Study(
    val id: String,
    val title: String,
    val description: String,
    val objective: String,
    val subject: Subject,
    val level: KnowledgeLevel,
    val modules: List<LearningModule> = emptyList(),
    val archived: Boolean = false,
    /** Where the learner stopped, so reopening the Study can continue from there. */
    val lastLessonId: String? = null,
    val createdAtEpochDay: Long,
    /** Why the learner is studying this: an exam, an interview, a curriculum... */
    val purpose: StudyPurpose = StudyPurpose.Skill,
    /** The exam, syllabus, institution or role, when the purpose has one. */
    val curriculum: String = "",
    /** Specific areas the learner wants to focus on. */
    val focus: String = "",
    /** Exam, interview or due date, when there is one. */
    val deadlineEpochDay: Long? = null,
    /** True: lessons open in order. False: the learner may open any lesson. */
    val guided: Boolean = false,
    /** True when the lessons were proposed by an AI (and then reviewed by the learner). */
    val aiStructured: Boolean = false,
) {
    val allLessons: List<Lesson> get() = modules.flatMap { it.lessons }
    val completedLessons: Int get() = allLessons.count { it.completed }

    fun lesson(id: String?): Lesson? = allLessons.firstOrNull { it.id == id }
    fun moduleOf(lessonId: String): LearningModule? = modules.firstOrNull { m -> m.lessons.any { it.id == lessonId } }

    /** The first lesson that is not finished yet. */
    val firstUnfinished: Lesson? get() = allLessons.firstOrNull { !it.completed }

    /** Where "Continue" should go: the lesson the learner was on, otherwise the first unfinished one. */
    val currentLesson: Lesson? get() = lesson(lastLessonId) ?: firstUnfinished ?: allLessons.firstOrNull()

    fun previousOf(lessonId: String): Lesson? {
        val i = allLessons.indexOfFirst { it.id == lessonId }
        return if (i > 0) allLessons[i - 1] else null
    }

    fun nextOf(lessonId: String): Lesson? {
        val i = allLessons.indexOfFirst { it.id == lessonId }
        return if (i >= 0) allLessons.getOrNull(i + 1) else null
    }

    /**
     * Finished lessons can always be revisited. In a guided study the next lesson opens once the
     * ones before it are done; a flexible study opens everything.
     */
    fun canOpen(lessonId: String): Boolean {
        if (!guided) return true
        val lessons = allLessons
        val index = lessons.indexOfFirst { it.id == lessonId }
        if (index < 0) return false
        val firstOpen = lessons.indexOfFirst { !it.completed }
        return firstOpen < 0 || index <= firstOpen
    }

    /** 0f..1f. Zero when no learning path exists yet. */
    val progress: Float
        get() {
            val total = allLessons.size
            return if (total == 0) 0f else completedLessons.toFloat() / total
        }
}

@Serializable
enum class NoteSource(val label: String) {
    Manual("Note"),
    Ai("AI explanation"),
    Research("Research"),
    Solver("Solved problem"),
    Dictionary("Definition"),
}

@Serializable
data class Note(
    val id: String,
    /** The Study this note belongs to, or null for a general note. */
    val studyId: String?,
    val title: String,
    val body: String,
    val source: NoteSource = NoteSource.Manual,
    val updatedAtEpochDay: Long,
    val pinned: Boolean = false,
    /**
     * The note as the learner sees it: paragraphs, checklists, photos and voice notes in order.
     * Empty for older notes, which are shown from [body]. [body] always holds a plain-text copy,
     * used for search and for the preview in the list.
     */
    val blocks: List<NoteBlock> = emptyList(),
)

/** A word the learner saved from the dictionary. [id] is the lower-case word. */
@Serializable
data class FavoriteWord(
    val id: String,
    val word: String,
    val phonetic: String? = null,
    val partOfSpeech: String = "",
    val definition: String,
    val example: String? = null,
    val savedAtEpochDay: Long,
)

@Serializable
data class ChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val saved: Boolean = false,
)

/** One finding from the research assistant. [sourceTitle] stays null until a real source exists. */
@Serializable
data class Finding(
    val text: String,
    val sourceTitle: String? = null,
    val sourceUrl: String? = null,
) {
    val isSourced: Boolean get() = sourceTitle != null
}

@Serializable
data class ResearchItem(
    val id: String,
    val studyId: String,
    val question: String,
    val findings: List<Finding>,
    val saved: Boolean = false,
)

// ---------------------------------------------------------------------------------------------
// Quizzes
// ---------------------------------------------------------------------------------------------

@Serializable
data class QuizQuestion(
    val id: String,
    val topic: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
)

@Serializable
enum class QuizDifficulty(val label: String) { Easy("Easy"), Medium("Medium"), Hard("Hard") }

@Serializable
data class AssessmentResult(
    val id: String,
    val studyId: String,
    val title: String,
    val correct: Int,
    val total: Int,
    val epochDay: Long,
    val weakTopics: List<String>,
) {
    val percent: Int get() = if (total == 0) 0 else (correct * 100) / total
}

// ---------------------------------------------------------------------------------------------
// Planning
// ---------------------------------------------------------------------------------------------

@Serializable
data class Reminder(
    val enabled: Boolean = true,
    val minutesBefore: Int = 10,
    /** Extra weekdays the reminder repeats on (empty = only for this session). */
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val sound: String = "Gentle chime",
    val vibrate: Boolean = true,
)

@Serializable
data class StudySession(
    val id: String,
    val title: String,
    val studyId: String?,
    val subject: Subject,
    val epochDay: Long,
    val startMinute: Int,
    val durationMinutes: Int,
    val kind: SessionKind = SessionKind.Study,
    val priority: Priority = Priority.Medium,
    val status: SessionStatus = SessionStatus.Scheduled,
    /** Minutes actually studied; only active (not paused) time counts. */
    val actualMinutes: Int = 0,
    val planId: String? = null,
    val reminder: Reminder? = null,
    val reflection: String = "",
    /** An assignment (practice task or homework) is due with this session. */
    val hasAssignment: Boolean = false,
    /** Belongs to the built-in sample data, which never rings the phone. */
    val isSample: Boolean = false,
) {
    val endMinute: Int get() = startMinute + durationMinutes
}

/** A stretch of the day, in minutes since midnight. [endMinute] is exclusive. */
@Serializable
data class TimeBlock(val startMinute: Int, val endMinute: Int) {
    val minutes: Int get() = endMinute - startMinute

    /** Blocks that only touch (one ends exactly when the next starts) do not overlap. */
    fun overlaps(other: TimeBlock): Boolean = startMinute < other.endMinute && other.startMinute < endMinute
}

/** How often a study plan hands the learner an assignment. */
@Serializable
enum class AssignmentCadence(val label: String) {
    None("No assignments"),
    EverySession("After every session"),
    Weekly("Once a week"),
}

@Serializable
data class StudyPlan(
    val id: String,
    val name: String,
    val goal: String,
    val studyIds: List<String>,
    val startEpochDay: Long,
    val endEpochDay: Long,
    /** Each study day maps to its own sessions, which can differ from day to day and never overlap. */
    val weeklyBlocks: Map<DayOfWeek, List<TimeBlock>>,
    val assignment: AssignmentCadence = AssignmentCadence.None,
    val includeRevision: Boolean = true,
    val reminder: Reminder = Reminder(),
    val priority: Priority = Priority.Medium,
    val active: Boolean = true,
) {
    val studyDays: Set<DayOfWeek> get() = weeklyBlocks.filterValues { it.isNotEmpty() }.keys
    val sessionsPerWeek: Int get() = weeklyBlocks.values.sumOf { it.size }
    val minutesPerWeek: Int get() = weeklyBlocks.values.sumOf { blocks -> blocks.sumOf { it.minutes } }
}

// ---------------------------------------------------------------------------------------------
// User
// ---------------------------------------------------------------------------------------------

@Serializable
data class UserProfile(
    val name: String = "Student",
    val email: String = "",
    val interests: Set<Subject> = emptySet(),
    val studyDays: Set<DayOfWeek> = setOf(
        DayOfWeek.Monday, DayOfWeek.Tuesday, DayOfWeek.Wednesday, DayOfWeek.Thursday, DayOfWeek.Friday,
    ),
    val preferredStartMinute: Int = 18 * 60,
    val preferredEndMinute: Int = 21 * 60,
    val timezone: String = "Device time zone",
    val remindersEnabled: Boolean = true,
    val scheduleChangeAlerts: Boolean = true,
    val learningSuggestions: Boolean = true,
    val defaultSound: String = "Gentle chime",
    val vibrate: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStartMinute: Int = 22 * 60,
    val quietEndMinute: Int = 7 * 60,
    val personalized: Boolean = false,
)

val SoundOptions = listOf("Gentle chime", "Soft bell", "Digital", "Silent")

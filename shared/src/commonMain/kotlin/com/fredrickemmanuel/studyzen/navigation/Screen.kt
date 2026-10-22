package com.fredrickemmanuel.studyzen.navigation

import com.fredrickemmanuel.studyzen.feature.auth.data.CodePurpose

/** The tools a study offers. Each one opens on what was already done there before offering a new item. */
enum class StudyTool(val label: String, val newLabel: String) {
    Chat("Ask AI & scan", "New conversation"),
    Notes("Notes", "New note"),
    Research("Research", "New search"),
    Dictionary("Dictionary", "Look up a word"),
    Calculator("Calculator", "Open calculator"),
}

/** Every destination in the app. Add new screens here as they are built. */
sealed interface Screen {
    // Entry and account
    data object Onboarding : Screen
    data object Login : Screen
    data object SignUp : Screen
    data object ForgotPassword : Screen
    data class VerifyCode(val email: String, val purpose: CodePurpose) : Screen
    data class ResetPassword(val email: String, val code: String) : Screen
    data class Personalization(val fromProfile: Boolean) : Screen
    data object NotificationPermission : Screen

    /** Home, Plan, Progress and Profile tabs with the bottom bar. */
    data object Main : Screen

    // Studies
    data object StudyLibrary : Screen
    data class StudyEditor(val studyId: String?) : Screen
    data class StudyDetail(val studyId: String) : Screen
    data class Assistant(val studyId: String, val lessonId: String? = null) : Screen
    data class LearningPath(val studyId: String) : Screen
    data class Lesson(val studyId: String, val lessonId: String) : Screen
    data class Research(val studyId: String, val itemId: String? = null) : Screen
    data class ToolHistory(val studyId: String, val tool: StudyTool, val lessonId: String? = null) : Screen
    data object Notes : Screen
    data class NoteEditor(val studyId: String?, val noteId: String?) : Screen
    data class QuizSetup(val studyId: String, val lessonId: String? = null) : Screen
    data class Quiz(val studyId: String, val assessmentId: String) : Screen
    data class QuizResult(val studyId: String, val assessmentId: String) : Screen

    // Planning and sessions
    data class PlanBuilder(val planId: String?) : Screen
    data class SessionEditor(val sessionId: String?, val epochDay: Long) : Screen
    data object Reminders : Screen
    data object StartStudying : Screen
    data class StudySession(val sessionId: String) : Screen
    data class SessionSummary(val sessionId: String) : Screen

    // Tools
    data class Scanner(val studyId: String?, val scanId: String? = null, val lessonId: String? = null) : Screen
    data class Solution(val studyId: String?, val problem: String) : Screen
    data class Calculator(val studyId: String?, val expression: String? = null) : Screen
    data class Dictionary(val studyId: String?, val word: String? = null) : Screen

    // Settings
    data object EditProfile : Screen
    data object NotificationSettings : Screen
    data object Security : Screen
    data object AiSettings : Screen

    /**
     * Text form used to keep the back stack across Android configuration changes and process
     * death. Format: `name|arg1|arg2` (at most two arguments). The last argument may contain any character.
     */
    fun encode(): String = when (this) {
        Onboarding -> "onboarding"
        Login -> "login"
        SignUp -> "signup"
        ForgotPassword -> "forgot"
        is VerifyCode -> "verify|${purpose.name}|$email"
        is ResetPassword -> "reset|$code|$email"
        is Personalization -> "personalize|$fromProfile"
        NotificationPermission -> "notifperm"
        Main -> "main"
        StudyLibrary -> "library"
        is StudyEditor -> "studyeditor|${studyId.enc()}"
        is StudyDetail -> "study|$studyId"
        is Assistant -> "assistant|$studyId|${lessonId.enc()}"
        is LearningPath -> "path|$studyId"
        is Lesson -> "lesson|$studyId|$lessonId"
        is Research -> "research|$studyId|${itemId.enc()}"
        is ToolHistory -> "toolhistory|$studyId|${tool.name},${lessonId.enc()}"
        Notes -> "notes"
        is NoteEditor -> "note|${studyId.enc()}|${noteId.enc()}"
        is QuizSetup -> "quizsetup|$studyId|${lessonId.enc()}"
        is Quiz -> "quiz|$studyId|$assessmentId"
        is QuizResult -> "quizresult|$studyId|$assessmentId"
        is PlanBuilder -> "plan|${planId.enc()}"
        is SessionEditor -> "sessioneditor|${sessionId.enc()}|$epochDay"
        Reminders -> "reminders"
        StartStudying -> "startstudying"
        is StudySession -> "session|$sessionId"
        is SessionSummary -> "summary|$sessionId"
        is Scanner -> "scanner|${studyId.enc()}|${scanId.enc()},${lessonId.enc()}"
        is Solution -> "solution|${studyId.enc()}|$problem"
        is Calculator -> "calculator|${studyId.enc()}|${expression.enc()}"
        is Dictionary -> "dictionary|${studyId.enc()}|${word.enc()}"
        EditProfile -> "editprofile"
        NotificationSettings -> "notifsettings"
        Security -> "security"
        AiSettings -> "aisettings"
    }

    companion object {
        fun decode(value: String): Screen {
            val p = value.split("|", limit = 3)
            fun arg(i: Int): String = p.getOrElse(i) { "" }
            return when (p[0]) {
                "login" -> Login
                "signup" -> SignUp
                "forgot" -> ForgotPassword
                "verify" -> VerifyCode(
                    email = arg(2),
                    purpose = CodePurpose.entries.firstOrNull { it.name == arg(1) } ?: CodePurpose.VerifyEmail,
                )
                "reset" -> ResetPassword(email = arg(2), code = arg(1))
                "personalize" -> Personalization(fromProfile = arg(1) == "true")
                "notifperm" -> NotificationPermission
                "main" -> Main
                "library" -> StudyLibrary
                "studyeditor" -> StudyEditor(arg(1).dec())
                "study" -> StudyDetail(arg(1))
                "assistant" -> Assistant(arg(1), arg(2).dec())
                "path" -> LearningPath(arg(1))
                "lesson" -> Lesson(arg(1), arg(2))
                "research" -> Research(arg(1), arg(2).dec())
                "toolhistory" -> {
                    val parts = arg(2).split(",", limit = 2)
                    ToolHistory(arg(1), StudyTool.entries.firstOrNull { it.name == parts[0] } ?: StudyTool.Chat, parts.getOrElse(1) { "" }.dec())
                }
                "notes" -> Notes
                "note" -> NoteEditor(arg(1).dec(), arg(2).dec())
                "quizsetup" -> QuizSetup(arg(1), arg(2).dec())
                "quiz" -> Quiz(arg(1), arg(2))
                "quizresult" -> QuizResult(arg(1), arg(2))
                "plan" -> PlanBuilder(arg(1).dec())
                "sessioneditor" -> SessionEditor(arg(1).dec(), arg(2).toLongOrNull() ?: 0L)
                "reminders" -> Reminders
                "startstudying" -> StartStudying
                "session" -> StudySession(arg(1))
                "summary" -> SessionSummary(arg(1))
                "scanner" -> {
                    val parts = arg(2).split(",", limit = 2)
                    Scanner(arg(1).dec(), parts[0].dec(), parts.getOrElse(1) { "" }.dec())
                }
                "solution" -> Solution(arg(1).dec(), arg(2))
                "calculator" -> Calculator(arg(1).dec(), arg(2).dec())
                "dictionary" -> Dictionary(arg(1).dec(), arg(2).dec())
                "editprofile" -> EditProfile
                "notifsettings" -> NotificationSettings
                "security" -> Security
                "aisettings" -> AiSettings
                else -> Onboarding
            }
        }

        private fun String?.enc(): String = this ?: "-"
        private fun String.dec(): String? = if (this == "-" || this.isEmpty()) null else this
    }
}

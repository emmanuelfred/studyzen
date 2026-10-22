package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.feature.auth.data.CodePurpose
import com.fredrickemmanuel.studyzen.navigation.AppNavigator
import com.fredrickemmanuel.studyzen.navigation.Screen
import com.fredrickemmanuel.studyzen.navigation.StudyTool
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationTest {

    @Test
    fun screensSurviveEncodeAndDecode() {
        val screens = listOf(
            Screen.Onboarding,
            Screen.Login,
            Screen.SignUp,
            Screen.ForgotPassword,
            Screen.Main,
            Screen.StudyLibrary,
            Screen.StudyEditor(null),
            Screen.StudyEditor("study-1"),
            Screen.StudyDetail("study-1"),
            Screen.Lesson("study-1", "les-2"),
            Screen.NoteEditor("study-1", null),
            Screen.NoteEditor("study-1", "note-9"),
            Screen.NoteEditor(null, null),
            Screen.NoteEditor(null, "note-3"),
            Screen.Notes,
            Screen.AiSettings,
            Screen.LearningPath("study-1"),
            Screen.Assistant("study-1", null),
            Screen.Assistant("study-1", "les-2"),
            Screen.QuizSetup("study-1", null),
            Screen.QuizSetup("study-1", "les-2"),
            Screen.Quiz("study-1", "asm-1"),
            Screen.QuizResult("study-1", "asm-1"),
            Screen.Scanner(null, null, null),
            Screen.Scanner("study-1", null, "les-2"),
            Screen.Scanner("study-1", "scan-4", "les-2"),
            Screen.ToolHistory("study-1", StudyTool.Notes, null),
            Screen.ToolHistory("study-1", StudyTool.Chat, "les-2"),
            Screen.Research("study-1", null),
            Screen.Research("study-1", "res-7"),
            Screen.Dictionary("study-1", "ephemeral"),
            Screen.Calculator("study-1", "2+2 | 4"),
            Screen.Scanner("study-1", "scan-4"),
            Screen.Dictionary("study-1"),
            Screen.Dictionary(null),
            Screen.PlanBuilder(null),
            Screen.SessionEditor("ses-1", 20_746L),
            Screen.SessionEditor(null, 20_746L),
            Screen.StudySession("ses-1"),
            Screen.StartStudying,
            Screen.SessionSummary("ses-1"),
            Screen.Solution("study-1", "2x + 3 = 11 | solve for x"),
            Screen.Solution(null, "x^2"),
            Screen.Calculator(null),
            Screen.Personalization(true),
            Screen.NotificationSettings,
            Screen.VerifyCode("a@b.co", CodePurpose.VerifyEmail),
            Screen.VerifyCode("odd|name@b.co", CodePurpose.ResetPassword),
            Screen.ResetPassword("a@b.co", "123456"),
        )
        screens.forEach { assertEquals(it, Screen.decode(it.encode())) }
    }

    @Test
    fun backStackBehaves() {
        val navigator = AppNavigator(listOf(Screen.Onboarding))
        assertFalse(navigator.canGoBack)

        navigator.navigate(Screen.Login)
        navigator.navigate(Screen.SignUp)
        assertTrue(navigator.canGoBack)
        assertEquals(Screen.SignUp, navigator.current)

        navigator.back()
        assertEquals(Screen.Login, navigator.current)

        navigator.replaceTop(Screen.ForgotPassword)
        assertEquals(Screen.ForgotPassword, navigator.current)
        navigator.back()
        assertEquals(Screen.Onboarding, navigator.current)

        navigator.navigate(Screen.Login)
        navigator.replaceAll(Screen.Main)
        assertEquals(Screen.Main, navigator.current)
        assertFalse(navigator.canGoBack)
    }

    @Test
    fun backToMainSkipsEverythingAbove() {
        val navigator = AppNavigator(listOf(Screen.Main))
        navigator.navigate(Screen.StudyDetail("s"))
        navigator.navigate(Screen.PlanBuilder(null))
        navigator.backToMain()
        assertEquals(Screen.Main, navigator.current)
        assertFalse(navigator.canGoBack)
    }
}

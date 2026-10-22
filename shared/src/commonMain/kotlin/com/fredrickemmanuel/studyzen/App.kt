package com.fredrickemmanuel.studyzen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.fredrickemmanuel.studyzen.core.designsystem.theme.StudyZenTheme
import com.fredrickemmanuel.studyzen.feature.auth.google.FakeGoogleSignInClient
import com.fredrickemmanuel.studyzen.feature.auth.google.GoogleSignInClient
import com.fredrickemmanuel.studyzen.feature.media.MediaPlatform
import com.fredrickemmanuel.studyzen.feature.media.NoMediaPlatform
import com.fredrickemmanuel.studyzen.feature.reminders.NoReminderPlatform
import com.fredrickemmanuel.studyzen.feature.reminders.ReminderPlatform
import com.fredrickemmanuel.studyzen.navigation.AppNavHost

/**
 * Root of the app, shared by Android, iOS, desktop and web.
 *
 * Each platform entry point passes in its real [googleSignInClient]. Until one is provided,
 * a fake client is used so the flow can still be tried.
 */
@Composable
fun App(
    googleSignInClient: GoogleSignInClient = remember { FakeGoogleSignInClient() },
    reminderPlatform: ReminderPlatform = NoReminderPlatform,
    mediaPlatform: MediaPlatform = NoMediaPlatform,
) {
    StudyZenTheme {
        AppNavHost(
            googleClient = googleSignInClient,
            reminderPlatform = reminderPlatform,
            mediaPlatform = mediaPlatform,
        )
    }
}

@Composable
@Preview
private fun AppPreview() {
    App()
}

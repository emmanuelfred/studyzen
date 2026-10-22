package com.fredrickemmanuel.studyzen

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import android.content.Intent
import com.fredrickemmanuel.studyzen.navigation.DeepLinks

/** OAuth "Web application" client ID from Google Cloud Console (not a secret). */
private const val GOOGLE_WEB_CLIENT_ID =
    "61437022052-1eoho25is83kc8iurhrrgqg3s2il36qs.apps.googleusercontent.com"

class MainActivity : ComponentActivity() {
    private var reminders: AndroidReminderPlatform? = null
    private var media: AndroidMediaPlatform? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is light-only, so always use dark status/navigation bar icons,
        // even when the phone itself is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        readDeepLink(intent)

        val googleClient = AndroidGoogleSignInClient(this, GOOGLE_WEB_CLIENT_ID)
        val reminderPlatform = AndroidReminderPlatform(this)
        reminders = reminderPlatform
        val mediaPlatform = AndroidMediaPlatform(this)
        media = mediaPlatform
        setContent {
            App(
                googleSignInClient = googleClient,
                reminderPlatform = reminderPlatform,
                mediaPlatform = mediaPlatform,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readDeepLink(intent)
    }

    /** A tapped reminder carries studyzen://session/<id>. */
    private fun readDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "studyzen" && data.host == "session") {
            data.lastPathSegment?.let { DeepLinks.pendingSessionId = it }
        }
    }

    override fun onResume() {
        super.onResume()
        reminders?.refresh() // the learner may have changed a notification or alarm setting
    }

    override fun onDestroy() {
        media?.release()
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

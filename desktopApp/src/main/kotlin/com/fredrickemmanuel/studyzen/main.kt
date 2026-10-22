package com.fredrickemmanuel.studyzen

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val icon = BitmapPainter(useResource("icon.png", ::loadImageBitmap))

    Window(
        onCloseRequest = ::exitApplication,
        title = "StudyZen",
        icon = icon,
        // Phone-shaped window while the app is mobile-first.
        state = rememberWindowState(width = 430.dp, height = 900.dp),
    ) {
        App(
            googleSignInClient = remember { DesktopGoogleSignInClient(DesktopGoogleConfig.CLIENT_ID, DesktopGoogleConfig.CLIENT_SECRET) },
            mediaPlatform = remember { DesktopMediaPlatform() },
        )
    }
}

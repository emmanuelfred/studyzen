package com.fredrickemmanuel.studyzen.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Where the app should go once it is open and signed in, set by the platform (for example when the learner
 * taps a reminder). Held here so a tap that launches the app is not lost while the app is still starting.
 */
object DeepLinks {
    /** A session id to open in the study timer, or null. */
    var pendingSessionId by mutableStateOf<String?>(null)
}

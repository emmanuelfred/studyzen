package com.fredrickemmanuel.studyzen

import java.util.Properties

/**
 * Google "Desktop app" OAuth client used for "Continue with Google" on desktop.
 *
 * The values are NOT stored in git. They are read from, in order:
 *  1. a file `desktopApp/src/main/resources/google-desktop.properties` (ignored by git), or
 *  2. the environment variables STUDYZEN_GOOGLE_CLIENT_ID and STUDYZEN_GOOGLE_CLIENT_SECRET.
 *
 * With neither, the Google button explains that it is not set up and everything else still works.
 * See `google-desktop.properties.example` for the file format.
 */
object DesktopGoogleConfig {
    private val file: Properties = Properties().also { props ->
        DesktopGoogleConfig::class.java.getResourceAsStream("/google-desktop.properties")?.use { props.load(it) }
    }

    val CLIENT_ID: String = file.getProperty("client_id") ?: System.getenv("STUDYZEN_GOOGLE_CLIENT_ID").orEmpty()
    val CLIENT_SECRET: String = file.getProperty("client_secret") ?: System.getenv("STUDYZEN_GOOGLE_CLIENT_SECRET").orEmpty()
}

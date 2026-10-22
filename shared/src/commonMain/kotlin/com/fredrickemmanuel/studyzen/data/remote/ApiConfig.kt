package com.fredrickemmanuel.studyzen.data.remote

/** Where the StudyZen API lives, and a switch to run the app without a server. */
object ApiConfig {
    /**
     * Set this to your computer's address when testing on a real phone, for example
     * "http://192.168.1.20:4000" (phone and computer on the same Wi-Fi). Leave null otherwise.
     * For a published app this becomes your HTTPS server address.
     */
    var baseUrlOverride: String? = "https://studyzen-api.markpayhub.com"

    val baseUrl: String get() = (baseUrlOverride ?: platformDefaultBaseUrl()).trimEnd('/')

    /** true = use the built-in fake login (no server needed), handy for UI work. */
    const val USE_FAKE_BACKEND: Boolean = false
}

/** The development server address as seen from this platform. */
expect fun platformDefaultBaseUrl(): String

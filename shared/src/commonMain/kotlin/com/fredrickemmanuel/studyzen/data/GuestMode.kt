package com.fredrickemmanuel.studyzen.data

/**
 * "Try without an account": the app works fully on this device with no server. Remembered across restarts
 * so the learner is not sent back to the login screen. Signing in later backs up whatever was done as a guest.
 */
object GuestMode {
    private const val KEY = "guest.v1"
    private val storage: KeyValueStore by lazy { platformKeyValueStore() }

    var enabled: Boolean
        get() = storage.get(KEY) == "1"
        set(value) {
            if (value) storage.put(KEY, "1") else storage.remove(KEY)
        }
}

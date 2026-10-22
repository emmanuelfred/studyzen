package com.fredrickemmanuel.studyzen.data

import com.fredrickemmanuel.studyzen.data.remote.ApiUser
import com.fredrickemmanuel.studyzen.data.remote.SessionResponse
import com.russhwolf.settings.Settings

/** Tiny key-value storage so the session logic can be tested without a device. */
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

class MemoryKeyValueStore : KeyValueStore {
    private val map = mutableMapOf<String, String>()
    override fun get(key: String) = map[key]
    override fun put(key: String, value: String) { map[key] = value }
    override fun remove(key: String) { map.remove(key) }
}

/** Saves to the platform's app-private storage (SharedPreferences, NSUserDefaults, localStorage...). */
class SettingsKeyValueStore(private val settings: Settings) : KeyValueStore {
    override fun get(key: String): String? = settings.getStringOrNull(key)
    override fun put(key: String, value: String) = settings.putString(key, value)
    override fun remove(key: String) = settings.remove(key)
}

fun platformKeyValueStore(): KeyValueStore =
    try {
        SettingsKeyValueStore(Settings())
    } catch (e: Exception) {
        MemoryKeyValueStore() // storage blocked (private browsing...): the login lasts until the app closes
    }

/**
 * Keeps the login on the device so the learner stays signed in after closing the app.
 * Stored in app-private storage; hardware-backed encryption (Android Keystore, iOS Keychain)
 * is a planned hardening step.
 */
class SessionStore(private val store: KeyValueStore = platformKeyValueStore()) {

    val refreshToken: String? get() = store.get(REFRESH)
    val accessToken: String? get() = store.get(ACCESS)

    val user: ApiUser?
        get() {
            val id = store.get(USER_ID) ?: return null
            return ApiUser(
                id = id,
                name = store.get(USER_NAME).orEmpty(),
                email = store.get(USER_EMAIL).orEmpty(),
                emailVerified = true,
                hasPassword = store.get(HAS_PASSWORD) != "false",
            )
        }

    val isSignedIn: Boolean get() = refreshToken != null && user != null

    fun save(session: SessionResponse) {
        store.put(ACCESS, session.tokens.accessToken)
        store.put(REFRESH, session.tokens.refreshToken)
        store.put(USER_ID, session.user.id)
        store.put(USER_NAME, session.user.name)
        store.put(USER_EMAIL, session.user.email)
        store.put(HAS_PASSWORD, session.user.hasPassword.toString())
    }

    /** Keeps the saved profile in step after the learner edits it. */
    fun updateUser(name: String? = null, hasPassword: Boolean? = null) {
        if (name != null) store.put(USER_NAME, name)
        if (hasPassword != null) store.put(HAS_PASSWORD, hasPassword.toString())
    }

    fun clear() {
        listOf(ACCESS, REFRESH, USER_ID, USER_NAME, USER_EMAIL, HAS_PASSWORD).forEach(store::remove)
    }

    private companion object {
        const val ACCESS = "session.access"
        const val REFRESH = "session.refresh"
        const val USER_ID = "session.user.id"
        const val USER_NAME = "session.user.name"
        const val USER_EMAIL = "session.user.email"
        const val HAS_PASSWORD = "session.user.hasPassword"
    }
}

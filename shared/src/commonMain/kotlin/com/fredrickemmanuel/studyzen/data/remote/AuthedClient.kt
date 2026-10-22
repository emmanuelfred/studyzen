package com.fredrickemmanuel.studyzen.data.remote

import com.fredrickemmanuel.studyzen.data.SessionStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val SESSION_ENDED = "Your session ended. Please log in again."

/**
 * Calls endpoints that need a login. Sends the saved access token; when the server says it has
 * expired, trades the refresh token for a new pair once and repeats the call.
 *
 * The refresh runs under a lock and checks the token again inside it, so several calls that expire
 * together share one refresh (the server treats a reused refresh token as theft and ends the session).
 */
class AuthedClient(
    private val client: ApiClient = ApiClient(),
    private val sessions: SessionStore = SessionStore(),
    private val auth: AuthApi = AuthApi(client),
) {
    private val refreshLock = Mutex()

    suspend fun get(path: String): String = withToken { client.get(path, it) }
    suspend fun post(path: String, jsonBody: String): String = withToken { client.post(path, jsonBody, it) }
    suspend fun patch(path: String, jsonBody: String): String = withToken { client.patch(path, jsonBody, it) }

    private suspend fun withToken(call: suspend (token: String) -> String): String {
        val token = sessions.accessToken ?: throw ApiException(401, "UNAUTHORIZED", SESSION_ENDED)
        try {
            return call(token)
        } catch (e: ApiException) {
            if (e.status != 401 || e.code != "TOKEN_EXPIRED") throw e
        }
        return call(refreshedToken(staleToken = token))
    }

    private suspend fun refreshedToken(staleToken: String): String = refreshLock.withLock {
        val current = sessions.accessToken
        if (current != null && current != staleToken) return current // another call already refreshed

        val refresh = sessions.refreshToken ?: throw ApiException(401, "UNAUTHORIZED", SESSION_ENDED)
        try {
            val session = auth.refresh(refresh)
            sessions.save(session)
            session.tokens.accessToken
        } catch (e: ApiException) {
            if (e.status == 401) {
                sessions.clear()
                throw ApiException(401, "UNAUTHORIZED", SESSION_ENDED)
            }
            throw e
        }
    }
}

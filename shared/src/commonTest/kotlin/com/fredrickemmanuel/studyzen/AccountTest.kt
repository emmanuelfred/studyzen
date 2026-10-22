package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.data.MemoryKeyValueStore
import com.fredrickemmanuel.studyzen.data.SessionStore
import com.fredrickemmanuel.studyzen.data.remote.AccountApi
import com.fredrickemmanuel.studyzen.data.remote.ApiClient
import com.fredrickemmanuel.studyzen.data.remote.ApiUser
import com.fredrickemmanuel.studyzen.data.remote.ApiTokens
import com.fredrickemmanuel.studyzen.data.remote.AuthedClient
import com.fredrickemmanuel.studyzen.data.remote.SessionResponse
import com.fredrickemmanuel.studyzen.feature.account.RemoteAccountRepository
import com.fredrickemmanuel.studyzen.feature.auth.data.AuthResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val userJson = """{"id":"u1","name":"Ada","email":"ada@example.com","emailVerified":true}"""
    private val expired = """{"error":{"message":"Your session expired.","code":"TOKEN_EXPIRED"}}"""

    private fun signedIn(): SessionStore = SessionStore(MemoryKeyValueStore()).also {
        it.save(SessionResponse(ApiUser("u1", "Ada", "ada@example.com"), ApiTokens("old-access", "old-refresh")))
    }

    private fun repo(sessions: SessionStore, log: MutableList<String>, handler: (String, String?) -> Pair<HttpStatusCode, String>): RemoteAccountRepository {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            log += "${request.method.value} $path ${request.headers[HttpHeaders.Authorization]}"
            val (status, body) = handler(path, request.body.toByteArray().decodeToString())
            respond(body, status, json)
        }
        val client = ApiClient(HttpClient(engine), baseUrl = { "http://test" })
        return RemoteAccountRepository(AccountApi(AuthedClient(client, sessions)), sessions)
    }

    @Test
    fun anExpiredTokenIsRefreshedOnceAndTheCallRepeated() = runTest {
        val sessions = signedIn()
        val log = mutableListOf<String>()
        var patched = 0
        val repository = repo(sessions, log) { path, _ ->
            when (path) {
                "/users/me" -> if (patched++ == 0) HttpStatusCode.Unauthorized to expired
                else HttpStatusCode.OK to """{"user":$userJson}"""
                "/auth/refresh" -> HttpStatusCode.OK to
                    """{"user":$userJson,"tokens":{"accessToken":"new-access","refreshToken":"new-refresh"}}"""
                else -> HttpStatusCode.NotFound to "{}"
            }
        }

        assertEquals(AuthResult.Success, repository.updateName("Ada King"))
        assertEquals(listOf("PATCH /users/me Bearer old-access", "POST /auth/refresh null", "PATCH /users/me Bearer new-access"), log)
        assertEquals("new-refresh", sessions.refreshToken)
    }

    @Test
    fun aRejectedRefreshEndsTheSession() = runTest {
        val sessions = signedIn()
        val repository = repo(sessions, mutableListOf()) { path, _ ->
            if (path == "/auth/refresh") HttpStatusCode.Unauthorized to """{"error":{"message":"x","code":"TOKEN_REUSED"}}"""
            else HttpStatusCode.Unauthorized to expired
        }
        val result = repository.updateName("Ada King")
        assertTrue(result is AuthResult.Failure)
        assertNull(sessions.refreshToken)
    }

    @Test
    fun changePasswordStoresTheNewSessionAndSendsNoCurrentPasswordForGoogleAccounts() = runTest {
        val sessions = signedIn()
        val bodies = mutableListOf<String>()
        val repository = repo(sessions, mutableListOf()) { _, body ->
            bodies += body.orEmpty()
            HttpStatusCode.OK to """{"user":$userJson,"tokens":{"accessToken":"a2","refreshToken":"r2"}}"""
        }
        assertEquals(AuthResult.Success, repository.changePassword(null, "N3wPassword"))
        assertEquals("""{"newPassword":"N3wPassword"}""", bodies.single())
        assertEquals("a2", sessions.accessToken)
    }

    @Test
    fun deleteAndLogoutEverywhereClearTheLocalSessionOnlyOnSuccess() = runTest {
        val failing = signedIn()
        val bad = repo(failing, mutableListOf()) { _, _ ->
            HttpStatusCode.BadRequest to """{"error":{"message":"That password is incorrect.","code":"WRONG_PASSWORD"}}"""
        }
        val result = bad.deleteAccount("nope")
        assertEquals(AuthResult.Failure("That password is incorrect."), result)
        assertTrue(failing.isSignedIn)

        val ok = signedIn()
        assertEquals(AuthResult.Success, repo(ok, mutableListOf()) { _, _ -> HttpStatusCode.OK to """{"ok":true}""" }.deleteAccount("pw"))
        assertTrue(!ok.isSignedIn)

        val all = signedIn()
        assertEquals(AuthResult.Success, repo(all, mutableListOf()) { _, _ -> HttpStatusCode.OK to """{"ok":true}""" }.logoutEverywhere())
        assertTrue(!all.isSignedIn)
    }
}

package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.data.MemoryKeyValueStore
import com.fredrickemmanuel.studyzen.data.SessionStore
import com.fredrickemmanuel.studyzen.data.remote.ApiClient
import com.fredrickemmanuel.studyzen.data.remote.AuthApi
import com.fredrickemmanuel.studyzen.data.remote.NETWORK_ERROR_MESSAGE
import com.fredrickemmanuel.studyzen.feature.auth.data.AuthResult
import com.fredrickemmanuel.studyzen.feature.auth.data.CodePurpose
import com.fredrickemmanuel.studyzen.feature.auth.data.RemoteAuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemoteAuthTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val sessionJson =
        """{"user":{"id":"u1","name":"Ada","email":"ada@example.com","emailVerified":true},""" +
            """"tokens":{"accessToken":"acc","refreshToken":"ref","expiresIn":900}}"""

    private class Recorder { val requests = mutableListOf<Pair<String, String>>() }

    private fun repo(
        sessions: SessionStore = SessionStore(MemoryKeyValueStore()),
        rec: Recorder = Recorder(),
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): RemoteAuthRepository {
        val engine = MockEngine { request ->
            rec.requests += request.url.encodedPath to request.body.toByteArray().decodeToString()
            handler(request)
        }
        val api = AuthApi(ApiClient(HttpClient(engine), baseUrl = { "http://test" }))
        return RemoteAuthRepository(api, sessions)
    }

    @Test
    fun loginSavesTheSessionAndSendsTheRightRequest() = runTest {
        val sessions = SessionStore(MemoryKeyValueStore())
        val rec = Recorder()
        val repository = repo(sessions, rec) { respond(sessionJson, HttpStatusCode.OK, json) }

        assertNull(repository.currentUser)
        assertEquals(AuthResult.Success, repository.login("ada@example.com", "Sup3rSecret"))

        assertEquals("/auth/login", rec.requests.single().first)
        assertTrue(rec.requests.single().second.contains("\"email\":\"ada@example.com\""))
        assertEquals("ref", sessions.refreshToken)
        assertEquals("Ada", repository.currentUser?.name)
    }

    @Test
    fun serverMessagesAreShownAsFailures() = runTest {
        val repository = repo {
            respond(
                """{"error":{"message":"Incorrect email or password.","code":"BAD_CREDENTIALS"}}""",
                HttpStatusCode.Unauthorized, json,
            )
        }
        assertEquals(AuthResult.Failure("Incorrect email or password."), repository.login("a@b.co", "x"))
    }

    @Test
    fun unverifiedAccountsAreReportedSeparately() = runTest {
        val repository = repo {
            respond(
                """{"error":{"message":"Please verify your email. We sent you a new code.","code":"EMAIL_NOT_VERIFIED"}}""",
                HttpStatusCode.Forbidden, json,
            )
        }
        val result = repository.login("a@b.co", "x")
        assertTrue(result is AuthResult.EmailNotVerified)
    }

    @Test
    fun unreachableServerGivesAFriendlyMessage() = runTest {
        val repository = repo { throw RuntimeException("connection refused") }
        assertEquals(AuthResult.Failure(NETWORK_ERROR_MESSAGE), repository.signUp("Ada", "ada@example.com", "Sup3rSecret"))
    }

    @Test
    fun oddErrorBodiesStillGiveAMessage() = runTest {
        val repository = repo { respond("<html>Bad gateway</html>", HttpStatusCode.BadGateway) }
        val result = repository.requestPasswordReset("a@b.co")
        assertTrue(result is AuthResult.Failure && result.message.isNotBlank())
    }

    @Test
    fun resendUsesTheServerPurposeNames() = runTest {
        val rec = Recorder()
        val repository = repo(rec = rec) { respond("""{"ok":true}""", HttpStatusCode.OK, json) }
        repository.resendCode("a@b.co", CodePurpose.ResetPassword)
        repository.resendCode("a@b.co", CodePurpose.VerifyEmail)
        assertTrue(rec.requests[0].second.contains("reset_password"))
        assertTrue(rec.requests[1].second.contains("verify_email"))
    }

    @Test
    fun logoutClearsTheDeviceEvenWhenTheServerIsDown() = runTest {
        val sessions = SessionStore(MemoryKeyValueStore())
        val repository = repo(sessions) { request ->
            if (request.url.encodedPath == "/auth/login") respond(sessionJson, HttpStatusCode.OK, json)
            else throw RuntimeException("offline")
        }
        repository.login("ada@example.com", "Sup3rSecret")
        assertNotNull(sessions.refreshToken)
        repository.logout()
        assertNull(sessions.refreshToken)
        assertFalse(sessions.isSignedIn)
        assertNull(repository.currentUser)
    }
}

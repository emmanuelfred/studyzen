package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.data.MemoryKeyValueStore
import com.fredrickemmanuel.studyzen.data.ai.AiFailure
import com.fredrickemmanuel.studyzen.data.ai.AiImage
import com.fredrickemmanuel.studyzen.data.ai.AiMessage
import com.fredrickemmanuel.studyzen.data.ai.AiProvider
import com.fredrickemmanuel.studyzen.data.ai.AiProviderConfig
import com.fredrickemmanuel.studyzen.data.ai.AiResult
import com.fredrickemmanuel.studyzen.data.ai.AiRole
import com.fredrickemmanuel.studyzen.data.ai.AiService
import com.fredrickemmanuel.studyzen.data.ai.AiSettings
import com.fredrickemmanuel.studyzen.data.ai.AiSettingsStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiTest {
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val hello = listOf(AiMessage(AiRole.User, "Solve 2x+3=11", AiImage("image/jpeg", "QUJD")))

    private val geminiOk = """{"candidates":[{"content":{"parts":[{"thought":true,"text":"hmm"},{"text":"x = 4"}]}}]}"""
    private val openAiOk = """{"choices":[{"message":{"role":"assistant","content":"<think>work</think>x = 4"}}]}"""

    private class Call(val url: String, val body: String, val headers: Map<String, String>)

    private fun service(calls: MutableList<Call>, handler: (Call) -> Pair<HttpStatusCode, String>): AiService {
        val engine = MockEngine { request ->
            val call = Call(
                url = request.url.toString(),
                body = (request.body as? TextContent)?.text.orEmpty(),
                headers = mapOf(
                    "key" to (request.headers["x-goog-api-key"] ?: ""),
                    "auth" to (request.headers[HttpHeaders.Authorization] ?: ""),
                ),
            )
            calls += call
            val (status, body) = handler(call)
            respond(body, status, jsonHeaders)
        }
        return AiService(HttpClient(engine))
    }

    private val gemini = AiProviderConfig(AiProvider.Gemini, "g-key")
    private val groq = AiProviderConfig(AiProvider.Groq, "q-key", "my-model")

    @Test
    fun geminiRequestCarriesThePhotoAndTheAnswerIsRead() = runTest {
        val calls = mutableListOf<Call>()
        val result = service(calls) { HttpStatusCode.OK to geminiOk }.askOne(gemini, "sys", hello)
        assertEquals(AiResult.Success("x = 4", AiProvider.Gemini, AiProvider.Gemini.defaultModel), result)
        val call = calls.single()
        assertTrue(call.url.contains("generativelanguage.googleapis.com"))
        assertTrue(call.url.endsWith(":generateContent"))
        assertEquals("g-key", call.headers["key"])
        assertTrue(call.body.contains("inline_data"))
        assertTrue(call.body.contains("QUJD"))
        assertTrue(call.body.contains("system_instruction"))
    }

    @Test
    fun openAiStyleRequestUsesTheChosenModelAndHidesThinking() = runTest {
        val calls = mutableListOf<Call>()
        val result = service(calls) { HttpStatusCode.OK to openAiOk }.askOne(groq, "sys", hello)
        assertEquals(AiResult.Success("x = 4", AiProvider.Groq, "my-model"), result)
        val call = calls.single()
        assertTrue(call.url.contains("api.groq.com"))
        assertEquals("Bearer q-key", call.headers["auth"])
        assertTrue(call.body.contains("\"model\":\"my-model\""))
        assertTrue(call.body.contains("data:image/jpeg;base64,QUJD"))
    }

    @Test
    fun theNextAccountTakesOverWhenTheMainOneIsOutOfQuota() = runTest {
        val calls = mutableListOf<Call>()
        val svc = service(calls) { call ->
            if (call.url.contains("googleapis")) HttpStatusCode.TooManyRequests to """{"error":{"message":"quota"}}"""
            else HttpStatusCode.OK to openAiOk
        }
        val settings = AiSettings(listOf(groq, gemini), primary = AiProvider.Gemini)
        val result = svc.ask(settings, "sys", hello)
        assertTrue(result is AiResult.Success && result.provider == AiProvider.Groq)
        assertEquals(2, calls.size)
        assertTrue(calls[0].url.contains("googleapis")) // the main one is tried first
    }

    private suspend fun failure(status: HttpStatusCode, body: String): AiResult.Failure =
        service(mutableListOf()) { status to body }.askOne(groq, "s", hello) as AiResult.Failure

    @Test
    fun errorsAreExplainedInPlainWords() = runTest {
        assertEquals(AiFailure.BadKey, failure(HttpStatusCode.Unauthorized, """{"error":{"message":"bad"}}""").kind)
        assertEquals(AiFailure.BadKey, failure(HttpStatusCode.BadRequest, """{"error":{"message":"API key not valid"}}""").kind)
        assertEquals(AiFailure.RateLimited, failure(HttpStatusCode.TooManyRequests, "{}").kind)
        assertEquals(AiFailure.ModelProblem, failure(HttpStatusCode.NotFound, "{}").kind)
        assertEquals(AiFailure.ModelProblem, failure(HttpStatusCode.BadRequest, """{"error":{"message":"model does not support image input"}}""").kind)
        assertEquals(AiFailure.Server, failure(HttpStatusCode.BadGateway, "oops").kind)
    }

    @Test
    fun noKeyAndNoConnection() = runTest {
        val calls = mutableListOf<Call>()
        val ok = service(calls) { HttpStatusCode.OK to openAiOk }
        assertEquals(AiFailure.NoKey, (ok.ask(AiSettings(), "s", hello) as AiResult.Failure).kind)

        val offline = AiService(HttpClient(MockEngine { error("no network") }))
        val result = offline.ask(AiSettings(listOf(gemini, groq), AiProvider.Gemini), "s", hello)
        assertEquals(AiFailure.Offline, (result as AiResult.Failure).kind)
    }

    @Test
    fun emptyAnswerIsReported() = runTest {
        val result = service(mutableListOf()) { HttpStatusCode.OK to """{"candidates":[]}""" }.askOne(gemini, "s", hello)
        assertEquals(AiFailure.Empty, (result as AiResult.Failure).kind)
    }

    @Test
    fun keysAreStoredAndTheMainOneIsKept() {
        val kv = MemoryKeyValueStore()
        val store = AiSettingsStore(kv)
        assertTrue(!store.settings.hasKey)

        store.save(AiProvider.Groq, "  q-key ", "")
        assertEquals(AiProvider.Groq, store.settings.primary) // the first key becomes the main one
        assertEquals("q-key", store.settings.configFor(AiProvider.Groq)?.apiKey)

        store.save(AiProvider.Gemini, "g-key", "gemini-x")
        assertEquals(AiProvider.Groq, store.settings.primary)
        store.makePrimary(AiProvider.Gemini)
        assertEquals(listOf(AiProvider.Gemini, AiProvider.Groq), store.settings.ready().map { it.provider })

        // survives closing the app
        val again = AiSettingsStore(kv)
        assertEquals(AiProvider.Gemini, again.settings.primary)
        assertEquals("gemini-x", again.settings.configFor(AiProvider.Gemini)?.effectiveModel)

        again.remove(AiProvider.Gemini)
        assertEquals(AiProvider.Groq, again.settings.primary)
        again.save(AiProvider.Groq, "  ", "") // a blank key removes the account
        assertNull(again.settings.configFor(AiProvider.Groq))
        assertTrue(!again.settings.hasKey)
    }
}

package com.fredrickemmanuel.studyzen.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

fun aiHttpClient(): HttpClient = HttpClient {
    install(HttpTimeout) {
        connectTimeoutMillis = 20_000
        requestTimeoutMillis = 120_000
        socketTimeoutMillis = 120_000
    }
}

/**
 * Sends a question to the learner's own AI account (Gemini, Groq or OpenRouter) straight from
 * the phone. If the main one fails (limit reached, busy, wrong model...) it tries the others.
 */
class AiService(private val http: HttpClient = aiHttpClient()) {

    suspend fun ask(settings: AiSettings, system: String, messages: List<AiMessage>): AiResult {
        val ready = settings.ready()
        if (ready.isEmpty()) {
            return AiResult.Failure(AiFailure.NoKey, "Add a free AI key first to use this.")
        }
        val failures = mutableListOf<AiResult.Failure>()
        for (config in ready) {
            when (val result = askOne(config, system, messages)) {
                is AiResult.Success -> return result
                is AiResult.Failure -> {
                    failures += result
                    // no connection: trying the others would fail the same way
                    if (result.kind == AiFailure.Offline) return result
                }
            }
        }
        val first = failures.first()
        if (failures.size == 1) return first
        val detail = failures.joinToString("\n") { "• ${it.provider?.label ?: "AI"}: ${it.message}" }
        return AiResult.Failure(first.kind, "None of your AI accounts could answer.\n$detail", first.provider)
    }

    /** Asks one specific account. Also used by the "Test" button in AI settings. */
    suspend fun askOne(config: AiProviderConfig, system: String, messages: List<AiMessage>): AiResult {
        val provider = config.provider
        val model = config.effectiveModel
        return try {
            val response: HttpResponse = when (provider) {
                AiProvider.Gemini -> http.post(
                    "https://generativelanguage.googleapis.com/v1beta/models/" +
                        "${model.removePrefix("models/").encodeURLPathPart()}:generateContent",
                ) {
                    header("x-goog-api-key", config.apiKey.trim())
                    contentType(ContentType.Application.Json)
                    setBody(geminiBody(system, messages))
                }
                AiProvider.Groq -> http.post("https://api.groq.com/openai/v1/chat/completions") {
                    header(HttpHeaders.Authorization, "Bearer ${config.apiKey.trim()}")
                    contentType(ContentType.Application.Json)
                    setBody(openAiBody(model, system, messages))
                }
                AiProvider.OpenRouter -> http.post("https://openrouter.ai/api/v1/chat/completions") {
                    header(HttpHeaders.Authorization, "Bearer ${config.apiKey.trim()}")
                    header("X-Title", "StudyZen")
                    contentType(ContentType.Application.Json)
                    setBody(openAiBody(model, system, messages))
                }
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) return failureFor(provider, response.status.value, body)
            val text = if (provider == AiProvider.Gemini) parseGeminiAnswer(body) else parseOpenAiAnswer(body)
            if (text == null) {
                AiResult.Failure(
                    AiFailure.Empty,
                    "${provider.label} sent back no answer (it may have been blocked). Try asking in a different way.",
                    provider,
                )
            } else {
                AiResult.Success(text, provider, model)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpRequestTimeoutException) {
            AiResult.Failure(AiFailure.Server, "${provider.label} took too long to answer. Try again.", provider)
        } catch (e: Exception) {
            AiResult.Failure(
                AiFailure.Offline,
                "Can't reach ${provider.label}. Check your internet connection and try again.",
                provider,
            )
        }
    }
}

// --- Request bodies -------------------------------------------------------------------------------

internal fun geminiBody(system: String, messages: List<AiMessage>): String = buildJsonObject {
    putJsonObject("system_instruction") {
        putJsonArray("parts") { addJsonObject { put("text", system) } }
    }
    putJsonArray("contents") {
        messages.forEach { m ->
            addJsonObject {
                put("role", if (m.role == AiRole.User) "user" else "model")
                putJsonArray("parts") {
                    m.image?.let { image ->
                        addJsonObject {
                            putJsonObject("inline_data") {
                                put("mime_type", image.mimeType)
                                put("data", image.base64)
                            }
                        }
                    }
                    addJsonObject { put("text", m.text) }
                }
            }
        }
    }
    putJsonObject("generationConfig") { put("temperature", 0.4) }
}.toString()

/** Groq and OpenRouter both speak the OpenAI "chat completions" format. */
internal fun openAiBody(model: String, system: String, messages: List<AiMessage>): String = buildJsonObject {
    put("model", model)
    put("temperature", 0.4)
    putJsonArray("messages") {
        addJsonObject {
            put("role", "system")
            put("content", system)
        }
        messages.forEach { m ->
            addJsonObject {
                put("role", if (m.role == AiRole.User) "user" else "assistant")
                if (m.image == null) {
                    put("content", m.text)
                } else {
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", m.text)
                        }
                        addJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") { put("url", "data:${m.image.mimeType};base64,${m.image.base64}") }
                        }
                    }
                }
            }
        }
    }
}.toString()

// --- Reading answers and errors ---------------------------------------------------------------------

private val lenient = Json { ignoreUnknownKeys = true }

private fun parseObject(text: String): JsonObject? =
    try {
        lenient.parseToJsonElement(text) as? JsonObject
    } catch (e: Exception) {
        null
    }

private fun JsonElement?.obj(): JsonObject? = this as? JsonObject
private fun JsonElement?.arr(): JsonArray? = this as? JsonArray
private fun JsonElement?.text(): String? = (this as? JsonPrimitive)?.contentOrNull

internal fun parseGeminiAnswer(body: String): String? {
    val parts = parseObject(body)?.get("candidates").arr()?.firstOrNull().obj()
        ?.get("content").obj()?.get("parts").arr() ?: return null
    val text = parts.mapNotNull { part ->
        val o = part.obj() ?: return@mapNotNull null
        if ((o["thought"] as? JsonPrimitive)?.booleanOrNull == true) null else o["text"].text()
    }.joinToString("")
    return stripThinking(text).trim().ifEmpty { null }
}

internal fun parseOpenAiAnswer(body: String): String? {
    val message = parseObject(body)?.get("choices").arr()?.firstOrNull().obj()?.get("message").obj() ?: return null
    val content = message["content"]
    val text = when (content) {
        is JsonArray -> content.mapNotNull { it.obj()?.get("text").text() }.joinToString("")
        else -> content.text()
    } ?: return null
    return stripThinking(text).trim().ifEmpty { null }
}

/** Some models print their reasoning between <think> tags first: the learner only wants the answer. */
internal fun stripThinking(text: String): String {
    var out = text
    val close = out.lastIndexOf("</think>")
    if (close >= 0) out = out.substring(close + "</think>".length)
    return out
}

internal fun errorMessage(body: String): String? {
    val root = parseObject(body) ?: return null
    val error = root["error"]
    return (error.obj()?.get("message").text() ?: error.text() ?: root["message"].text())?.trim()?.takeIf { it.isNotEmpty() }
}

internal fun failureFor(provider: AiProvider, status: Int, body: String): AiResult.Failure {
    val name = provider.label
    val detail = errorMessage(body)
    val lower = detail?.lowercase().orEmpty()
    fun fail(kind: AiFailure, message: String) = AiResult.Failure(kind, message, provider)
    return when {
        status == 401 || status == 403 || (status == 400 && ("api key" in lower || "api_key" in lower)) ->
            fail(AiFailure.BadKey, "$name did not accept your key. Check it in AI settings.")
        status == 429 ->
            fail(AiFailure.RateLimited, "$name's free limit is reached for now. Wait a bit, or add another AI account.")
        status == 402 ->
            fail(AiFailure.RateLimited, "$name needs credits for this model. Pick a free model in AI settings.")
        status == 413 ->
            fail(AiFailure.TooLarge, "The photo is too big for $name. Try a smaller or closer photo.")
        status == 404 ->
            fail(AiFailure.ModelProblem, "$name could not find the model. Change the model in AI settings.")
        status == 400 && ("image" in lower || "vision" in lower || "multimodal" in lower || "modalit" in lower) ->
            fail(AiFailure.ModelProblem, "This $name model can't read photos. Choose a model that supports images in AI settings.")
        status in 500..599 ->
            fail(AiFailure.Server, "$name is busy right now. Try again in a moment.")
        else ->
            fail(AiFailure.Other, "$name could not answer" + (detail?.let { " ($status: ${it.take(160)})" } ?: " (error $status)") + ".")
    }
}

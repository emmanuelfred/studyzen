package com.fredrickemmanuel.studyzen.data.dictionary

import com.fredrickemmanuel.studyzen.data.remote.defaultHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val OFFLINE = "Can't reach the dictionary. Check your internet connection and try again."

@Serializable private data class ApiEntry(
    val word: String = "",
    val phonetic: String? = null,
    val phonetics: List<ApiPhonetic> = emptyList(),
    val meanings: List<ApiMeaning> = emptyList(),
)
@Serializable private data class ApiPhonetic(val text: String? = null, val audio: String? = null)
@Serializable private data class ApiMeaning(
    val partOfSpeech: String = "",
    val definitions: List<ApiDefinition> = emptyList(),
    val synonyms: List<String> = emptyList(),
)
@Serializable private data class ApiDefinition(
    val definition: String = "",
    val example: String? = null,
    val synonyms: List<String> = emptyList(),
)

/**
 * Looks words up in the free Dictionary API (dictionaryapi.dev, English, no account needed).
 * That service is often busy, so when it fails or does not know the word the lookup falls back to
 * Wiktionary's free definition service. Needs an internet connection; saved favourites and the word
 * of the day work without one.
 */
class DictionaryClient(
    private val http: HttpClient = defaultHttpClient(),
    private val baseUrl: String = "https://api.dictionaryapi.dev/api/v2/entries/en/",
    private val fallbackBaseUrl: String? = "https://en.wiktionary.org/api/rest_v1/page/definition/",
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun lookup(rawWord: String): LookupResult {
        val word = rawWord.trim().lowercase()
        if (word.isEmpty()) return LookupResult.NotFound

        val results = mutableListOf<LookupResult>()
        results += fetch(baseUrl, word) { parse(it) }
        (results.first() as? LookupResult.Found)?.let { return it }
        if (fallbackBaseUrl != null) {
            results += fetch(fallbackBaseUrl, word, withUserAgent = true) { parseWiktionary(it, word) }
        }
        results.firstOrNull { it is LookupResult.Found }?.let { return it }
        if (results.any { it is LookupResult.NotFound }) return LookupResult.NotFound
        val failures = results.filterIsInstance<LookupResult.Failed>()
        return failures.firstOrNull { !it.offline } ?: failures.first()
    }

    private suspend fun fetch(
        base: String,
        word: String,
        withUserAgent: Boolean = false,
        parser: (String) -> DictionaryEntry?,
    ): LookupResult = try {
        val response = http.get(base + word.encodeURLPathPart()) {
            if (withUserAgent) header(HttpHeaders.UserAgent, "StudyZen/1.0 (open-source study app)")
        }
        when {
            response.status.value == 404 -> LookupResult.NotFound
            !response.status.isSuccess() -> LookupResult.Failed("The dictionary isn't available right now. Try again in a moment.", offline = false)
            else -> parser(response.bodyAsText())?.let { LookupResult.Found(it) } ?: LookupResult.NotFound
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LookupResult.Failed(OFFLINE, offline = true)
    }

    /** Wiktionary answers {"en":[{"partOfSpeech":"Noun","definitions":[{"definition":"<html>","examples":["<html>"]}]}]}. */
    fun parseWiktionary(text: String, word: String = ""): DictionaryEntry? {
        val english = try {
            json.parseToJsonElement(text).jsonObject["en"]?.jsonArray
        } catch (e: Exception) {
            null
        } ?: return null

        val meanings = english.mapNotNull { element ->
            val part = (element as? JsonObject) ?: return@mapNotNull null
            val definitions = (part["definitions"] as? JsonArray).orEmpty().mapNotNull { d ->
                val obj = d as? JsonObject ?: return@mapNotNull null
                val definition = plainText(obj["definition"]).takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val example = (obj["examples"] as? JsonArray).orEmpty().firstNotNullOfOrNull { plainText(it).takeIf { t -> t.isNotBlank() } }
                WordDefinition(definition, example)
            }.take(5)
            if (definitions.isEmpty()) null
            else WordMeaning(plainText(part["partOfSpeech"]).lowercase(), definitions)
        }
        if (meanings.isEmpty()) return null
        return DictionaryEntry(word, phonetic = null, meanings = meanings, audioUrl = null)
    }

    private fun plainText(element: kotlinx.serialization.json.JsonElement?): String {
        val raw = (element as? JsonPrimitive)?.contentOrNull ?: return ""
        return raw.replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")
            .trim()
    }

    /** Public so it can be tested without a network. Returns null when there is no usable definition. */
    fun parse(text: String): DictionaryEntry? {
        val entries = try {
            json.decodeFromString(ListSerializer(ApiEntry.serializer()), text)
        } catch (e: Exception) {
            return null
        }
        if (entries.isEmpty()) return null

        val meanings = entries.flatMap { it.meanings }.mapNotNull { m ->
            val definitions = m.definitions.filter { it.definition.isNotBlank() }
                .map { WordDefinition(it.definition.trim(), it.example?.trim()?.takeIf { e -> e.isNotEmpty() }) }
            if (definitions.isEmpty()) {
                null
            } else {
                val synonyms = (m.synonyms + m.definitions.flatMap { it.synonyms }).distinct().take(8)
                WordMeaning(m.partOfSpeech.trim(), definitions, synonyms)
            }
        }
        if (meanings.isEmpty()) return null

        val phonetic = entries.firstNotNullOfOrNull { e ->
            e.phonetic?.takeIf { it.isNotBlank() } ?: e.phonetics.firstNotNullOfOrNull { it.text?.takeIf { t -> t.isNotBlank() } }
        }
        return DictionaryEntry(entries.first().word.ifBlank { "" }, phonetic, meanings, pickAudio(entries.flatMap { it.phonetics }.mapNotNull { it.audio }))
    }

    /** Prefers the American recording, and fixes links that start with "//". */
    private fun pickAudio(links: List<String>): String? {
        val usable = links.map { it.trim() }.filter { it.isNotEmpty() }
        val best = usable.firstOrNull { it.contains("-us", ignoreCase = true) } ?: usable.firstOrNull() ?: return null
        return if (best.startsWith("//")) "https:$best" else best
    }
}

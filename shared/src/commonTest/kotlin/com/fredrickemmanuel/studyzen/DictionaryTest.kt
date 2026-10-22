package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.data.StudyStore
import com.fredrickemmanuel.studyzen.data.dictionary.DictionaryClient
import com.fredrickemmanuel.studyzen.data.dictionary.LookupResult
import com.fredrickemmanuel.studyzen.data.dictionary.WordBank
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
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

class DictionaryTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private val sample = """[{"word":"hello","phonetic":"/həˈləʊ/","phonetics":[{"text":"/həˈləʊ/","audio":""}],
        "meanings":[
          {"partOfSpeech":"noun","definitions":[{"definition":"A greeting.","synonyms":[],"antonyms":[]}],"synonyms":["greeting"],"antonyms":[]},
          {"partOfSpeech":"verb","definitions":[{"definition":"To say hello.","example":"He helloed.","synonyms":["greet"],"antonyms":[]}],"synonyms":[],"antonyms":[]}],
        "license":{"name":"CC BY-SA 3.0","url":"x"},"sourceUrls":["x"]}]"""

    private fun client(handler: (String) -> Pair<HttpStatusCode, String>): DictionaryClient {
        val engine = MockEngine { request ->
            val (status, body) = handler(request.url.encodedPath)
            respond(body, status, json)
        }
        return DictionaryClient(HttpClient(engine), baseUrl = "http://test/entries/")
    }

    @Test
    fun entriesAreParsedAndMerged() = runTest {
        val result = client { HttpStatusCode.OK to sample }.lookup("hello")
        val entry = (result as LookupResult.Found).entry
        assertEquals("hello", entry.word)
        assertEquals("/həˈləʊ/", entry.phonetic)
        assertEquals(listOf("noun", "verb"), entry.meanings.map { it.partOfSpeech })
        assertEquals(listOf("greeting"), entry.meanings[0].synonyms)
        assertEquals(listOf("greet"), entry.meanings[1].synonyms)
        assertNull(entry.meanings[0].definitions.single().example)
        assertEquals("He helloed.", entry.meanings[1].definitions.single().example)

        val favorite = assertNotNull(entry.toFavorite(20_000))
        assertEquals("hello", favorite.id)
        assertEquals("A greeting.", favorite.definition)
        assertTrue(entry.toNoteText().contains("1. A greeting."))
    }

    @Test
    fun pronunciationRecordingIsPickedAndFixed() {
        val c = client { HttpStatusCode.OK to sample }
        fun entry(audio1: String, audio2: String) = c.parse(
            """[{"word":"hi","phonetics":[{"text":"/h/","audio":"$audio1"},{"audio":"$audio2"}],
            "meanings":[{"partOfSpeech":"noun","definitions":[{"definition":"x"}]}]}]""",
        )
        assertEquals("https://ex.com/hi-us.mp3", entry("", "//ex.com/hi-us.mp3")?.audioUrl)
        assertEquals("https://ex.com/hi-us.mp3", entry("https://ex.com/hi-uk.mp3", "https://ex.com/hi-us.mp3")?.audioUrl)
        assertNull(c.parse(sample)?.audioUrl)
    }

    @Test
    fun theWordIsTrimmedLowerCasedAndEncoded() = runTest {
        val seen = mutableListOf<String>()
        val c = client { path -> seen += path; HttpStatusCode.OK to sample }
        c.lookup("  Hello ")
        c.lookup("ice cream")
        assertEquals(listOf("/entries/hello", "/entries/ice%20cream"), seen)
    }

    @Test
    fun unknownWordsEmptyAnswersAndGarbageAreNotFound() = runTest {
        val notFound = """{"title":"No Definitions Found","message":"Sorry","resolution":"Try later"}"""
        assertEquals(LookupResult.NotFound, client { HttpStatusCode.NotFound to notFound }.lookup("zzzz"))
        assertEquals(LookupResult.NotFound, client { HttpStatusCode.OK to "[]" }.lookup("x"))
        assertEquals(LookupResult.NotFound, client { HttpStatusCode.OK to "not json" }.lookup("x"))
        assertEquals(LookupResult.NotFound, client { HttpStatusCode.OK to """[{"word":"x","meanings":[]}]""" }.lookup("x"))
        assertEquals(LookupResult.NotFound, client { HttpStatusCode.OK to sample }.lookup("   "))
    }

    @Test
    fun serverErrorsAndNoConnectionGiveFriendlyMessages() = runTest {
        val broken = client { HttpStatusCode.InternalServerError to "oops" }.lookup("hello")
        assertTrue(broken is LookupResult.Failed && !broken.offline)

        val engine = MockEngine { throw RuntimeException("no network") }
        val offline = DictionaryClient(HttpClient(engine), "http://test/").lookup("hello")
        assertTrue(offline is LookupResult.Failed && offline.offline)
        assertTrue((offline as LookupResult.Failed).message.contains("internet"))
    }

    @Test
    fun wiktionaryIsUsedWhenTheMainDictionaryIsDown() = runTest {
        val wiki = """{"en":[{"partOfSpeech":"Interjection","language":"English","definitions":[
            {"definition":"<a href=\"x\">A</a> greeting &amp; salutation.","examples":["<i>Hello</i>, world!"]}]}]}"""
        val engine = MockEngine { request ->
            if (request.url.host == "main") respond("oops", HttpStatusCode.ServiceUnavailable, json)
            else respond(wiki, HttpStatusCode.OK, json)
        }
        val c = DictionaryClient(HttpClient(engine), "http://main/entries/", "http://wiki/definition/")
        val entry = (c.lookup("Hello") as LookupResult.Found).entry
        assertEquals("Hello".lowercase(), entry.word)
        assertEquals("interjection", entry.meanings.single().partOfSpeech)
        assertEquals("A greeting & salutation.", entry.meanings.single().definitions.single().text)
        assertEquals("Hello, world!", entry.meanings.single().definitions.single().example)
    }

    @Test
    fun noFallbackMeansTheOriginalErrorIsReported() = runTest {
        val engine = MockEngine { respond("oops", HttpStatusCode.InternalServerError, json) }
        val c = DictionaryClient(HttpClient(engine), "http://main/", fallbackBaseUrl = null)
        assertTrue(c.lookup("hello") is LookupResult.Failed)
    }

    @Test
    fun wordOfTheDayChangesDailyAndWorksOffline() {
        val words = WordBank.words
        assertEquals(60, words.size)
        assertEquals(words.size, words.map { it.word }.toSet().size)
        assertTrue(words.all { it.definition.isNotBlank() && it.example.isNotBlank() })
        assertEquals(WordBank.wordOfTheDay(100), WordBank.wordOfTheDay(100))
        assertFalse(WordBank.wordOfTheDay(100) == WordBank.wordOfTheDay(101))
        assertEquals(WordBank.wordOfTheDay(5), WordBank.wordOfTheDay(5 + words.size))
        assertNotNull(WordBank.wordOfTheDay(-3))
    }

    @Test
    fun favouritesAreSavedOnceAndRemoved() {
        val store = StudyStore().also { it.clearAll() }
        val word = WordBank.words.first().toFavorite(20_000)
        store.addFavoriteWord(word)
        store.addFavoriteWord(word.copy(definition = "Updated"))
        assertEquals(1, store.favoriteWords.size)
        assertEquals("Updated", store.favoriteWords.single().definition)
        assertTrue(store.isFavoriteWord("  " + word.word.uppercase()))
        store.removeFavoriteWord(word.id)
        assertTrue(store.favoriteWords.isEmpty())
    }
}

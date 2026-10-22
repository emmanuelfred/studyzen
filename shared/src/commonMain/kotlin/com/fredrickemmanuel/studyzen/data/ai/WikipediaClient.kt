package com.fredrickemmanuel.studyzen.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/**
 * Searches Wikipedia (English) for real articles. No account or key needed, and it works as long as
 * the phone is online. Used for lesson sources and the research tool.
 */
class WikipediaClient(
    private val http: HttpClient = aiHttpClient(),
    private val baseUrl: String = "https://en.wikipedia.org/w/api.php",
) {
    /** Null when Wikipedia could not be reached; an empty list when nothing matched. */
    suspend fun search(query: String, limit: Int = 3): List<WikiArticle>? {
        val clean = query.trim()
        if (clean.isEmpty()) return emptyList()
        return try {
            val response = http.get(
                "$baseUrl?action=query&format=json&generator=search&gsrsearch=${urlEncode(clean)}" +
                    "&gsrlimit=$limit&prop=extracts%7Cinfo&exintro=1&explaintext=1&exlimit=max&inprop=url&redirects=1",
            ) {
                header(HttpHeaders.UserAgent, "StudyZen/1.0 (open-source study app)")
            }
            if (!response.status.isSuccess()) null else parseWikipediaSearch(response.bodyAsText())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}

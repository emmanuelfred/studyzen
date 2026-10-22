package com.fredrickemmanuel.studyzen.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

const val NETWORK_ERROR_MESSAGE = "Can't reach StudyZen. Check your internet connection and try again."
private const val GENERIC_ERROR_MESSAGE = "Something went wrong. Please try again."

/**
 * A failed API call. [message] is safe to show to the learner. [status] is null when the server
 * could not be reached at all.
 */
class ApiException(val status: Int?, val code: String?, message: String) : Exception(message) {
    val isNetworkError: Boolean get() = status == null
}

fun defaultHttpClient(): HttpClient = HttpClient {
    install(HttpTimeout) { requestTimeoutMillis = 20_000 }
}

/** Sends JSON to the StudyZen API and turns every failure into an [ApiException]. */
class ApiClient(
    private val http: HttpClient = defaultHttpClient(),
    private val baseUrl: () -> String = { ApiConfig.baseUrl },
) {
    suspend fun post(path: String, jsonBody: String, bearer: String? = null): String = send {
        http.post(baseUrl() + path) {
            contentType(ContentType.Application.Json)
            if (bearer != null) header(HttpHeaders.Authorization, "Bearer $bearer")
            setBody(jsonBody)
        }
    }

    suspend fun patch(path: String, jsonBody: String, bearer: String? = null): String = send {
        http.patch(baseUrl() + path) {
            contentType(ContentType.Application.Json)
            if (bearer != null) header(HttpHeaders.Authorization, "Bearer $bearer")
            setBody(jsonBody)
        }
    }

    suspend fun get(path: String, bearer: String? = null): String = send {
        http.get(baseUrl() + path) {
            if (bearer != null) header(HttpHeaders.Authorization, "Bearer $bearer")
        }
    }

    private suspend fun send(request: suspend () -> HttpResponse): String {
        val response = try {
            request()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Shown only in the console/logcat, never to the learner: tells you WHY it could not connect.
            println("StudyZen network error calling ${baseUrl()}: ${e::class.simpleName}: ${e.message} / cause: ${e.cause}")
            throw ApiException(null, "NETWORK", NETWORK_ERROR_MESSAGE)
        }
        val text = try {
            response.bodyAsText()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ""
        }
        if (response.status.isSuccess()) return text

        val detail = runCatching { ApiJson.decodeFromString(ApiErrorBody.serializer(), text).error }.getOrNull()
        throw ApiException(
            status = response.status.value,
            code = detail?.code,
            message = detail?.message?.takeIf { it.isNotBlank() } ?: GENERIC_ERROR_MESSAGE,
        )
    }
}

package com.fredrickemmanuel.studyzen

import com.fredrickemmanuel.studyzen.feature.auth.google.GoogleSignInClient
import com.fredrickemmanuel.studyzen.feature.auth.google.GoogleSignInResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlin.coroutines.coroutineContext

/**
 * "Continue with Google" on desktop, using Google's installed-app flow:
 * the system browser opens Google's sign-in page, and Google sends the answer back to a
 * one-time address on this computer (http://127.0.0.1:<port>). The code is exchanged for a Google
 * ID token that the StudyZen server checks (POST /auth/google), exactly like on Android.
 *
 * [clientId] and [clientSecret] come from a Google Cloud OAuth client of type **Desktop app**.
 * Google documents that a desktop client's "secret" is not confidential. The client ID must also
 * be listed in the server's GOOGLE_CLIENT_IDS.
 */
class DesktopGoogleSignInClient(
    private val clientId: String,
    private val clientSecret: String,
) : GoogleSignInClient {

    override suspend fun signIn(): GoogleSignInResult {
        if (clientId.isBlank()) {
            return GoogleSignInResult.Error("Google sign-in is not set up in this build. Use e-mail sign-in or Try without an account.")
        }
        return try {
            withContext(Dispatchers.IO) { performSignIn() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("StudyZen desktop Google sign-in: ${e::class.simpleName}: ${e.message}")
            GoogleSignInResult.Error("Google sign-in failed. Please try again.")
        }
    }

    private suspend fun performSignIn(): GoogleSignInResult {
        val random = SecureRandom()
        val verifier = randomUrlSafe(random, 48)
        val challenge = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        val state = randomUrlSafe(random, 16)

        ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
            server.soTimeout = 1000
            val redirect = "http://127.0.0.1:${server.localPort}"
            val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" + form(
                "client_id" to clientId,
                "redirect_uri" to redirect,
                "response_type" to "code",
                "scope" to "openid email profile",
                "code_challenge" to challenge,
                "code_challenge_method" to "S256",
                "state" to state,
                "prompt" to "select_account",
            )
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                return GoogleSignInResult.Error("Could not open your web browser for Google sign-in.")
            }
            Desktop.getDesktop().browse(URI(authUrl))

            val deadline = System.currentTimeMillis() + 3 * 60_000
            while (coroutineContext.isActive && System.currentTimeMillis() < deadline) {
                val socket = try {
                    server.accept()
                } catch (e: SocketTimeoutException) {
                    continue
                }
                socket.use { s ->
                    val requestLine = s.getInputStream().bufferedReader().readLine().orEmpty()
                    val query = requestLine.substringAfter(' ').substringBefore(' ').substringAfter('?', "")
                    val params = query.split('&').filter { it.contains('=') }.associate {
                        URLDecoder.decode(it.substringBefore('='), "UTF-8") to URLDecoder.decode(it.substringAfter('='), "UTF-8")
                    }
                    val isAnswer = params.containsKey("code") || params.containsKey("error")
                    val ok = params["state"] == state && params.containsKey("code")
                    respond(s, if (!isAnswer) "" else if (ok) "You are signed in. You can close this tab and go back to StudyZen." else "Sign-in was cancelled. You can close this tab.")
                    if (!isAnswer) return@use
                    if (params["state"] != state) return GoogleSignInResult.Error("Google sign-in could not be verified. Please try again.")
                    if (params.containsKey("error")) return GoogleSignInResult.Cancelled
                    return exchange(params.getValue("code"), verifier, redirect)
                }
            }
            return GoogleSignInResult.Cancelled
        }
    }

    private fun exchange(code: String, verifier: String, redirect: String): GoogleSignInResult {
        val body = form(
            "code" to code,
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "code_verifier" to verifier,
            "redirect_uri" to redirect,
            "grant_type" to "authorization_code",
        )
        val connection = (URI("https://oauth2.googleapis.com/token").toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        connection.outputStream.use { it.write(body.toByteArray()) }
        val text = (if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()?.readText().orEmpty()
        val idToken = Regex("\"id_token\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.get(1)
        return if (idToken != null) {
            GoogleSignInResult.Success(idToken)
        } else {
            println("StudyZen desktop Google sign-in: token exchange failed: $text")
            GoogleSignInResult.Error("Google sign-in failed. Please try again.")
        }
    }

    private fun respond(socket: java.net.Socket, message: String) {
        val html = "<html><body style=\"font-family:sans-serif;text-align:center;margin-top:20vh\"><h2>StudyZen</h2><p>$message</p></body></html>"
        val bytes = html.toByteArray()
        val out = socket.getOutputStream()
        out.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
        out.write(bytes)
        out.flush()
    }

    private fun form(vararg pairs: Pair<String, String>) =
        pairs.joinToString("&") { (k, v) -> "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }

    private fun randomUrlSafe(random: SecureRandom, bytes: Int): String {
        val b = ByteArray(bytes).also { random.nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b)
    }
}

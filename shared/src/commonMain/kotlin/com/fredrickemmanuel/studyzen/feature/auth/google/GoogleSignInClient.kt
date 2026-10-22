package com.fredrickemmanuel.studyzen.feature.auth.google

import kotlinx.coroutines.delay

sealed interface GoogleSignInResult {
    /** [idToken] is the Google ID token (a JWT). The backend verifies it and creates the session. */
    data class Success(val idToken: String) : GoogleSignInResult

    /** The user closed the Google account picker. Not an error, nothing is shown. */
    data object Cancelled : GoogleSignInResult

    data class Error(val message: String) : GoogleSignInResult
}

/**
 * The single place where real Google sign-in plugs in.
 *
 * Each platform implements this once (Android: Credential Manager, iOS: GoogleSignIn SDK,
 * web: Google Identity Services, desktop: browser OAuth) and passes it to `App(...)`.
 * The screens never know which platform they run on.
 *
 * Implementations must not throw for normal failures: return [GoogleSignInResult.Cancelled]
 * or [GoogleSignInResult.Error] instead.
 */
fun interface GoogleSignInClient {
    suspend fun signIn(): GoogleSignInResult
}

/**
 * Used until a real client is provided, so the whole flow can be tried on any platform.
 * It pretends the user picked an account and returns a made-up token.
 * DO NOT ship with this: pass your real client to `App(googleSignInClient = ...)`.
 */
class FakeGoogleSignInClient : GoogleSignInClient {
    override suspend fun signIn(): GoogleSignInResult {
        delay(700)
        return GoogleSignInResult.Success(idToken = "fake-google-id-token")
    }
}

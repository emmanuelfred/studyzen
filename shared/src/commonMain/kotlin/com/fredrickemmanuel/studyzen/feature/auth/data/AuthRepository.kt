package com.fredrickemmanuel.studyzen.feature.auth.data

import kotlinx.coroutines.delay

/** Why a one-time code was sent. */
enum class CodePurpose { VerifyEmail, ResetPassword }

sealed interface AuthResult {
    data object Success : AuthResult

    /** The user backed out (e.g. closed the Google picker). Nothing is shown. */
    data object Cancelled : AuthResult
    data class Failure(val message: String) : AuthResult

    /** Password was right but the e-mail is not verified yet; the server sent a new code. */
    data class EmailNotVerified(val message: String) : AuthResult
}

/** The signed-in learner, as the server knows them. */
data class SessionUser(val id: String, val name: String, val email: String)

/**
 * Everything the auth screens need from the server. The UI only talks to this interface, so
 * when the Node backend is ready we add a real implementation (HTTP client) and swap it in
 * `AppNavHost` without touching any screen.
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): AuthResult
    suspend fun signUp(name: String, email: String, password: String): AuthResult

    /**
     * Sends the Google ID token to the backend (planned: `POST /auth/google { idToken }`).
     * The backend verifies it with Google and logs in or creates the account.
     */
    suspend fun loginWithGoogle(idToken: String): AuthResult
    suspend fun verifyEmail(email: String, code: String): AuthResult
    suspend fun resendCode(email: String, purpose: CodePurpose): AuthResult
    suspend fun requestPasswordReset(email: String): AuthResult
    suspend fun verifyResetCode(email: String, code: String): AuthResult
    suspend fun resetPassword(email: String, code: String, newPassword: String): AuthResult

    /** The learner kept signed in on this device, or null. */
    val currentUser: SessionUser? get() = null

    /** Ends the session on this device (and on the server when it can be reached). */
    suspend fun logout() {}
}

/**
 * Stand-in used until the backend exists. Every call takes ~0.9s and succeeds, except these
 * triggers so the error states can be seen:
 *  - login with password `wrongpass1`            -> "Incorrect email or password."
 *  - sign up with email `taken@studyzen.app`     -> "An account with this email already exists."
 *  - entering the code `000000`                  -> "That code is incorrect or has expired."
 */
class FakeAuthRepository : AuthRepository {

    private suspend fun fakeNetwork() = delay(900)

    override suspend fun login(email: String, password: String): AuthResult {
        fakeNetwork()
        return if (password == "wrongpass1") {
            AuthResult.Failure("Incorrect email or password.")
        } else {
            AuthResult.Success
        }
    }

    override suspend fun signUp(name: String, email: String, password: String): AuthResult {
        fakeNetwork()
        return if (email.equals("taken@studyzen.app", ignoreCase = true)) {
            AuthResult.Failure("An account with this email already exists.")
        } else {
            AuthResult.Success
        }
    }

    override suspend fun loginWithGoogle(idToken: String): AuthResult {
        fakeNetwork()
        return AuthResult.Success
    }

    override suspend fun verifyEmail(email: String, code: String): AuthResult {
        fakeNetwork()
        return checkCode(code)
    }

    override suspend fun resendCode(email: String, purpose: CodePurpose): AuthResult {
        fakeNetwork()
        return AuthResult.Success
    }

    override suspend fun requestPasswordReset(email: String): AuthResult {
        fakeNetwork()
        return AuthResult.Success
    }

    override suspend fun verifyResetCode(email: String, code: String): AuthResult {
        fakeNetwork()
        return checkCode(code)
    }

    override suspend fun resetPassword(email: String, code: String, newPassword: String): AuthResult {
        fakeNetwork()
        return AuthResult.Success
    }

    private fun checkCode(code: String): AuthResult =
        if (code == "000000") {
            AuthResult.Failure("That code is incorrect or has expired.")
        } else {
            AuthResult.Success
        }
}

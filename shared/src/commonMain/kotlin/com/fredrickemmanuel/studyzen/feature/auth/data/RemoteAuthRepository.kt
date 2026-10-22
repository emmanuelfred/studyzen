package com.fredrickemmanuel.studyzen.feature.auth.data

import com.fredrickemmanuel.studyzen.data.SessionStore
import com.fredrickemmanuel.studyzen.data.remote.ApiException
import com.fredrickemmanuel.studyzen.data.remote.AuthApi
import com.fredrickemmanuel.studyzen.data.remote.SessionResponse
import kotlinx.coroutines.CancellationException

private const val UNEXPECTED = "Something went wrong. Please try again."

/** Talks to the Node server. Replaces [FakeAuthRepository] in the running app. */
class RemoteAuthRepository(
    private val api: AuthApi = AuthApi(),
    private val sessions: SessionStore = SessionStore(),
) : AuthRepository {

    override val currentUser: SessionUser?
        get() = if (sessions.isSignedIn) sessions.user?.let { SessionUser(it.id, it.name, it.email) } else null

    override suspend fun login(email: String, password: String) =
        guarded { startSession(api.login(email, password)) }

    override suspend fun signUp(name: String, email: String, password: String) =
        guarded { api.signUp(name, email, password) }

    override suspend fun loginWithGoogle(idToken: String) =
        guarded { startSession(api.loginWithGoogle(idToken)) }

    override suspend fun verifyEmail(email: String, code: String) =
        guarded { startSession(api.verifyEmail(email, code)) }

    override suspend fun resendCode(email: String, purpose: CodePurpose) =
        guarded {
            api.resendCode(
                email,
                when (purpose) {
                    CodePurpose.VerifyEmail -> "verify_email"
                    CodePurpose.ResetPassword -> "reset_password"
                },
            )
        }

    override suspend fun requestPasswordReset(email: String) = guarded { api.forgotPassword(email) }

    override suspend fun verifyResetCode(email: String, code: String) =
        guarded { api.verifyResetCode(email, code) }

    override suspend fun resetPassword(email: String, code: String, newPassword: String) =
        guarded { api.resetPassword(email, code, newPassword) }

    override suspend fun logout() {
        val token = sessions.refreshToken
        sessions.clear() // the learner is signed out here even if the server cannot be reached
        if (token != null) {
            try {
                api.logout(token)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // best effort: the token also expires on its own
            }
        }
    }

    private fun startSession(session: SessionResponse) {
        sessions.save(session)
    }

    private suspend fun guarded(block: suspend () -> Unit): AuthResult =
        try {
            block()
            AuthResult.Success
        } catch (e: ApiException) {
            if (e.code == "EMAIL_NOT_VERIFIED") AuthResult.EmailNotVerified(e.message.orEmpty()) else AuthResult.Failure(e.message.orEmpty())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Failure(UNEXPECTED)
        }
}

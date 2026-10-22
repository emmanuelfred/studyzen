package com.fredrickemmanuel.studyzen.data.remote

import kotlinx.serialization.serializer

/** One function per `/auth` endpoint of the Node server. */
class AuthApi(private val client: ApiClient = ApiClient()) {

    private suspend inline fun <reified B, reified R> call(path: String, body: B): R {
        val text = client.post(path, ApiJson.encodeToString(serializer<B>(), body))
        return ApiJson.decodeFromString(serializer<R>(), text)
    }

    suspend fun signUp(name: String, email: String, password: String) {
        call<SignUpBody, OkResponse>("/auth/signup", SignUpBody(name, email, password))
    }

    suspend fun verifyEmail(email: String, code: String): SessionResponse =
        call("/auth/verify-email", CodeBody(email, code))

    suspend fun resendCode(email: String, purpose: String) {
        call<ResendBody, OkResponse>("/auth/resend-code", ResendBody(email, purpose))
    }

    suspend fun login(email: String, password: String): SessionResponse =
        call("/auth/login", LoginBody(email, password))

    suspend fun loginWithGoogle(idToken: String): SessionResponse =
        call("/auth/google", GoogleBody(idToken))

    suspend fun forgotPassword(email: String) {
        call<EmailBody, OkResponse>("/auth/forgot-password", EmailBody(email))
    }

    suspend fun verifyResetCode(email: String, code: String) {
        call<CodeBody, OkResponse>("/auth/verify-reset-code", CodeBody(email, code))
    }

    suspend fun resetPassword(email: String, code: String, newPassword: String) {
        call<ResetBody, OkResponse>("/auth/reset-password", ResetBody(email, code, newPassword))
    }

    suspend fun refresh(refreshToken: String): SessionResponse =
        call("/auth/refresh", RefreshBody(refreshToken))

    suspend fun logout(refreshToken: String) {
        call<RefreshBody, OkResponse>("/auth/logout", RefreshBody(refreshToken))
    }
}

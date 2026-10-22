package com.fredrickemmanuel.studyzen.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

// --- Requests ------------------------------------------------------------------------------

@Serializable internal data class SignUpBody(val name: String, val email: String, val password: String)
@Serializable internal data class LoginBody(val email: String, val password: String)
@Serializable internal data class GoogleBody(val idToken: String)
@Serializable internal data class CodeBody(val email: String, val code: String)
@Serializable internal data class ResendBody(val email: String, val purpose: String)
@Serializable internal data class EmailBody(val email: String)
@Serializable internal data class ResetBody(val email: String, val code: String, val newPassword: String)
@Serializable internal data class RefreshBody(val refreshToken: String)
@Serializable internal data class NameBody(val name: String)
@Serializable internal data class ChangePasswordBody(val currentPassword: String? = null, val newPassword: String)
@Serializable internal data class DeleteAccountBody(val password: String? = null, val confirm: String)
@Serializable internal data class EmptyBody(val unused: Boolean = true)

// --- Responses -----------------------------------------------------------------------------

@Serializable internal data class OkResponse(val ok: Boolean = true)
@Serializable internal data class UserResponse(val user: ApiUser)

@Serializable
data class ApiUser(
    val id: String,
    val name: String,
    val email: String,
    val emailVerified: Boolean = false,
    /** False for accounts that only use Google sign-in. */
    val hasPassword: Boolean = true,
)

@Serializable
data class ApiTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long = 900,
)

@Serializable
data class SessionResponse(val user: ApiUser, val tokens: ApiTokens)

@Serializable internal data class ApiErrorBody(val error: ApiErrorDetail? = null)
@Serializable internal data class ApiErrorDetail(val message: String = "", val code: String? = null)

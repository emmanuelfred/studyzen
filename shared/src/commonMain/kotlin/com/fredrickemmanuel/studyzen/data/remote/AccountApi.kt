package com.fredrickemmanuel.studyzen.data.remote

import kotlinx.serialization.serializer

/** One function per account endpoint of the Node server (`/users/me`, `/auth/logout-all`). */
class AccountApi(private val client: AuthedClient = AuthedClient()) {

    suspend fun me(): ApiUser =
        ApiJson.decodeFromString(serializer<UserResponse>(), client.get("/users/me")).user

    suspend fun updateName(name: String): ApiUser {
        val body = ApiJson.encodeToString(serializer<NameBody>(), NameBody(name))
        return ApiJson.decodeFromString(serializer<UserResponse>(), client.patch("/users/me", body)).user
    }

    /** Returns the fresh session: the server ends every other login and keeps this device signed in. */
    suspend fun changePassword(currentPassword: String?, newPassword: String): SessionResponse {
        val body = ApiJson.encodeToString(serializer<ChangePasswordBody>(), ChangePasswordBody(currentPassword, newPassword))
        return ApiJson.decodeFromString(serializer<SessionResponse>(), client.post("/users/me/change-password", body))
    }

    suspend fun logoutEverywhere() {
        client.post("/auth/logout-all", ApiJson.encodeToString(serializer<EmptyBody>(), EmptyBody()))
    }

    suspend fun deleteAccount(password: String?) {
        val body = ApiJson.encodeToString(serializer<DeleteAccountBody>(), DeleteAccountBody(password, "DELETE"))
        client.post("/users/me/delete", body)
    }
}

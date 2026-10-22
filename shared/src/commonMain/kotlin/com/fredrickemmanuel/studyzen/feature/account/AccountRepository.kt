package com.fredrickemmanuel.studyzen.feature.account

import androidx.compose.runtime.staticCompositionLocalOf
import com.fredrickemmanuel.studyzen.data.SessionStore
import com.fredrickemmanuel.studyzen.data.remote.AccountApi
import com.fredrickemmanuel.studyzen.data.remote.ApiException
import com.fredrickemmanuel.studyzen.feature.auth.data.AuthResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val UNEXPECTED = "Something went wrong. Please try again."

data class AccountInfo(val name: String, val email: String, val hasPassword: Boolean)

/** Everything the Edit profile and Security screens need from the account. */
interface AccountRepository {
    /** The account as the server knows it, or null when it cannot be loaded right now. */
    suspend fun loadAccount(): AccountInfo?
    suspend fun updateName(name: String): AuthResult
    /** [currentPassword] is null for accounts that signed up with Google and have no password yet. */
    suspend fun changePassword(currentPassword: String?, newPassword: String): AuthResult
    /** Ends every login of this account, including this device. */
    suspend fun logoutEverywhere(): AuthResult
    /** Permanently deletes the account. [password] is null for Google-only accounts. */
    suspend fun deleteAccount(password: String?): AuthResult
}

val LocalAccountRepository = staticCompositionLocalOf<AccountRepository> { FakeAccountRepository() }

/** Works offline with an in-memory account, for previews and for running without the server. */
class FakeAccountRepository(
    private var name: String = "Demo learner",
    private val email: String = "demo@studyzen.app",
    private var password: String? = "Password1",
) : AccountRepository {

    override suspend fun loadAccount() = AccountInfo(name, email, password != null)

    override suspend fun updateName(name: String): AuthResult {
        delay(500)
        this.name = name.trim()
        return AuthResult.Success
    }

    override suspend fun changePassword(currentPassword: String?, newPassword: String): AuthResult {
        delay(700)
        val saved = password
        if (saved != null && currentPassword != saved) return AuthResult.Failure("That password is incorrect.")
        password = newPassword
        return AuthResult.Success
    }

    override suspend fun logoutEverywhere(): AuthResult {
        delay(500)
        return AuthResult.Success
    }

    override suspend fun deleteAccount(password: String?): AuthResult {
        delay(700)
        val saved = this.password
        if (saved != null && password != saved) return AuthResult.Failure("That password is incorrect.")
        return AuthResult.Success
    }
}

class RemoteAccountRepository(
    private val api: AccountApi = AccountApi(),
    private val sessions: SessionStore = SessionStore(),
) : AccountRepository {

    override suspend fun loadAccount(): AccountInfo? =
        try {
            val user = api.me()
            sessions.updateUser(name = user.name, hasPassword = user.hasPassword)
            AccountInfo(user.name, user.email, user.hasPassword)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            sessions.user?.let { AccountInfo(it.name, it.email, it.hasPassword) }
        }

    override suspend fun updateName(name: String) = guarded {
        val user = api.updateName(name.trim())
        sessions.updateUser(name = user.name)
    }

    override suspend fun changePassword(currentPassword: String?, newPassword: String) = guarded {
        sessions.save(api.changePassword(currentPassword, newPassword))
    }

    override suspend fun logoutEverywhere() = guarded {
        api.logoutEverywhere()
        sessions.clear()
    }

    override suspend fun deleteAccount(password: String?) = guarded {
        api.deleteAccount(password)
        sessions.clear()
    }

    private suspend fun guarded(block: suspend () -> Unit): AuthResult =
        try {
            block()
            AuthResult.Success
        } catch (e: ApiException) {
            AuthResult.Failure(e.message.orEmpty())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Failure(UNEXPECTED)
        }
}

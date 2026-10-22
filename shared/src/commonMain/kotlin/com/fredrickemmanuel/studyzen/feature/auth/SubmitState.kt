package com.fredrickemmanuel.studyzen.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.fredrickemmanuel.studyzen.feature.auth.data.AuthResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Tracks one form submission: whether it is in flight and the server's error message.
 * Ignores a second submit while one is running.
 */
@Stable
class SubmitState(private val scope: CoroutineScope) {
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun clearError() {
        error = null
    }

    fun submit(
        action: suspend () -> AuthResult,
        onSuccess: () -> Unit,
        onEmailNotVerified: (() -> Unit)? = null,
    ) {
        if (isLoading) return
        error = null
        isLoading = true
        scope.launch {
            val result = action()
            isLoading = false
            when (result) {
                is AuthResult.Success -> onSuccess()
                is AuthResult.Failure -> error = result.message
                is AuthResult.EmailNotVerified ->
                    if (onEmailNotVerified != null) onEmailNotVerified() else error = result.message
                is AuthResult.Cancelled -> Unit
            }
        }
    }
}

@Composable
fun rememberSubmitState(): SubmitState {
    val scope = rememberCoroutineScope()
    return remember(scope) { SubmitState(scope) }
}

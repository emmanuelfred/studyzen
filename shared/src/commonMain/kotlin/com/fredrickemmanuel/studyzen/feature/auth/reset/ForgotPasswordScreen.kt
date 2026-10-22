package com.fredrickemmanuel.studyzen.feature.auth.reset

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.LockIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.feature.auth.AuthValidation
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthBadge
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthErrorBanner
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthFooterLink
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthScaffold
import com.fredrickemmanuel.studyzen.feature.auth.data.AuthRepository
import com.fredrickemmanuel.studyzen.feature.auth.rememberSubmitState

/** First step of password recovery: ask for the account email and send a reset code. */
@Composable
fun ForgotPasswordScreen(
    repository: AuthRepository,
    onBack: () -> Unit,
    onCodeSent: (email: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val submit = rememberSubmitState()

    val emailError = if (attempted) AuthValidation.emailError(email) else null

    fun trySubmit() {
        attempted = true
        if (AuthValidation.emailError(email) == null) {
            val cleanEmail = email.trim()
            submit.submit(
                action = { repository.requestPasswordReset(cleanEmail) },
                onSuccess = { onCodeSent(cleanEmail) },
            )
        }
    }

    AuthScaffold(
        title = "Forgot password?",
        subtitle = "Enter your email and we'll send you a code to reset your password.",
        modifier = modifier,
        onBack = onBack,
        header = {
            AuthBadge {
                LockIcon(size = 32.dp, tint = MaterialTheme.colorScheme.primary)
            }
        },
    ) {
        StudyZenTextField(
            value = email,
            onValueChange = { email = it; submit.clearError() },
            label = "Email",
            placeholder = "you@example.com",
            error = emailError,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done,
            enabled = !submit.isLoading,
            onImeAction = ::trySubmit,
        )

        Spacer(Modifier.height(24.dp))
        submit.error?.let {
            AuthErrorBanner(message = it)
            Spacer(Modifier.height(12.dp))
        }
        StudyZenButton(
            text = "Send code",
            onClick = ::trySubmit,
            loading = submit.isLoading,
        )

        Spacer(Modifier.height(16.dp))
        AuthFooterLink(
            prompt = "Remembered it?",
            action = "Back to log in",
            onClick = onBack,
        )
    }
}

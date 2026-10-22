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
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.CheckIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.LockIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.feature.auth.AuthValidation
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthBadge
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthErrorBanner
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthScaffold
import com.fredrickemmanuel.studyzen.feature.auth.components.PasswordRequirements
import com.fredrickemmanuel.studyzen.feature.auth.data.AuthRepository
import com.fredrickemmanuel.studyzen.feature.auth.rememberSubmitState

/**
 * Last step of password recovery: choose a new password. After it succeeds the screen switches
 * to a confirmation and [onDone] takes the user back to log in.
 */
@Composable
fun ResetPasswordScreen(
    email: String,
    code: String,
    repository: AuthRepository,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var completed by rememberSaveable { mutableStateOf(false) }
    val submit = rememberSubmitState()

    if (completed) {
        AuthScaffold(
            title = "Password updated",
            subtitle = "Your password has been changed. You can now log in with your new password.",
            modifier = modifier,
            header = {
                AuthBadge {
                    CheckIcon(size = 32.dp, tint = MaterialTheme.colorScheme.primary)
                }
            },
        ) {
            StudyZenButton(text = "Back to log in", onClick = onDone)
        }
        return
    }

    val passwordError = if (attempted) AuthValidation.newPasswordError(password) else null
    val confirmError = if (attempted) AuthValidation.confirmPasswordError(password, confirmation) else null

    fun trySubmit() {
        attempted = true
        val valid = AuthValidation.newPasswordError(password) == null &&
            AuthValidation.confirmPasswordError(password, confirmation) == null
        if (valid) {
            submit.submit(
                action = { repository.resetPassword(email, code, password) },
                onSuccess = { completed = true },
            )
        }
    }

    AuthScaffold(
        title = "Create new password",
        subtitle = "Choose a strong password you haven't used before.",
        modifier = modifier,
        onBack = onBack,
        header = {
            AuthBadge {
                LockIcon(size = 32.dp, tint = MaterialTheme.colorScheme.primary)
            }
        },
    ) {
        StudyZenTextField(
            value = password,
            onValueChange = { password = it; submit.clearError() },
            label = "New password",
            placeholder = "Create a password",
            error = passwordError,
            isPassword = true,
            enabled = !submit.isLoading,
        )
        Spacer(Modifier.height(10.dp))
        PasswordRequirements(password = password)
        Spacer(Modifier.height(16.dp))
        StudyZenTextField(
            value = confirmation,
            onValueChange = { confirmation = it; submit.clearError() },
            label = "Confirm new password",
            placeholder = "Repeat your password",
            error = confirmError,
            isPassword = true,
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
            text = "Reset password",
            onClick = ::trySubmit,
            loading = submit.isLoading,
        )
    }
}

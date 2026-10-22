package com.fredrickemmanuel.studyzen.feature.auth.verify

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.MailIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.OtpField
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.feature.auth.AuthValidation
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthBadge
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthErrorBanner
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthScaffold
import com.fredrickemmanuel.studyzen.feature.auth.data.AuthRepository
import com.fredrickemmanuel.studyzen.feature.auth.data.CodePurpose
import com.fredrickemmanuel.studyzen.feature.auth.rememberSubmitState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ResendCooldownSeconds = 30

/**
 * Enter the 6-digit code that was emailed. Used both to verify a new account's email and to
 * confirm a password reset ([purpose]). [onVerified] receives the accepted code.
 */
@Composable
fun VerifyCodeScreen(
    email: String,
    purpose: CodePurpose,
    repository: AuthRepository,
    onBack: () -> Unit,
    onVerified: (code: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val submit = rememberSubmitState()
    val scope = rememberCoroutineScope()

    // Resend cooldown: restarts every time a new code is requested.
    var resendCount by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(ResendCooldownSeconds) }
    var resendInFlight by remember { mutableStateOf(false) }
    var resendNotice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(resendCount) {
        secondsLeft = ResendCooldownSeconds
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    val codeError = if (attempted) AuthValidation.codeError(code) else null

    fun trySubmit() {
        attempted = true
        if (AuthValidation.codeError(code) == null) {
            val entered = code
            submit.submit(
                action = {
                    when (purpose) {
                        CodePurpose.VerifyEmail -> repository.verifyEmail(email, entered)
                        CodePurpose.ResetPassword -> repository.verifyResetCode(email, entered)
                    }
                },
                onSuccess = { onVerified(entered) },
            )
        }
    }

    fun resend() {
        if (secondsLeft > 0 || resendInFlight) return
        resendInFlight = true
        resendNotice = null
        scope.launch {
            repository.resendCode(email, purpose)
            resendInFlight = false
            resendNotice = "We sent you a new code."
            code = ""
            attempted = false
            resendCount += 1
        }
    }

    AuthScaffold(
        title = when (purpose) {
            CodePurpose.VerifyEmail -> "Verify your email"
            CodePurpose.ResetPassword -> "Check your email"
        },
        subtitle = "Enter the 6-digit code we sent to $email.",
        modifier = modifier,
        onBack = onBack,
        header = {
            AuthBadge {
                MailIcon(size = 32.dp, tint = MaterialTheme.colorScheme.primary)
            }
        },
    ) {
        OtpField(
            value = code,
            onValueChange = {
                code = it
                submit.clearError()
                resendNotice = null
            },
            isError = codeError != null || submit.error != null,
            enabled = !submit.isLoading,
        )
        if (codeError != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = codeError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(24.dp))
        submit.error?.let {
            AuthErrorBanner(message = it)
            Spacer(Modifier.height(12.dp))
        }
        resendNotice?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }
        StudyZenButton(
            text = if (purpose == CodePurpose.VerifyEmail) "Verify email" else "Continue",
            onClick = ::trySubmit,
            loading = submit.isLoading,
        )

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (secondsLeft > 0) {
                Text(
                    text = "Didn't get a code? Resend in ${formatCountdown(secondsLeft)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Didn't get a code?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StudyZenTextButton(
                    text = "Resend code",
                    onClick = ::resend,
                    enabled = !resendInFlight,
                )
            }
        }
    }
}

/** 27 -> "0:27", 75 -> "1:15". */
internal fun formatCountdown(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

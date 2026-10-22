package com.fredrickemmanuel.studyzen.feature.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenLogo
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.feature.auth.AuthValidation
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthErrorBanner
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthFooterLink
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthScaffold
import com.fredrickemmanuel.studyzen.feature.auth.components.GoogleAuthSection
import com.fredrickemmanuel.studyzen.feature.auth.data.AuthRepository
import com.fredrickemmanuel.studyzen.feature.auth.google.GoogleSignInClient
import com.fredrickemmanuel.studyzen.feature.auth.rememberSubmitState

@Composable
fun LoginScreen(
    repository: AuthRepository,
    googleClient: GoogleSignInClient,
    onBack: () -> Unit,
    onLoggedIn: () -> Unit,
    onForgotPassword: () -> Unit,
    onSignUp: () -> Unit,
    onNeedsVerification: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    onContinueAsGuest: (() -> Unit)? = null,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val submit = rememberSubmitState()

    val emailError = if (attempted) AuthValidation.emailError(email) else null
    val passwordError = if (attempted) AuthValidation.loginPasswordError(password) else null

    fun trySubmit() {
        attempted = true
        if (AuthValidation.emailError(email) == null && AuthValidation.loginPasswordError(password) == null) {
            submit.submit(
                action = { repository.login(email.trim(), password) },
                onSuccess = onLoggedIn,
                onEmailNotVerified = { onNeedsVerification(email.trim()) },
            )
        }
    }

    AuthScaffold(
        title = "Welcome back",
        subtitle = "Log in to continue your learning journey.",
        modifier = modifier,
        onBack = onBack,
        header = { StudyZenLogo(markSize = 36.dp) },
    ) {
        StudyZenTextField(
            value = email,
            onValueChange = {
                email = it
                submit.clearError()
            },
            label = "Email",
            placeholder = "you@example.com",
            error = emailError,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            enabled = !submit.isLoading,
        )
        Spacer(Modifier.height(16.dp))
        StudyZenTextField(
            value = password,
            onValueChange = {
                password = it
                submit.clearError()
            },
            label = "Password",
            placeholder = "Your password",
            error = passwordError,
            isPassword = true,
            imeAction = ImeAction.Done,
            enabled = !submit.isLoading,
            onImeAction = ::trySubmit,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StudyZenTextButton(text = "Forgot password?", onClick = onForgotPassword)
        }

        Column {
            submit.error?.let {
                AuthErrorBanner(message = it)
                Spacer(Modifier.height(12.dp))
            }
            StudyZenButton(
                text = "Log in",
                onClick = ::trySubmit,
                loading = submit.isLoading,
            )
        }

        Spacer(Modifier.height(20.dp))
        GoogleAuthSection(
            googleClient = googleClient,
            repository = repository,
            onSignedIn = onLoggedIn,
            enabled = !submit.isLoading,
        )

        Spacer(Modifier.height(16.dp))
        AuthFooterLink(
            prompt = "New to StudyZen?",
            action = "Sign up",
            onClick = onSignUp,
        )
        if (onContinueAsGuest != null) {
            StudyZenTextButton(text = "Try without an account", onClick = onContinueAsGuest, modifier = Modifier.fillMaxWidth())
        }
    }
}

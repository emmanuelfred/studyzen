package com.fredrickemmanuel.studyzen.feature.auth.signup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenLogo
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.feature.auth.AuthValidation
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthErrorBanner
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthFooterLink
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthScaffold
import com.fredrickemmanuel.studyzen.feature.auth.components.GoogleAuthSection
import com.fredrickemmanuel.studyzen.feature.auth.components.PasswordRequirements
import com.fredrickemmanuel.studyzen.feature.auth.data.AuthRepository
import com.fredrickemmanuel.studyzen.feature.auth.google.GoogleSignInClient
import com.fredrickemmanuel.studyzen.feature.auth.rememberSubmitState

/** Creates the account, then asks the caller to continue to email verification. */
@Composable
fun SignUpScreen(
    repository: AuthRepository,
    googleClient: GoogleSignInClient,
    onBack: () -> Unit,
    onSignedUp: (email: String) -> Unit,
    onGoogleSignedIn: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val submit = rememberSubmitState()

    val nameError = if (attempted) AuthValidation.nameError(name) else null
    val emailError = if (attempted) AuthValidation.emailError(email) else null
    val passwordError = if (attempted) AuthValidation.newPasswordError(password) else null
    val confirmError = if (attempted) AuthValidation.confirmPasswordError(password, confirmation) else null

    fun trySubmit() {
        attempted = true
        val valid = AuthValidation.nameError(name) == null &&
            AuthValidation.emailError(email) == null &&
            AuthValidation.newPasswordError(password) == null &&
            AuthValidation.confirmPasswordError(password, confirmation) == null
        if (valid) {
            val cleanEmail = email.trim()
            submit.submit(
                action = { repository.signUp(name.trim(), cleanEmail, password) },
                onSuccess = { onSignedUp(cleanEmail) },
            )
        }
    }

    AuthScaffold(
        title = "Create your account",
        subtitle = "Plan smarter study sessions and keep every topic in one place.",
        modifier = modifier,
        onBack = onBack,
        header = { StudyZenLogo(markSize = 36.dp) },
    ) {
        StudyZenTextField(
            value = name,
            onValueChange = { name = it; submit.clearError() },
            label = "Full name",
            placeholder = "Your name",
            error = nameError,
            enabled = !submit.isLoading,
        )
        Spacer(Modifier.height(16.dp))
        StudyZenTextField(
            value = email,
            onValueChange = { email = it; submit.clearError() },
            label = "Email",
            placeholder = "you@example.com",
            error = emailError,
            keyboardType = KeyboardType.Email,
            enabled = !submit.isLoading,
        )
        Spacer(Modifier.height(16.dp))
        StudyZenTextField(
            value = password,
            onValueChange = { password = it; submit.clearError() },
            label = "Password",
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
            label = "Confirm password",
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
            text = "Create account",
            onClick = ::trySubmit,
            loading = submit.isLoading,
        )

        Spacer(Modifier.height(20.dp))
        GoogleAuthSection(
            googleClient = googleClient,
            repository = repository,
            onSignedIn = onGoogleSignedIn,
            enabled = !submit.isLoading,
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = "By continuing you agree to the StudyZen Terms and Privacy Policy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        AuthFooterLink(
            prompt = "Already have an account?",
            action = "Log in",
            onClick = onLogin,
        )
    }
}

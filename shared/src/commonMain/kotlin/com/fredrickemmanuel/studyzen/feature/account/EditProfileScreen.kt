package com.fredrickemmanuel.studyzen.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.feature.auth.AuthValidation
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthErrorBanner
import com.fredrickemmanuel.studyzen.feature.auth.rememberSubmitState
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator

/** Change the display name. The e-mail address is shown but cannot be changed yet. */
@Composable
fun EditProfileScreen() {
    val nav = LocalAppNavigator.current
    val store = LocalStudyStore.current
    val account = LocalAccountRepository.current
    val colors = MaterialTheme.colorScheme
    val submit = rememberSubmitState()

    val saved = store.profile.name
    var name by remember { mutableStateOf(saved) }
    var showErrors by remember { mutableStateOf(false) }
    val nameError = AuthValidation.nameError(name)

    ScreenScaffold(title = "Edit profile", onBack = nav::back) {
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(84.dp).clip(CircleShape).background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.trim().firstOrNull()?.uppercase() ?: "S",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            submit.error?.let { AuthErrorBanner(it) }

            StudyZenTextField(
                value = name,
                onValueChange = { name = it; submit.clearError() },
                label = "Full name",
                error = if (showErrors) nameError else null,
            )
            StudyZenTextField(
                value = store.profile.email,
                onValueChange = {},
                label = "Email",
                keyboardType = KeyboardType.Email,
                enabled = false,
            )
            Text(
                "Your email is how you log in, so it can't be changed yet.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
        StudyZenButton(
            text = "Save changes",
            enabled = name.trim() != saved,
            loading = submit.isLoading,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                showErrors = true
                if (nameError == null) {
                    submit.submit(
                        action = { account.updateName(name) },
                        onSuccess = {
                            store.profile = store.profile.copy(name = name.trim())
                            nav.back()
                        },
                    )
                }
            },
        )
    }
}

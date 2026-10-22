package com.fredrickemmanuel.studyzen.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.AppIcon
import com.fredrickemmanuel.studyzen.core.designsystem.components.BannerTone
import com.fredrickemmanuel.studyzen.core.designsystem.components.InfoBanner
import com.fredrickemmanuel.studyzen.core.designsystem.components.ScreenScaffold
import com.fredrickemmanuel.studyzen.core.designsystem.components.SectionHeader
import com.fredrickemmanuel.studyzen.core.designsystem.components.SettingsRow
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenCard
import com.fredrickemmanuel.studyzen.core.designsystem.components.StudyZenTextField
import com.fredrickemmanuel.studyzen.data.LocalStudyStore
import com.fredrickemmanuel.studyzen.feature.auth.AuthValidation
import com.fredrickemmanuel.studyzen.feature.auth.components.AuthErrorBanner
import com.fredrickemmanuel.studyzen.feature.auth.components.PasswordRequirements
import com.fredrickemmanuel.studyzen.feature.auth.rememberSubmitState
import com.fredrickemmanuel.studyzen.navigation.LocalAppNavigator
import com.fredrickemmanuel.studyzen.navigation.LocalSyncEngine
import com.fredrickemmanuel.studyzen.navigation.Screen

/** Password, logging out of every device, and deleting the account. */
@Composable
fun SecurityScreen() {
    val nav = LocalAppNavigator.current
    val store = LocalStudyStore.current
    val account = LocalAccountRepository.current
    val sync = LocalSyncEngine.current
    var backedUp by remember { mutableStateOf(true) }
    val colors = MaterialTheme.colorScheme

    var hasPassword by remember { mutableStateOf(true) }
    LaunchedEffect(account) { account.loadAccount()?.let { hasPassword = it.hasPassword } }

    // --- password form
    val passwordSubmit = rememberSubmitState()
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    var passwordSaved by remember { mutableStateOf(false) }

    val currentError = if (hasPassword) AuthValidation.loginPasswordError(current) else null
    val newError = AuthValidation.newPasswordError(newPassword)
    val confirmError = AuthValidation.confirmPasswordError(newPassword, confirm)

    // --- log out everywhere / delete
    val logoutSubmit = rememberSubmitState()
    var confirmLogout by remember { mutableStateOf(false) }
    val deleteSubmit = rememberSubmitState()
    var confirmDelete by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }
    var deleteWord by remember { mutableStateOf("") }

    ScreenScaffold(title = "Security", onBack = nav::back) {
        Spacer(Modifier.height(8.dp))
        SectionHeader(if (hasPassword) "Change password" else "Set a password")
        Spacer(Modifier.height(8.dp))
        StudyZenCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!hasPassword) {
                    InfoBanner("You signed in with Google. Set a password to also log in with your email.")
                }
                if (passwordSaved) {
                    InfoBanner("Password updated. Your other devices were logged out.")
                }
                passwordSubmit.error?.let { AuthErrorBanner(it) }

                if (hasPassword) {
                    StudyZenTextField(
                        value = current,
                        onValueChange = { current = it; passwordSubmit.clearError(); passwordSaved = false },
                        label = "Current password",
                        isPassword = true,
                        error = if (showErrors) currentError else null,
                    )
                }
                StudyZenTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; passwordSubmit.clearError(); passwordSaved = false },
                    label = "New password",
                    isPassword = true,
                    error = if (showErrors) newError else null,
                )
                PasswordRequirements(newPassword)
                StudyZenTextField(
                    value = confirm,
                    onValueChange = { confirm = it; passwordSubmit.clearError(); passwordSaved = false },
                    label = "Confirm new password",
                    isPassword = true,
                    error = if (showErrors) confirmError else null,
                )
                StudyZenButton(
                    text = if (hasPassword) "Update password" else "Set password",
                    loading = passwordSubmit.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showErrors = true
                        if (currentError == null && newError == null && confirmError == null) {
                            passwordSubmit.submit(
                                action = { account.changePassword(if (hasPassword) current else null, newPassword) },
                                onSuccess = {
                                    hasPassword = true
                                    current = ""; newPassword = ""; confirm = ""
                                    showErrors = false
                                    passwordSaved = true
                                },
                            )
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("Devices")
        Spacer(Modifier.height(8.dp))
        StudyZenCard(contentPadding = 8) {
            SettingsRow(
                AppIcon.Logout,
                "Log out of all devices",
                subtitle = "Ends every login of your account, including this one",
                onClick = { logoutSubmit.clearError(); confirmLogout = true },
            )
        }
        logoutSubmit.error?.let {
            Spacer(Modifier.height(8.dp))
            AuthErrorBanner(it)
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("Danger zone")
        Spacer(Modifier.height(8.dp))
        StudyZenCard(contentPadding = 8) {
            SettingsRow(
                AppIcon.Trash,
                "Delete account",
                subtitle = "Permanently removes your account and sign-in",
                titleColor = colors.error,
                iconTint = colors.error,
                onClick = {
                    deleteSubmit.clearError()
                    deletePassword = ""; deleteWord = ""
                    confirmDelete = true
                },
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { if (!logoutSubmit.isLoading) confirmLogout = false },
            title = { Text("Log out of all devices?") },
            text = { Text("You will be logged out here too and need to log in again.") },
            confirmButton = {
                TextButton(
                    enabled = !logoutSubmit.isLoading,
                    onClick = {
                        logoutSubmit.submit(
                            action = {
                                backedUp = sync?.finalSync() ?: true // save this phone's changes first
                                account.logoutEverywhere()
                            },
                            onSuccess = {
                                confirmLogout = false
                                if (backedUp) sync?.forgetLocalData()
                                nav.replaceAll(Screen.Login)
                            },
                        )
                    },
                ) { Text(if (logoutSubmit.isLoading) "Logging out..." else "Log out", color = colors.error) }
            },
            dismissButton = {
                TextButton(enabled = !logoutSubmit.isLoading, onClick = { confirmLogout = false }) { Text("Cancel") }
            },
        )
    }

    if (confirmDelete) {
        val ready = deleteWord == "DELETE" && (!hasPassword || deletePassword.isNotEmpty())
        AlertDialog(
            onDismissRequest = { if (!deleteSubmit.isLoading) confirmDelete = false },
            title = { Text("Delete your account?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your account and sign-in are removed for good. This cannot be undone.")
                    deleteSubmit.error?.let { AuthErrorBanner(it) }
                    if (hasPassword) {
                        StudyZenTextField(
                            value = deletePassword,
                            onValueChange = { deletePassword = it; deleteSubmit.clearError() },
                            label = "Your password",
                            isPassword = true,
                        )
                    }
                    StudyZenTextField(
                        value = deleteWord,
                        onValueChange = { deleteWord = it; deleteSubmit.clearError() },
                        label = "Type DELETE to confirm",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = ready && !deleteSubmit.isLoading,
                    onClick = {
                        deleteSubmit.submit(
                            action = { account.deleteAccount(if (hasPassword) deletePassword else null) },
                            onSuccess = {
                                confirmDelete = false
                                if (sync != null) sync.forgetLocalData() else store.clearAll()
                                nav.replaceAll(Screen.Login)
                            },
                        )
                    },
                ) { Text(if (deleteSubmit.isLoading) "Deleting..." else "Delete", color = colors.error) }
            },
            dismissButton = {
                TextButton(enabled = !deleteSubmit.isLoading, onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
    }
}

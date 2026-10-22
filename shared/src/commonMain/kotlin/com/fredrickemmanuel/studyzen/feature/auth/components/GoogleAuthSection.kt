package com.fredrickemmanuel.studyzen.feature.auth.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickemmanuel.studyzen.core.designsystem.components.GoogleButton
import com.fredrickemmanuel.studyzen.core.designsystem.components.OrDivider
import com.fredrickemmanuel.studyzen.feature.auth.data.AuthRepository
import com.fredrickemmanuel.studyzen.feature.auth.data.AuthResult
import com.fredrickemmanuel.studyzen.feature.auth.google.GoogleSignInClient
import com.fredrickemmanuel.studyzen.feature.auth.google.GoogleSignInResult
import com.fredrickemmanuel.studyzen.feature.auth.rememberSubmitState
import kotlinx.coroutines.CancellationException

/**
 * "or" divider + Continue with Google button, with its own loading and error state.
 * Used on both the login and sign-up screens.
 *
 * Flow: [googleClient] shows the Google account picker and returns an ID token, which is sent to
 * the backend with [AuthRepository.loginWithGoogle]. [onSignedIn] runs when the backend accepts it.
 */
@Composable
fun GoogleAuthSection(
    googleClient: GoogleSignInClient,
    repository: AuthRepository,
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val state = rememberSubmitState()

    Column(modifier) {
        OrDivider()
        Spacer(Modifier.height(20.dp))
        GoogleButton(
            onClick = {
                state.submit(
                    action = {
                        try {
                            when (val result = googleClient.signIn()) {
                                is GoogleSignInResult.Success -> repository.loginWithGoogle(result.idToken)
                                GoogleSignInResult.Cancelled -> AuthResult.Cancelled
                                is GoogleSignInResult.Error -> AuthResult.Failure(result.message)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            AuthResult.Failure("Google sign-in failed. Please try again.")
                        }
                    },
                    onSuccess = onSignedIn,
                )
            },
            enabled = enabled,
            loading = state.isLoading,
        )
        state.error?.let {
            Spacer(Modifier.height(12.dp))
            AuthErrorBanner(message = it)
        }
    }
}

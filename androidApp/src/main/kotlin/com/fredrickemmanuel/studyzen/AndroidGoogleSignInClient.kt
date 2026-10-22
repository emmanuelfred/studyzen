package com.fredrickemmanuel.studyzen

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.fredrickemmanuel.studyzen.feature.auth.google.GoogleSignInClient
import com.fredrickemmanuel.studyzen.feature.auth.google.GoogleSignInResult
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/**
 * Real "Continue with Google" on Android, using Credential Manager.
 *
 * It shows Google's account picker and returns a Google ID token. The StudyZen server checks
 * that token with Google (POST /auth/google).
 *
 * [activityContext] must be an Activity (the picker is shown on top of it).
 * [webClientId] is the OAuth **Web application** client ID from Google Cloud Console, the same
 * one the server has in GOOGLE_CLIENT_IDS. It is not a secret.
 */
class AndroidGoogleSignInClient(
    private val activityContext: Context,
    private val webClientId: String,
) : GoogleSignInClient {

    override suspend fun signIn(): GoogleSignInResult {
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()

        return try {
            val result = CredentialManager.create(activityContext).getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleSignInResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleSignInResult.Error("Google sign-in returned something unexpected. Please try again.")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            println("StudyZen Google sign-in: no credential: ${e.message}")
            GoogleSignInResult.Error(
                "No Google account could be used. Add a Google account to this phone and try again.",
            )
        } catch (e: GetCredentialException) {
            println("StudyZen Google sign-in: ${e::class.simpleName}: ${e.message}")
            GoogleSignInResult.Error("Google sign-in failed. Please try again.")
        } catch (e: GoogleIdTokenParsingException) {
            println("StudyZen Google sign-in: bad token: ${e.message}")
            GoogleSignInResult.Error("Google sign-in failed. Please try again.")
        }
    }
}

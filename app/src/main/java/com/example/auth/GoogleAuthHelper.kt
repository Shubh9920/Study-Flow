package com.example.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GoogleUserData(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null,
    val idToken: String? = null
)

class GoogleAuthHelper(private val context: Context) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle(
        serverClientId: String? = null,
        onSuccess: (GoogleUserData) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.Main) {
            try {
                // If a server client ID is provided or default placeholder
                val clientId = serverClientId?.ifBlank { null }
                    ?: "dummy-studyflow-client-id.apps.googleusercontent.com"

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response: GetCredentialResponse = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                handleCredentialResponse(response, onSuccess, onError)
            } catch (e: GetCredentialCancellationException) {
                onError("Sign in was cancelled.")
            } catch (e: NoCredentialException) {
                onError("No Google accounts found on this device or emulator.")
            } catch (e: GetCredentialException) {
                onError("Google Sign-In service message: ${e.message}")
            } catch (e: Exception) {
                onError("Sign in error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    private fun handleCredentialResponse(
        response: GetCredentialResponse,
        onSuccess: (GoogleUserData) -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val userData = GoogleUserData(
                    email = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id.substringBefore("@"),
                    photoUrl = googleIdTokenCredential.profilePictureUri?.toString(),
                    idToken = googleIdTokenCredential.idToken
                )
                onSuccess(userData)
            } catch (e: Exception) {
                onError("Failed to parse Google credentials: ${e.message}")
            }
        } else {
            onError("Unexpected credential type received.")
        }
    }

    suspend fun signOut(onComplete: () -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {}
        }
        withContext(Dispatchers.Main) {
            onComplete()
        }
    }
}

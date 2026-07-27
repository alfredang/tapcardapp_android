package com.tertiaryinfotech.tapcard.net

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/** Outcome of the system Google account picker. */
sealed interface GoogleSignInOutcome {
    /** The user picked an account; [idToken] is ready to POST to the backend. */
    data class Success(val idToken: String) : GoogleSignInOutcome

    /** The user dismissed the picker — treat as a no-op, not an error. */
    data object Cancelled : GoogleSignInOutcome

    /** Something went wrong; [message] is safe to show the user. */
    data class Failure(val message: String) : GoogleSignInOutcome
}

/**
 * Runs Google sign-in via Credential Manager and returns a Google ID token to
 * hand to [TapcardApi.googleSignIn]. Needs an **Activity** context, so call it
 * from a composable with `LocalContext.current` — the account picker is system
 * UI that can't be shown from the application context.
 *
 * [serverClientId] must be the Web OAuth client ID (see
 * [ApiConfig.GOOGLE_SERVER_CLIENT_ID]); it becomes the token's `aud`, which the
 * backend checks.
 */
suspend fun requestGoogleIdToken(context: Context, serverClientId: String): GoogleSignInOutcome {
    if (serverClientId.isBlank() || serverClientId.startsWith("REPLACE_")) {
        return GoogleSignInOutcome.Failure("Google sign-in isn't set up yet. Try email instead.")
    }

    val option = GetGoogleIdOption.Builder()
        .setServerClientId(serverClientId)
        // false → offer every Google account on the device, so first-time users
        // (no Tapcard account yet) can sign up, not just returning ones.
        .setFilterByAuthorizedAccounts(false)
        .setAutoSelectEnabled(false)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

    return try {
        val result = CredentialManager.create(context).getCredential(context, request)
        val cred = result.credential
        if (cred is CustomCredential &&
            cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleSignInOutcome.Success(GoogleIdTokenCredential.createFrom(cred.data).idToken)
        } else {
            GoogleSignInOutcome.Failure("Unexpected sign-in response from Google. Try again.")
        }
    } catch (_: GetCredentialCancellationException) {
        GoogleSignInOutcome.Cancelled
    } catch (_: NoCredentialException) {
        GoogleSignInOutcome.Failure("No Google account on this device. Add one in Settings, then try again.")
    } catch (_: GoogleIdTokenParsingException) {
        GoogleSignInOutcome.Failure("Couldn't read the Google response. Try again.")
    } catch (_: GetCredentialException) {
        GoogleSignInOutcome.Failure("Google sign-in failed. Try again.")
    }
}

package com.pamaz.vi.core.data.auth

import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.pamaz.vi.core.data.repository.AccountErrorReason
import com.pamaz.vi.core.database.PlatformContext
import java.security.MessageDigest
import java.util.UUID

/** Real implementation — see this class's commonMain KDoc and `docs/auth-notes.md` §1 for the nonce handling this follows exactly. */
actual class GoogleIdTokenRequester actual constructor(private val context: PlatformContext) {

    actual suspend fun requestIdToken(webClientId: String): Result<GoogleIdTokenResult> {
        val rawNonce = UUID.randomUUID().toString()
        val hashedNonce = sha256Hex(rawNonce)
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .setFilterByAuthorizedAccounts(false)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return try {
            val credentialManager = CredentialManager.create(context.context)
            val response = credentialManager.getCredential(context.context, request)
            val googleIdTokenCredential =
                GoogleIdTokenCredential.createFrom(response.credential.data)
            Result.success(
                GoogleIdTokenResult(
                    idToken = googleIdTokenCredential.idToken,
                    rawNonce = rawNonce
                )
            )
        } catch (e: GetCredentialCancellationException) {
            Result.failure(NativeSignInException(AccountErrorReason.CANCELLED))
        } catch (e: NoCredentialException) {
            Result.failure(NativeSignInException(AccountErrorReason.NO_ACCOUNT))
        } catch (e: GetCredentialProviderConfigurationException) {
            Result.failure(NativeSignInException(AccountErrorReason.DEVELOPER_ERROR))
        } catch (e: GetCredentialInterruptedException) {
            Result.failure(NativeSignInException(AccountErrorReason.OFFLINE))
        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(NativeSignInException(AccountErrorReason.DEVELOPER_ERROR))
        } catch (e: GetCredentialException) {
            Result.failure(NativeSignInException(AccountErrorReason.UNKNOWN))
        }
    }
}

private fun sha256Hex(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

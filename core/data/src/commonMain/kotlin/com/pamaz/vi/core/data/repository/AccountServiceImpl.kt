package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.data.auth.AppleIdTokenRequester
import com.pamaz.vi.core.data.auth.GoogleIdTokenRequester
import com.pamaz.vi.core.data.auth.NativeSignInException
import com.pamaz.vi.core.data.getPlatformName
import com.pamaz.vi.core.network.config.GoogleAuthConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.RefreshFailureCause
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Google everywhere, Apple iOS-only — see `docs/auth-notes.md` §1/§3 for why. */
private val GOOGLE_AVAILABLE_PLATFORMS = setOf("Android", "iOS")
private val APPLE_AVAILABLE_PLATFORMS = setOf("iOS")

/**
 * Real [AccountService]: wraps the shared [SupabaseClient]'s `Auth` plugin. Lives in `:core:data`,
 * not `:core:network` (despite `:core:network` owning the Supabase client construction) — this
 * class implements a `:core:data` interface and needs `:core:data`'s own dependency on
 * `:core:network`'s public API, the reverse direction would be circular. See `docs/auth-notes.md`
 * for the research behind every choice below (native Android ID-token flow, iOS web-OAuth for
 * Google + native Apple, the error-reason mapping, the `delete_own_account` RPC).
 */
internal class AccountServiceImpl(
    private val supabase: SupabaseClient,
    private val googleIdTokenRequester: GoogleIdTokenRequester,
    private val appleIdTokenRequester: AppleIdTokenRequester,
) : AccountService {

    override val state: Flow<AccountState> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Initializing -> AccountState.Loading
            is SessionStatus.NotAuthenticated -> AccountState.SignedOut
            is SessionStatus.Authenticated -> status.session.user?.toAccountUser()
                ?.let { AccountState.SignedIn(it) }
                ?: AccountState.Error(AccountErrorReason.UNKNOWN)
            is SessionStatus.RefreshFailure -> AccountState.Error(status.cause.toReason())
        }
    }

    override val availableProviders: List<AuthProviderId> = buildList {
        val platform = getPlatformName()
        // Gated on GoogleAuthConfig.isConfigured() too, not just the platform check: without a real
        // GOOGLE_WEB_CLIENT_ID, tapping this button could only ever fail signInWithGoogle()'s own
        // require() — hiding it here is the real fix for that, not a nicer error message for it.
        if (platform in GOOGLE_AVAILABLE_PLATFORMS && GoogleAuthConfig.isConfigured()) add(
            AuthProviderId.GOOGLE
        )
        if (platform in APPLE_AVAILABLE_PLATFORMS) add(AuthProviderId.APPLE)
    }

    override suspend fun signIn(provider: AuthProviderId): Result<Unit> = try {
        when (provider) {
            AuthProviderId.GOOGLE -> signInWithGoogle()
            AuthProviderId.APPLE -> signInWithApple()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: NativeSignInException) {
        Result.failure(AccountErrorException(e.reason))
    } catch (e: RestException) {
        Result.failure(AccountErrorException(e.toReason()))
    } catch (e: HttpRequestException) {
        Result.failure(AccountErrorException(AccountErrorReason.OFFLINE))
    } catch (e: Exception) {
        Result.failure(AccountErrorException(AccountErrorReason.UNKNOWN))
    }

    private suspend fun signInWithGoogle() {
        val platform = getPlatformName()
        val webClientId = GoogleAuthConfig.fromBuildConfig().webClientId
        if (platform == "Android") {
            val tokenResult = googleIdTokenRequester.requestIdToken(webClientId).getOrThrow()
            supabase.auth.signInWith(IDToken) {
                idToken = tokenResult.idToken
                this.provider = Google
                nonce = tokenResult.rawNonce
            }
        } else {
            supabase.auth.signInWith(Google, redirectUrl = GOOGLE_SIGN_IN_REDIRECT) {}
        }
    }

    private suspend fun signInWithApple() {
        val tokenResult = appleIdTokenRequester.requestIdToken().getOrThrow()
        supabase.auth.signInWith(IDToken) {
            idToken = tokenResult.identityToken
            this.provider = Apple
            nonce = tokenResult.rawNonce
        }
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
    }

    override suspend fun deleteAccount(): Result<Unit> = try {
        supabase.postgrest.rpc("delete_own_account")
        supabase.auth.signOut()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: RestException) {
        Result.failure(AccountErrorException(e.toReason()))
    } catch (e: HttpRequestException) {
        Result.failure(AccountErrorException(AccountErrorReason.OFFLINE))
    } catch (e: Exception) {
        Result.failure(AccountErrorException(AccountErrorReason.UNKNOWN))
    }
}

/** The [Result.failure] carrier for [AccountService.signIn]/[AccountService.deleteAccount] — `:feature:settings` reads [reason], never [Throwable.message]. */
class AccountErrorException(val reason: AccountErrorReason) : Exception()

private const val GOOGLE_SIGN_IN_REDIRECT = "com.pamaz.vi://login-callback"

private fun UserInfo.toAccountUser(): AccountUser {
    val metadata: JsonObject = userMetadata ?: JsonObject(emptyMap())
    val displayName = metadata["full_name"]?.jsonPrimitive?.contentOrNull
        ?: metadata["name"]?.jsonPrimitive?.contentOrNull
    val avatarUrl = metadata["avatar_url"]?.jsonPrimitive?.contentOrNull
        ?: metadata["picture"]?.jsonPrimitive?.contentOrNull
    return AccountUser(id = id, displayName = displayName, email = email, avatarUrl = avatarUrl)
}

private fun RefreshFailureCause.toReason(): AccountErrorReason = when (this) {
    is RefreshFailureCause.NetworkError -> AccountErrorReason.OFFLINE
    is RefreshFailureCause.InternalServerError -> AccountErrorReason.UNKNOWN
}

/**
 * Maps a Postgrest/Auth REST failure's HTTP status to a reason — never reads [RestException.message]
 * (which can echo back request details) into anything shown to the user or logged beyond this.
 */
private fun RestException.toReason(): AccountErrorReason = when (statusCode) {
    401, 403 -> AccountErrorReason.NOT_ALLOWED
    else -> AccountErrorReason.UNKNOWN
}

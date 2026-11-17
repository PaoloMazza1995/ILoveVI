package com.pamaz.vi.core.data.repository

import kotlinx.coroutines.flow.Flow

/** Which third-party identity a [AccountService.signIn] call (or `availableProviders` entry) refers to. */
enum class AuthProviderId { GOOGLE, APPLE }

data class AccountUser(
    val id: String,
    val displayName: String?,
    val email: String?,
    val avatarUrl: String?,
)

/**
 * A closed, platform-independent reason code — never a raw exception message, a token, a key, or
 * a provider's internal error string. [AccountState.Error] carries one of these, not a message:
 * `:feature:settings`'s `SettingsViewModel` is the one place that maps it to a real, localized
 * (English/Italian) sentence via `composeResources`, since the `core:data`/`core:network` layers
 * this is produced in have no access to string resources. See `docs/auth-notes.md` §5 for exactly
 * which real exceptions map to which of these.
 */
enum class AccountErrorReason {
    /** The user dismissed the sign-in sheet/dialog — not a real failure, shown briefly or not at all. */
    CANCELLED,

    /** No Google account on this device (Android), or the user has no account to pick (iOS/web). */
    NO_ACCOUNT,

    /** Android Credential Manager "developer error"-shaped failure: wrong package name/SHA-1 registered for this build. */
    DEVELOPER_ERROR,

    /** Supabase rejected the sign-in — e.g. the Google consent screen is still in Testing mode and this account isn't an added test user. */
    NOT_ALLOWED,

    /** The OAuth redirect URL Supabase received doesn't match what's registered. */
    REDIRECT_MISMATCH,

    /** The Google/Apple provider isn't enabled in the Supabase project. */
    PROVIDER_DISABLED,

    /** No network connection. */
    OFFLINE,
    UNKNOWN,
}

sealed interface AccountState {
    data object SignedOut : AccountState
    data object Loading : AccountState
    data class SignedIn(val user: AccountUser) : AccountState
    data class Error(val reason: AccountErrorReason) : AccountState
}

/**
 * Optional account, Google (Android + iOS) and Apple (iOS only — see `docs/auth-notes.md` §1/§3
 * for why) sign-in. The app never requires an account for anything and keeps working fully
 * offline/signed-out — this interface only ever adds capability, it's never read as a gate.
 *
 * `:feature:settings`'s `SettingsViewModel` depends only on this interface, never on
 * `:core:network`'s implementation or on supabase-kt's types directly — same "feature depends on
 * `:core:data`'s interface" rule every other repository in this module already follows (see
 * `NewsRepository`/`ContentSyncer`). The real implementation (`AccountServiceImpl`, wrapping
 * supabase-kt's `Auth` plugin plus one internal `AuthProvider` per [AuthProviderId]) lives in
 * `:core:network`, per this step's `docs/auth-notes.md`.
 */
interface AccountService {
    val state: Flow<AccountState>

    /** Platform-filtered: [AuthProviderId.APPLE] is only ever present on iOS. */
    val availableProviders: List<AuthProviderId>

    suspend fun signIn(provider: AuthProviderId): Result<Unit>
    suspend fun signOut()

    /** Calls the backend's `delete_own_account` RPC, then clears the local session either way the call completes. */
    suspend fun deleteAccount(): Result<Unit>
}

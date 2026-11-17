package com.pamaz.vi.feature.settings.settings

import com.pamaz.vi.core.data.repository.AccountErrorReason
import com.pamaz.vi.core.data.repository.AuthProviderId
import com.pamaz.vi.core.data.spotify.SpotifyConnectionState
import kotlin.time.Instant

/** Account section — mirrors `:core:data`'s `AccountState` one-to-one, but flattens `SignedIn`'s user fields for the screen (no need to reach into a nested `AccountUser`). */
sealed interface AccountUiState {
    data object SignedOut : AccountUiState
    data object Loading : AccountUiState
    data class SignedIn(val displayName: String?, val email: String?, val avatarUrl: String?) :
        AccountUiState

    data class Error(val reason: AccountErrorReason) : AccountUiState
}

/** `:core:data`'s `Preferences.themeMode` is a plain `String` (that module has no `:core:design`
 * dependency, see its own KDoc); this is the one typed enum this UI state and the screen itself
 * use, mapped to/from that string only in [SettingsViewModel]. */
enum class ThemeModeOption { SYSTEM, LIGHT, DARK }

enum class LanguageOption { EN, IT }

/**
 * One state for the whole Settings screen — every section's data in one place, per this step's
 * "one UI state per screen" requirement. [availableProviders] drives which sign-in button(s) the
 * Account section's signed-out state renders (Google everywhere, Apple iOS-only). [spotifyState]
 * always reflects the real `SpotifyConnection` (today, always [SpotifyConnectionState.Unavailable])
 * — `FeatureFlags.spotify` is checked only by the screen's render logic (a compile-time constant,
 * not something this state can vary by), not by whether this field is populated.
 */
data class SettingsUiState(
    val account: AccountUiState = AccountUiState.SignedOut,
    val availableProviders: List<AuthProviderId> = emptyList(),
    val isDeletingAccount: Boolean = false,
    val deleteAccountError: AccountErrorReason? = null,
    val themeMode: ThemeModeOption = ThemeModeOption.SYSTEM,
    val language: LanguageOption = LanguageOption.EN,
    val showLabels: Boolean = true,
    val lastUpdateAt: Instant? = null,
    val isRefreshingData: Boolean = false,
    val dataRefreshError: String? = null,
    val spotifyState: SpotifyConnectionState = SpotifyConnectionState.Unavailable,
    val appVersion: String = "",
)

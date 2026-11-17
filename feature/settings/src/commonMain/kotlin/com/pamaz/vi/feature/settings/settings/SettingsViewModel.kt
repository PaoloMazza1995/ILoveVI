package com.pamaz.vi.feature.settings.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pamaz.vi.core.data.FeatureFlags
import com.pamaz.vi.core.data.repository.AccountErrorException
import com.pamaz.vi.core.data.repository.AccountErrorReason
import com.pamaz.vi.core.data.repository.AccountService
import com.pamaz.vi.core.data.repository.AccountState
import com.pamaz.vi.core.data.repository.AuthProviderId
import com.pamaz.vi.core.data.repository.Preferences
import com.pamaz.vi.core.data.repository.SettingsRepository
import com.pamaz.vi.core.data.spotify.SpotifyConnection
import com.pamaz.vi.core.data.spotify.SpotifyConnectionState
import com.pamaz.vi.core.data.sync.ContentSyncer
import com.pamaz.vi.core.data.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

private val DEFAULT_PREFERENCES = Preferences(
    showLabels = true,
    language = "en",
    selectedNewsTopic = null,
    lastContentUpdate = null,
    themeMode = "SYSTEM",
)

/** The four fields that aren't owned by any repository `Flow` — one-shot, ViewModel-local state from an in-flight sign-in/delete call. */
private data class LocalExtras(
    val isSigningIn: Boolean = false,
    val signInError: AccountErrorReason? = null,
    val isDeletingAccount: Boolean = false,
    val deleteAccountError: AccountErrorReason? = null,
)

/**
 * Backs the Settings tab. [AccountService]'s own `state` only ever reports [AccountState.Error]
 * for a *refresh* failure on an already-signed-in session — a failed *sign-in attempt* (cancelled,
 * no account, developer error, ...) is a [Result.failure] from [AccountService.signIn] itself,
 * which never touches `AccountService.state` at all (it was never signed in to begin with). This
 * class's own [LocalExtras.signInError] is where that result actually lands, merged with
 * [AccountService.state] into one [AccountUiState] the screen renders.
 *
 * [ContentSyncer] (not `:core:network` directly — same layering `NewsViewModel` already follows)
 * backs the Map section's "Data update" row; [SettingsRepository] backs Appearance (theme,
 * language) and the Map section's "Show labels" switch, read by `:feature:map`'s own
 * `MapViewModel` through the exact same repository, already wired, no change needed there.
 */
class SettingsViewModel(
    private val accountService: AccountService,
    private val settingsRepository: SettingsRepository,
    private val contentSyncer: ContentSyncer,
    private val spotifyConnection: SpotifyConnection,
    private val appVersion: String,
) : ViewModel() {

    private val localExtrasState = MutableStateFlow(LocalExtras())

    /**
     * Always reflects the real [SpotifyConnection.state] — unlike [SettingsScreen][com.pamaz.vi.feature.settings.settings.SettingsScreen],
     * which is the one place [FeatureFlags.spotify] is actually checked (a compile-time
     * `const val`, so it can't be varied in a test the way a constructor dependency can). Keeping
     * the flag-gate out of this class means [uiState]'s `spotifyState` is fully testable against a
     * fake [SpotifyConnection] regardless of the flag's current value.
     */
    val uiState: StateFlow<SettingsUiState> = combine(
        accountService.state,
        settingsRepository.preferences(),
        contentSyncer.syncState,
        spotifyConnection.state,
        localExtrasState,
    ) { accountState, preferences, syncState, spotifyState, extras ->
        SettingsUiState(
            account = accountState.toUiState(extras),
            availableProviders = accountService.availableProviders,
            isDeletingAccount = extras.isDeletingAccount,
            deleteAccountError = extras.deleteAccountError,
            themeMode = (preferences?.themeMode
                ?: DEFAULT_PREFERENCES.themeMode).toThemeModeOption(),
            language = (preferences?.language ?: DEFAULT_PREFERENCES.language).toLanguageOption(),
            showLabels = preferences?.showLabels ?: DEFAULT_PREFERENCES.showLabels,
            lastUpdateAt = syncState.lastUpdatedAt,
            isRefreshingData = syncState.status == SyncStatus.RUNNING,
            dataRefreshError = syncState.lastError,
            spotifyState = spotifyState,
            appVersion = appVersion,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(appVersion = appVersion),
    )

    fun onThemeModeSelect(mode: ThemeModeOption) {
        viewModelScope.launch { updatePreferences { it.copy(themeMode = mode.name) } }
    }

    fun onLanguageSelect(language: LanguageOption) {
        viewModelScope.launch { updatePreferences { it.copy(language = language.toCode()) } }
    }

    fun onShowLabelsToggle(enabled: Boolean) {
        viewModelScope.launch { updatePreferences { it.copy(showLabels = enabled) } }
    }

    fun onRefreshData() {
        viewModelScope.launch { contentSyncer.sync() }
    }

    fun onSignIn(provider: AuthProviderId) {
        viewModelScope.launch {
            localExtrasState.value =
                localExtrasState.value.copy(isSigningIn = true, signInError = null)
            val result = accountService.signIn(provider)
            val reason = result.exceptionOrNull()
                ?.let { (it as? AccountErrorException)?.reason ?: AccountErrorReason.UNKNOWN }
            localExtrasState.value =
                localExtrasState.value.copy(isSigningIn = false, signInError = reason)
        }
    }

    fun onSignOut() {
        viewModelScope.launch { accountService.signOut() }
    }

    fun onDeleteAccountConfirm() {
        viewModelScope.launch {
            localExtrasState.value =
                localExtrasState.value.copy(isDeletingAccount = true, deleteAccountError = null)
            val result = accountService.deleteAccount()
            val reason = result.exceptionOrNull()
                ?.let { (it as? AccountErrorException)?.reason ?: AccountErrorReason.UNKNOWN }
            localExtrasState.value =
                localExtrasState.value.copy(isDeletingAccount = false, deleteAccountError = reason)
        }
    }

    fun onSpotifyConnect() {
        viewModelScope.launch { spotifyConnection.connect() }
    }

    fun onSpotifyDisconnect() {
        viewModelScope.launch { spotifyConnection.disconnect() }
    }

    private suspend fun updatePreferences(transform: (Preferences) -> Preferences) {
        val current = settingsRepository.preferences().first() ?: DEFAULT_PREFERENCES
        settingsRepository.setPreferences(transform(current))
    }
}

private fun AccountState.toUiState(extras: LocalExtras): AccountUiState = when {
    extras.isSigningIn -> AccountUiState.Loading
    this is AccountState.Loading -> AccountUiState.Loading
    this is AccountState.SignedIn -> AccountUiState.SignedIn(
        displayName = user.displayName,
        email = user.email,
        avatarUrl = user.avatarUrl,
    )

    this is AccountState.Error -> AccountUiState.Error(reason)
    extras.signInError != null -> AccountUiState.Error(extras.signInError)
    else -> AccountUiState.SignedOut
}

private fun String.toThemeModeOption(): ThemeModeOption =
    ThemeModeOption.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }
        ?: ThemeModeOption.SYSTEM

private fun String.toLanguageOption(): LanguageOption =
    if (this == "it") LanguageOption.IT else LanguageOption.EN

private fun LanguageOption.toCode(): String = if (this == LanguageOption.IT) "it" else "en"

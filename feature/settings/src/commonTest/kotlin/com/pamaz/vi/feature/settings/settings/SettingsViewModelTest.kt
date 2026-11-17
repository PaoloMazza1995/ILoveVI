package com.pamaz.vi.feature.settings.settings

import com.pamaz.vi.core.data.repository.AccountErrorReason
import com.pamaz.vi.core.data.repository.AccountState
import com.pamaz.vi.core.data.repository.AccountUser
import com.pamaz.vi.core.data.repository.AuthProviderId
import com.pamaz.vi.core.data.repository.Preferences
import com.pamaz.vi.core.data.spotify.SpotifyConnectionState
import com.pamaz.vi.core.data.sync.SyncState
import com.pamaz.vi.core.data.sync.SyncStatus
import com.pamaz.vi.feature.settings.fake.FakeAccountService
import com.pamaz.vi.feature.settings.fake.FakeContentSyncer
import com.pamaz.vi.feature.settings.fake.FakeSettingsRepository
import com.pamaz.vi.feature.settings.fake.FakeSpotifyConnection
import com.pamaz.vi.feature.settings.fake.accountErrorFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val SOME_PREFERENCES = Preferences(
    showLabels = true,
    language = "en",
    selectedNewsTopic = null,
    lastContentUpdate = null,
    themeMode = "SYSTEM",
)

/** See `NewsViewModelTest`'s KDoc for why `Dispatchers.Main` must be a test dispatcher before the view model is constructed. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        accountService: FakeAccountService = FakeAccountService(),
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(SOME_PREFERENCES),
        contentSyncer: FakeContentSyncer = FakeContentSyncer(),
        spotifyConnection: FakeSpotifyConnection = FakeSpotifyConnection(),
        appVersion: String = "1.0",
    ) = SettingsViewModel(
        accountService,
        settingsRepository,
        contentSyncer,
        spotifyConnection,
        appVersion
    )

    // ---- Appearance: theme ----

    @Test
    fun onThemeModeSelect_persistsAndApplies() = runTest(mainDispatcher) {
        val repository = FakeSettingsRepository(SOME_PREFERENCES)
        val viewModel = viewModel(settingsRepository = repository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onThemeModeSelect(ThemeModeOption.DARK)
        advanceUntilIdle()

        assertEquals(ThemeModeOption.DARK, viewModel.uiState.value.themeMode)
        assertEquals("DARK", repository.currentPreferences()?.themeMode)
        collector.cancel()
    }

    @Test
    fun onThemeModeSelect_preservesOtherPreferenceFields() = runTest(mainDispatcher) {
        val repository =
            FakeSettingsRepository(SOME_PREFERENCES.copy(showLabels = false, language = "it"))
        val viewModel = viewModel(settingsRepository = repository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onThemeModeSelect(ThemeModeOption.LIGHT)
        advanceUntilIdle()

        val stored = repository.currentPreferences()
        assertEquals(false, stored?.showLabels)
        assertEquals("it", stored?.language)
        collector.cancel()
    }

    @Test
    fun defaultThemeMode_isSystem_whenNoPreferencesStoredYet() = runTest(mainDispatcher) {
        val viewModel = viewModel(settingsRepository = FakeSettingsRepository(null))
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        // "System" follows the device live via Theme.kt's isSystemInDarkTheme() — this view model
        // only has to pass the stored/default mode through unchanged, which this asserts.
        assertEquals(ThemeModeOption.SYSTEM, viewModel.uiState.value.themeMode)
        collector.cancel()
    }

    // ---- Appearance: language ----

    @Test
    fun onLanguageSelect_persistsAndApplies() = runTest(mainDispatcher) {
        val repository = FakeSettingsRepository(SOME_PREFERENCES)
        val viewModel = viewModel(settingsRepository = repository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onLanguageSelect(LanguageOption.IT)
        advanceUntilIdle()

        assertEquals(LanguageOption.IT, viewModel.uiState.value.language)
        assertEquals("it", repository.currentPreferences()?.language)
        collector.cancel()
    }

    // ---- Map: show labels ----

    @Test
    fun onShowLabelsToggle_persists() = runTest(mainDispatcher) {
        val repository = FakeSettingsRepository(SOME_PREFERENCES.copy(showLabels = true))
        val viewModel = viewModel(settingsRepository = repository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onShowLabelsToggle(false)
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.showLabels)
        assertEquals(false, repository.currentPreferences()?.showLabels)
        collector.cancel()
    }

    // ---- Map: data update / refresh ----

    @Test
    fun onRefreshData_triggersContentSyncerSync() = runTest(mainDispatcher) {
        val syncer = FakeContentSyncer()
        val viewModel = viewModel(contentSyncer = syncer)

        viewModel.onRefreshData()
        advanceUntilIdle()

        assertEquals(1, syncer.syncCallCount)
    }

    @Test
    fun dataSync_runningState_showsAsRefreshing() = runTest(mainDispatcher) {
        val syncer = FakeContentSyncer(
            SyncState(
                status = SyncStatus.RUNNING,
                lastUpdatedAt = null,
                lastError = null
            )
        )
        val viewModel = viewModel(contentSyncer = syncer)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isRefreshingData)
        collector.cancel()
    }

    @Test
    fun dataSync_error_surfacesInState() = runTest(mainDispatcher) {
        val syncer = FakeContentSyncer(
            SyncState(
                status = SyncStatus.IDLE,
                lastUpdatedAt = null,
                lastError = "weapons: offline"
            ),
        )
        val viewModel = viewModel(contentSyncer = syncer)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("weapons: offline", viewModel.uiState.value.dataRefreshError)
        collector.cancel()
    }

    // ---- Account ----

    @Test
    fun account_signedOut_isReflected() = runTest(mainDispatcher) {
        val viewModel =
            viewModel(accountService = FakeAccountService(initial = AccountState.SignedOut))
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertIs<AccountUiState.SignedOut>(viewModel.uiState.value.account)
        collector.cancel()
    }

    @Test
    fun account_signedIn_isReflected() = runTest(mainDispatcher) {
        val user = AccountUser(
            id = "u1",
            displayName = "Paolo",
            email = "paolo@example.com",
            avatarUrl = null
        )
        val viewModel =
            viewModel(accountService = FakeAccountService(initial = AccountState.SignedIn(user)))
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val account = assertIs<AccountUiState.SignedIn>(viewModel.uiState.value.account)
        assertEquals("Paolo", account.displayName)
        assertEquals("paolo@example.com", account.email)
        collector.cancel()
    }

    @Test
    fun googleSignIn_success_resolvesThroughAccountServiceState() = runTest(mainDispatcher) {
        val user =
            AccountUser(id = "u1", displayName = "Paolo", email = "p@example.com", avatarUrl = null)
        val accountService = FakeAccountService(initial = AccountState.SignedOut)
        accountService.signInResult = Result.success(Unit)
        accountService.onSignInEffect = { accountService.setState(AccountState.SignedIn(user)) }
        val viewModel = viewModel(accountService = accountService)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSignIn(AuthProviderId.GOOGLE)
        advanceUntilIdle()

        assertEquals(AuthProviderId.GOOGLE, accountService.lastSignInProvider)
        assertIs<AccountUiState.SignedIn>(viewModel.uiState.value.account)
        collector.cancel()
    }

    @Test
    fun googleSignIn_cancelled_showsCancelledError() = runTest(mainDispatcher) {
        val accountService = FakeAccountService()
        accountService.signInResult = accountErrorFailure(AccountErrorReason.CANCELLED)
        val viewModel = viewModel(accountService = accountService)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSignIn(AuthProviderId.GOOGLE)
        advanceUntilIdle()

        val account = assertIs<AccountUiState.Error>(viewModel.uiState.value.account)
        assertEquals(AccountErrorReason.CANCELLED, account.reason)
        collector.cancel()
    }

    @Test
    fun googleSignIn_developerError_isSurfaced() = runTest(mainDispatcher) {
        val accountService = FakeAccountService()
        accountService.signInResult = accountErrorFailure(AccountErrorReason.DEVELOPER_ERROR)
        val viewModel = viewModel(accountService = accountService)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSignIn(AuthProviderId.GOOGLE)
        advanceUntilIdle()

        val account = assertIs<AccountUiState.Error>(viewModel.uiState.value.account)
        assertEquals(AccountErrorReason.DEVELOPER_ERROR, account.reason)
        collector.cancel()
    }

    @Test
    fun googleSignIn_noNetwork_showsOfflineError() = runTest(mainDispatcher) {
        val accountService = FakeAccountService()
        accountService.signInResult = accountErrorFailure(AccountErrorReason.OFFLINE)
        val viewModel = viewModel(accountService = accountService)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSignIn(AuthProviderId.GOOGLE)
        advanceUntilIdle()

        val account = assertIs<AccountUiState.Error>(viewModel.uiState.value.account)
        assertEquals(AccountErrorReason.OFFLINE, account.reason)
        collector.cancel()
    }

    @Test
    fun signOut_callsAccountServiceAndReturnsToSignedOut() = runTest(mainDispatcher) {
        val user = AccountUser(id = "u1", displayName = "Paolo", email = null, avatarUrl = null)
        val accountService = FakeAccountService(initial = AccountState.SignedIn(user))
        val viewModel = viewModel(accountService = accountService)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSignOut()
        advanceUntilIdle()

        assertEquals(1, accountService.signOutCallCount)
        assertIs<AccountUiState.SignedOut>(viewModel.uiState.value.account)
        collector.cancel()
    }

    // ---- Danger zone: delete account ----

    @Test
    fun deleteAccount_success_clearsDeletingFlagAndError() = runTest(mainDispatcher) {
        val accountService = FakeAccountService()
        accountService.deleteAccountResult = Result.success(Unit)
        val viewModel = viewModel(accountService = accountService)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDeleteAccountConfirm()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isDeletingAccount)
        assertNull(viewModel.uiState.value.deleteAccountError)
        collector.cancel()
    }

    @Test
    fun deleteAccount_failure_surfacesReason() = runTest(mainDispatcher) {
        val accountService = FakeAccountService()
        accountService.deleteAccountResult = accountErrorFailure(AccountErrorReason.UNKNOWN)
        val viewModel = viewModel(accountService = accountService)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDeleteAccountConfirm()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isDeletingAccount)
        assertEquals(AccountErrorReason.UNKNOWN, viewModel.uiState.value.deleteAccountError)
        collector.cancel()
    }

    // ---- Spotify (flag checked only by the screen — see SettingsViewModel's own KDoc) ----

    @Test
    fun spotify_unavailable_isReflectedInState() = runTest(mainDispatcher) {
        val viewModel =
            viewModel(spotifyConnection = FakeSpotifyConnection(SpotifyConnectionState.Unavailable))
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(SpotifyConnectionState.Unavailable, viewModel.uiState.value.spotifyState)
        collector.cancel()
    }

    @Test
    fun spotify_disconnected_isReflectedInState() = runTest(mainDispatcher) {
        val viewModel =
            viewModel(spotifyConnection = FakeSpotifyConnection(SpotifyConnectionState.Disconnected))
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(SpotifyConnectionState.Disconnected, viewModel.uiState.value.spotifyState)
        collector.cancel()
    }

    @Test
    fun spotify_connected_isReflectedInState() = runTest(mainDispatcher) {
        val spotify = FakeSpotifyConnection(SpotifyConnectionState.Connected("Paolo"))
        val viewModel = viewModel(spotifyConnection = spotify)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = assertIs<SpotifyConnectionState.Connected>(viewModel.uiState.value.spotifyState)
        assertEquals("Paolo", state.displayName)
        collector.cancel()
    }

    @Test
    fun onSpotifyConnect_callsSpotifyConnection() = runTest(mainDispatcher) {
        val spotify = FakeSpotifyConnection()
        val viewModel = viewModel(spotifyConnection = spotify)

        viewModel.onSpotifyConnect()
        advanceUntilIdle()

        assertEquals(1, spotify.connectCallCount)
    }

    @Test
    fun onSpotifyDisconnect_callsSpotifyConnection() = runTest(mainDispatcher) {
        val spotify = FakeSpotifyConnection(SpotifyConnectionState.Connected("Paolo"))
        val viewModel = viewModel(spotifyConnection = spotify)

        viewModel.onSpotifyDisconnect()
        advanceUntilIdle()

        assertEquals(1, spotify.disconnectCallCount)
    }

    // ---- About ----

    @Test
    fun appVersion_isExposedAsGiven() = runTest(mainDispatcher) {
        val viewModel = viewModel(appVersion = "2.3")
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("2.3", viewModel.uiState.value.appVersion)
        collector.cancel()
    }
}

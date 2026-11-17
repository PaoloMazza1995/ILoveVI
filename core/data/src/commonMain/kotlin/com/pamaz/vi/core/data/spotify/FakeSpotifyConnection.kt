package com.pamaz.vi.core.data.spotify

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The only [SpotifyConnection] binding today (`DataModule.kt`) — always [SpotifyConnectionState.Unavailable],
 * `connect`/`disconnect` are no-ops. `:feature:settings` only shows the Spotify card at all when
 * `FeatureFlags.spotify` is true, so in practice this fake's state is never even read in a
 * shipped build; it exists so the Settings screen's dependency graph is complete today, with
 * step 13A swapping this binding for a real one and nothing else.
 */
class FakeSpotifyConnection : SpotifyConnection {
    private val stateFlow =
        MutableStateFlow<SpotifyConnectionState>(SpotifyConnectionState.Unavailable)
    override val state: StateFlow<SpotifyConnectionState> = stateFlow

    override suspend fun connect(): Result<Unit> = Result.success(Unit)

    override suspend fun disconnect() {
    }
}

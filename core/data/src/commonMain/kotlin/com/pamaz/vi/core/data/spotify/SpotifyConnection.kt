package com.pamaz.vi.core.data.spotify

import kotlinx.coroutines.flow.Flow

/**
 * The Settings screen's extension point for Spotify (step 13A, not implemented here — see
 * `FeatureFlags.spotify`). [Unavailable] is this step's only real state (served by
 * [FakeSpotifyConnection]): no Spotify module, library or network call exists anywhere in this
 * repo yet, by design — step 13A's job is to add a real implementation of this interface, nothing
 * about the Settings screen or this contract should need to change to do that.
 */
sealed interface SpotifyConnectionState {
    data object Unavailable : SpotifyConnectionState
    data object Disconnected : SpotifyConnectionState
    data class Connected(val displayName: String) : SpotifyConnectionState
}

interface SpotifyConnection {
    val state: Flow<SpotifyConnectionState>
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
}

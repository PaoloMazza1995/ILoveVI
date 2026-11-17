package com.pamaz.vi.feature.settings.fake

import com.pamaz.vi.core.data.spotify.SpotifyConnection
import com.pamaz.vi.core.data.spotify.SpotifyConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Unlike `:core:data`'s own `FakeSpotifyConnection` (the real DI binding, always [SpotifyConnectionState.Unavailable]), this one's state is test-settable — needed to exercise every [SpotifyConnectionState]. */
internal class FakeSpotifyConnection(
    initial: SpotifyConnectionState = SpotifyConnectionState.Unavailable,
) : SpotifyConnection {
    private val stateFlow = MutableStateFlow(initial)
    override val state: StateFlow<SpotifyConnectionState> = stateFlow

    var connectCallCount: Int = 0
    var disconnectCallCount: Int = 0

    fun setState(newState: SpotifyConnectionState) {
        stateFlow.value = newState
    }

    override suspend fun connect(): Result<Unit> {
        connectCallCount++
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        disconnectCallCount++
    }
}

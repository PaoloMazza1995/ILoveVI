package com.pamaz.vi.feature.settings.fake

import com.pamaz.vi.core.data.sync.ContentSyncer
import com.pamaz.vi.core.data.sync.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Same shape as `:feature:news`'s own test-local fake (that one is `internal` to its own module, not reusable) — for [com.pamaz.vi.feature.settings.settings.SettingsViewModel] tests. */
internal class FakeContentSyncer(initial: SyncState = SyncState.INITIAL) : ContentSyncer {
    private val state = MutableStateFlow(initial)
    override val syncState: StateFlow<SyncState> = state

    var syncCallCount: Int = 0

    fun setState(newState: SyncState) {
        state.value = newState
    }

    override suspend fun sync() {
        syncCallCount++
    }

    override fun start(scope: CoroutineScope) = Unit
}

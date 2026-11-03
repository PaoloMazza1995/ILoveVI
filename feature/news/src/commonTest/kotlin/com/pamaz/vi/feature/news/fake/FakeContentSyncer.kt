package com.pamaz.vi.feature.news.fake

import com.pamaz.vi.core.data.sync.ContentSyncer
import com.pamaz.vi.core.data.sync.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A tiny in-memory stand-in for the real [ContentSyncer], for [com.pamaz.vi.feature.news.news.NewsViewModel] tests. [onSync] lets a test observe/react to a manual-refresh call. */
internal class FakeContentSyncer(initial: SyncState = SyncState.INITIAL) : ContentSyncer {
    private val state = MutableStateFlow(initial)
    override val syncState: StateFlow<SyncState> = state

    var syncCallCount: Int = 0
    var onSync: (() -> Unit)? = null

    fun setState(newState: SyncState) {
        state.value = newState
    }

    override suspend fun sync() {
        syncCallCount++
        onSync?.invoke()
    }

    override fun start(scope: CoroutineScope) = Unit
}

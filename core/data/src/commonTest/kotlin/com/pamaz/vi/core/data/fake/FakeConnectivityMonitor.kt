package com.pamaz.vi.core.data.fake

import com.pamaz.vi.core.data.connectivity.ConnectivityMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeConnectivityMonitor(initiallyOnline: Boolean = true) : ConnectivityMonitor {
    private val state = MutableStateFlow(initiallyOnline)
    override val isOnline: Flow<Boolean> = state

    fun setOnline(online: Boolean) {
        state.value = online
    }
}

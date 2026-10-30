package com.pamaz.vi.core.data.connectivity

import com.pamaz.vi.core.database.PlatformContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.net.NetworkInterface

private const val POLL_INTERVAL_MILLIS = 5_000L

/**
 * No desktop JVM has one universal "connectivity changed" OS callback the way Android/iOS do, so
 * this polls [NetworkInterface] for any interface that's up and not loopback/virtual — a
 * best-effort heuristic (an interface being "up" doesn't guarantee actual internet reachability),
 * reasonable here since no real desktop build of this app exists yet; `:core:database`'s own
 * `DatabaseDriverFactory` JVM actual is the only other platform code in this app with a comparably
 * low bar.
 */
actual class PlatformConnectivityMonitor actual constructor(
    private val context: PlatformContext,
) : ConnectivityMonitor {

    actual override val isOnline: Flow<Boolean> = flow {
        while (true) {
            emit(hasActiveNetworkInterface())
            delay(POLL_INTERVAL_MILLIS)
        }
    }.distinctUntilChanged()

    private fun hasActiveNetworkInterface(): Boolean = try {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .any { it.isUp && !it.isLoopback && !it.isVirtual }
    } catch (e: Exception) {
        false
    }
}

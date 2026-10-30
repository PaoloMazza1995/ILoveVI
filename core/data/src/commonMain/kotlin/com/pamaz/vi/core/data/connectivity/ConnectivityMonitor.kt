package com.pamaz.vi.core.data.connectivity

import com.pamaz.vi.core.database.PlatformContext
import kotlinx.coroutines.flow.Flow

/**
 * Whether the device currently has a usable network connection — [ContentSyncer][com.pamaz.vi.core.data.sync.ContentSyncer]
 * watches this to trigger a sync when connectivity returns. [isOnline] should emit its current
 * value immediately to a new collector, then again on every change.
 */
interface ConnectivityMonitor {
    val isOnline: Flow<Boolean>
}

/**
 * One real [ConnectivityMonitor] per platform (Android/iOS/JVM) — same `expect`/`actual` factory
 * shape as `:core:database`'s `DatabaseDriverFactory`, reusing its [PlatformContext] rather than
 * inventing a second "app context" abstraction (Android's actual needs a real `android.content.Context`
 * to ask the OS for connectivity; iOS/JVM ignore it, same as `DatabaseDriverFactory`'s own actuals).
 */
expect class PlatformConnectivityMonitor(context: PlatformContext) : ConnectivityMonitor {
    override val isOnline: Flow<Boolean>
}

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.pamaz.vi.core.mapview.tile

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidReceiveMemoryWarningNotification

internal actual fun observeLowMemorySignal(
    platformContext: coil3.PlatformContext,
    onLowMemory: () -> Unit,
): () -> Unit {
    val center = NSNotificationCenter.defaultCenter
    val observer = center.addObserverForName(
        name = UIApplicationDidReceiveMemoryWarningNotification,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { _ -> onLowMemory() }
    return { center.removeObserver(observer) }
}

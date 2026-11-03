package com.pamaz.vi.core.mapview.tile

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration

internal actual fun observeLowMemorySignal(
    platformContext: coil3.PlatformContext,
    onLowMemory: () -> Unit,
): () -> Unit {
    val appContext: Context = platformContext.applicationContext
    val callbacks = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) onLowMemory()
        }

        override fun onConfigurationChanged(newConfig: Configuration) = Unit

        @Deprecated("Deprecated in ComponentCallbacks2, still called on API < 34")
        override fun onLowMemory() = onLowMemory.invoke()
    }
    appContext.registerComponentCallbacks(callbacks)
    return { appContext.unregisterComponentCallbacks(callbacks) }
}

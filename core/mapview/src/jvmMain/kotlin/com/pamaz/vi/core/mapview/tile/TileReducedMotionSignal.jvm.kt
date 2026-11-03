package com.pamaz.vi.core.mapview.tile

/** Desktop/JVM has no standard cross-platform "reduce motion" OS setting to read, so this is never reduced. */
internal actual fun isReducedMotionEnabled(platformContext: coil3.PlatformContext): Boolean = false

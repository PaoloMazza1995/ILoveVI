package com.pamaz.vi.core.mapview.tile

import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

internal actual fun isReducedMotionEnabled(platformContext: coil3.PlatformContext): Boolean =
    UIAccessibilityIsReduceMotionEnabled()

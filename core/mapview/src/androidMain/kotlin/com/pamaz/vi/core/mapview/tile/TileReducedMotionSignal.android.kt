package com.pamaz.vi.core.mapview.tile

import android.provider.Settings

/** Android's accessibility "Remove animations" setting scales every system/app animation duration to 0 — the closest equivalent to iOS's dedicated "Reduce Motion" toggle. */
internal actual fun isReducedMotionEnabled(platformContext: coil3.PlatformContext): Boolean {
    val resolver = platformContext.applicationContext.contentResolver
    val durationScale =
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    return durationScale == 0f
}

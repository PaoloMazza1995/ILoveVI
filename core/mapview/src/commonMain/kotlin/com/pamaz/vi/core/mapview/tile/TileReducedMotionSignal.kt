package com.pamaz.vi.core.mapview.tile

/**
 * Whether the system's "reduce motion" / "remove animations" accessibility setting is on. Read
 * once, when [TileEngine] is created ([rememberTileEngine]) — not on every frame: this is a
 * slow-changing, user-level accessibility preference, not something that needs live observation
 * at pinch-gesture speed (unlike [observeLowMemorySignal], which genuinely can fire mid-session).
 * [TileLayer]'s crossfade reads [TileEngine.reducedMotionEnabled] and, when `true`, skips straight
 * to the real tile instead of animating — task item 3's "respect the system setting for reduced
 * motion".
 */
internal expect fun isReducedMotionEnabled(platformContext: coil3.PlatformContext): Boolean

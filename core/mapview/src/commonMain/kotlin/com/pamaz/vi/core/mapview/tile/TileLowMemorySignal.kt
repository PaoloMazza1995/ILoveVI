package com.pamaz.vi.core.mapview.tile

/**
 * Registers [onLowMemory] with the platform's own low-memory signal (Android `onTrimMemory`/
 * `onLowMemory`, iOS `UIApplication.didReceiveMemoryWarningNotification`) so [TileEngine] can
 * respond by clearing its unpinned decoded-bitmap cache before the OS has to intervene more
 * drastically — task item 10's mobile memory-safety requirement. Returns a disposer; the caller
 * must invoke it when the observing scope ends (a `DisposableEffect.onDispose`), or the
 * registration leaks for the app's lifetime.
 *
 * Desktop/JVM has no equivalent OS-level signal exposed in any standard cross-platform way, so
 * that actual is a no-op that still returns a valid (no-op) disposer — heap pressure on desktop is
 * a different, much less constrained problem than it is on a phone, per the task's own framing
 * ("never crash with out-of-memory while zooming quickly" is specifically a *mobile* requirement).
 */
internal expect fun observeLowMemorySignal(
    platformContext: coil3.PlatformContext,
    onLowMemory: () -> Unit,
): () -> Unit

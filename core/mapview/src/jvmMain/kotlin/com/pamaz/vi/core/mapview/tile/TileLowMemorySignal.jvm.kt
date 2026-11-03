package com.pamaz.vi.core.mapview.tile

internal actual fun observeLowMemorySignal(
    platformContext: coil3.PlatformContext,
    onLowMemory: () -> Unit,
): () -> Unit = {}

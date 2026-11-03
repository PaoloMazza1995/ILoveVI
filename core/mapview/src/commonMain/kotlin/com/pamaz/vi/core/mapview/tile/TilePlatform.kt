package com.pamaz.vi.core.mapview.tile

/** Picks [TileCacheConfig]'s mobile vs. desktop memory/disk budgets — `true` only for the JVM/desktop target. */
internal expect fun isDesktopTileCacheTarget(): Boolean

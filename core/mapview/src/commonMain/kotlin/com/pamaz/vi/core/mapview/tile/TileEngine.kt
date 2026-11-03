package com.pamaz.vi.core.mapview.tile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import coil3.compose.LocalPlatformContext
import com.pamaz.vi.core.model.map.TileScheme
import io.ktor.client.HttpClient

/**
 * Everything [TileLayer] needs for one `(mapVersion, tileUrlTemplate)` pair: the pinned/LRU
 * decoded-bitmap cache, the fetch/dedup/priority/retry coordinator, and the hysteresis-aware zoom
 * selector — bundled so [rememberTileEngine] has exactly one thing to `remember`/dispose, and so a
 * test can construct one directly from fakes without going through Compose at all.
 */
@Stable
internal class TileEngine(
    val mapVersion: Int,
    val memoryCache: TileMemoryCache,
    val coordinator: TileLoadCoordinator,
    val metrics: TileCacheMetrics,
    val zoomSelector: TileZoomSelector,
    /** See [isReducedMotionEnabled]'s KDoc. Read once at engine creation, not per-frame. */
    val reducedMotionEnabled: Boolean,
    private val httpClient: HttpClient,
) {
    /**
     * Pins zoom levels `nativeMinZoom..min(nativeMaxZoom, PINNED_MAX_ZOOM)` (capped at
     * [TileCacheConfig.PINNED_MAX_TILE_COUNT] tiles total) against eviction and immediately
     * requests whichever of them aren't already decoded — "the app already downloads the lowest
     * levels at first launch for offline use" (task item 2's last bullet), reused here as the
     * pinned-levels warmup rather than a second implementation.
     */
    fun preloadPinnedLevels(
        scheme: TileScheme,
        nativeMinZoom: Int,
        nativeMaxZoom: Int,
        nowMillis: Long,
    ) {
        val pinnedZoomCeiling = minOf(nativeMaxZoom, TileCacheConfig.PINNED_MAX_ZOOM)
        val keys = buildSet {
            for (zoom in nativeMinZoom..pinnedZoomCeiling) {
                for (tileY in 0 until scheme.tilesHigh(zoom)) {
                    for (tileX in 0 until scheme.tilesWide(zoom)) {
                        if (size >= TileCacheConfig.PINNED_MAX_TILE_COUNT) return@buildSet
                        add(TileKey(mapVersion, zoom, tileX, tileY))
                    }
                }
            }
        }
        memoryCache.setPinnedKeys(keys)
        for (key in keys) {
            coordinator.request(
                key.zoom,
                key.tileX,
                key.tileY,
                TileRequestPriority.VIEWPORT,
                nowMillis
            )
        }
    }

    fun close() {
        httpClient.close()
    }
}

/**
 * Builds and remembers one [TileEngine], scoped to [mapVersion]/[tileUrlTemplate] — a new map
 * version (or a tile address pointing somewhere else entirely) gets a fresh cache/coordinator/
 * [HttpClient] rather than reusing one with stale URLs or a mixed-version memory cache.
 * [coil3.PlatformContext] (already a dependency of this module, obtained the same way
 * `feature:map`'s now-removed `PrefetchLowestZoomTiles` used to) resolves the per-platform disk
 * cache directory; see [defaultTileDiskCacheDirectory]'s KDoc.
 */
@Composable
internal fun rememberTileEngine(
    mapVersion: Int,
    tileUrlTemplate: String,
    tileSizePx: Int,
): TileEngine {
    val platformContext = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    val engine = remember(mapVersion, tileUrlTemplate, tileSizePx) {
        val isDesktop = isDesktopTileCacheTarget()
        val memoryBytes =
            if (isDesktop) TileCacheConfig.MEMORY_CACHE_BYTES_DESKTOP else TileCacheConfig.MEMORY_CACHE_BYTES_MOBILE
        val diskBytes =
            if (isDesktop) TileCacheConfig.DISK_CACHE_BYTES_DESKTOP else TileCacheConfig.DISK_CACHE_BYTES_MOBILE

        val memoryCache = TileMemoryCache(memoryBytes)
        val diskFiles = createTileDiskFiles(defaultTileDiskCacheDirectory(platformContext))
        val diskCache = TileDiskCache(diskFiles, diskBytes)
        val httpClient = HttpClient()
        val fetcher = HttpTileFetcher(httpClient, diskCache)
        val metrics = TileCacheMetrics()
        val coordinator = TileLoadCoordinator(
            scope = scope,
            memoryCache = memoryCache,
            fetcher = fetcher,
            metrics = metrics,
            tileUrlTemplate = tileUrlTemplate,
            mapVersion = mapVersion,
            tileSizePx = tileSizePx,
        )
        val reducedMotionEnabled = isReducedMotionEnabled(platformContext)
        TileEngine(
            mapVersion,
            memoryCache,
            coordinator,
            metrics,
            TileZoomSelector(),
            reducedMotionEnabled,
            httpClient
        )
    }
    DisposableEffect(engine) {
        onDispose { engine.close() }
    }
    // Mobile memory safety (task item 10): on a low-memory signal, drop everything except the
    // pinned overview levels rather than let the OS kill the process while zooming quickly piles
    // up decoded bitmaps faster than normal LRU eviction would otherwise catch up with.
    DisposableEffect(engine, platformContext) {
        val dispose = observeLowMemorySignal(platformContext) { engine.memoryCache.clearUnpinned() }
        onDispose { dispose() }
    }
    return engine
}

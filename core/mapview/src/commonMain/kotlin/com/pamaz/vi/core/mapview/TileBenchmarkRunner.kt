package com.pamaz.vi.core.mapview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import coil3.compose.LocalPlatformContext
import com.pamaz.vi.core.mapview.tile.HttpTileFetcher
import com.pamaz.vi.core.mapview.tile.TileCacheConfig
import com.pamaz.vi.core.mapview.tile.TileCacheMetrics
import com.pamaz.vi.core.mapview.tile.TileDiskCache
import com.pamaz.vi.core.mapview.tile.TileLoadCoordinator
import com.pamaz.vi.core.mapview.tile.TileMemoryCache
import com.pamaz.vi.core.mapview.tile.TileZoomSelector
import com.pamaz.vi.core.mapview.tile.createTileDiskFiles
import com.pamaz.vi.core.mapview.tile.defaultTileDiskCacheDirectory
import com.pamaz.vi.core.mapview.tile.isDesktopTileCacheTarget
import com.pamaz.vi.core.mapview.tile.runTileZoomSweepBenchmark
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.map.TileScheme
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * TESTS section's "scripted benchmark in the editor (debug only)", exposed as a [Composable]
 * trigger so a caller never has to touch [coil3.PlatformContext]/[CoroutineScope] plumbing itself
 * — the same "hide Coil from callers" convention [rememberTileEngine] already established. Call
 * the returned function with a loaded [MapConfig] to kick off one zoom-sweep benchmark run in the
 * background; [onResult] receives the formatted report (or an error message) once it finishes. A
 * no-op while a previous run from this same trigger is still in flight, so callers don't need
 * their own "is it already running" guard. `:tools:map-editor`'s hidden benchmark shortcut is the
 * one real caller; `:feature:map` never offers this in the shipped app (same release-gating
 * expectation as [MapViewBuildConfig.debugOverlaySupported]/[TileDebugOverlay] — this function
 * itself doesn't check that flag, since unlike the overlay it's never composed unconditionally;
 * the caller only wires the shortcut that reaches it behind its own debug-only gate).
 */
@Composable
fun rememberTileBenchmarkTrigger(
    onResult: (String) -> Unit,
): (config: MapConfig, viewportWidthPx: Float, viewportHeightPx: Float) -> Unit {
    val platformContext = LocalPlatformContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    val latestOnResult by rememberUpdatedState(onResult)

    return remember(platformContext, scope) {
        { config: MapConfig, viewportWidthPx: Float, viewportHeightPx: Float ->
            if (!running) {
                running = true
                scope.launch {
                    val report = try {
                        runTileBenchmarkReport(
                            platformContext,
                            scope,
                            config,
                            viewportWidthPx,
                            viewportHeightPx
                        )
                    } catch (e: Exception) {
                        "Tile benchmark failed: ${e.message}"
                    }
                    running = false
                    latestOnResult(report)
                }
            }
        }
    }
}

/**
 * Deliberately builds its own, fresh [com.pamaz.vi.core.mapview.tile.TileEngine] for [config]
 * rather than reusing whatever the on-screen [MapViewer] happens to be using: a benchmark should
 * be self-contained and repeatable, not dependent on how warm the live session's cache already
 * happens to be — [runTileZoomSweepBenchmark] already runs the sweep twice internally
 * specifically to measure a cold vs. warm pass against each other.
 */
private suspend fun runTileBenchmarkReport(
    platformContext: coil3.PlatformContext,
    scope: CoroutineScope,
    config: MapConfig,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
): String {
    val scheme = TileScheme(config.widthPx, config.heightPx)
    val nativeMaxZoom = minOf(config.maxZoom, scheme.maxZoom)
    val nativeMinZoom = config.minZoom

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
        tileUrlTemplate = config.tileUrlTemplate,
        mapVersion = config.mapVersion,
        tileSizePx = scheme.tileSize,
    )
    val engine = com.pamaz.vi.core.mapview.tile.TileEngine(
        mapVersion = config.mapVersion,
        memoryCache = memoryCache,
        coordinator = coordinator,
        metrics = metrics,
        zoomSelector = TileZoomSelector(),
        reducedMotionEnabled = false,
        httpClient = httpClient,
    )

    return try {
        val result = runTileZoomSweepBenchmark(
            engine = engine,
            scheme = scheme,
            nativeMinZoom = nativeMinZoom,
            nativeMaxZoom = nativeMaxZoom,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
        )
        buildString {
            appendLine("Tile benchmark — map v${config.mapVersion}, zoom $nativeMinZoom-$nativeMaxZoom:")
            appendLine("  total requests:        ${result.totalRequests}")
            appendLine("  duplicate net fetches:  ${result.duplicateNetworkFetches} (target 0)")
            appendLine("  2nd-pass cache hit rate: ${(result.secondPassCacheHitRate * 100).roundToInt()}% (target >95%)")
            appendLine("  requests/sec:           ${(result.requestsPerSecond * 10).roundToInt() / 10.0}")
            appendLine("  frame time p50/p95 ms:  ${result.frameTimeP50Ms} / ${result.frameTimeP95Ms}")
        }
    } finally {
        engine.close()
    }
}

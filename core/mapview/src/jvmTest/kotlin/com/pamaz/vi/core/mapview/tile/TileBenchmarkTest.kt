package com.pamaz.vi.core.mapview.tile

import androidx.compose.ui.graphics.ImageBitmap
import com.pamaz.vi.core.model.map.TileScheme
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [runTileZoomSweepBenchmark]'s own measurement logic (duplicate counting, second-pass hit rate),
 * against a fake [TileFetcher] that always succeeds immediately — no real network, no real delay
 * of consequence. Runs on a real [CoroutineScope] (not `TestScope`/`backgroundScope`) deliberately:
 * [TileLoadCoordinator]'s processing loop is rooted wherever `scope` points, and this benchmark's
 * own `delay(stepDelayMs)` calls need that loop to actually keep up in real time — see
 * [TileLoadCoordinatorTest]'s own KDoc for the `backgroundScope`/virtual-time quirk this sidesteps
 * entirely by not using virtual time here at all.
 */
class TileBenchmarkTest {

    private fun instantSuccessFetcher() = object : TileFetcher {
        override suspend fun fetch(url: String, tileSizePx: Int): TileFetchResult =
            TileFetchResult.Success(
                ImageBitmap(1, 1),
                byteSize = 10,
                source = TileDataSource.NETWORK
            )
    }

    private fun newEngine(scope: CoroutineScope, fetcher: TileFetcher): TileEngine {
        val metrics = TileCacheMetrics()
        val memoryCache = TileMemoryCache(maxBytes = 10_000_000)
        val coordinator = TileLoadCoordinator(
            scope = scope,
            memoryCache = memoryCache,
            fetcher = fetcher,
            metrics = metrics,
            tileUrlTemplate = "https://example.com/{z}/{x}/{y}.webp",
            mapVersion = 1,
            tileSizePx = 256,
            maxParallelRequests = 8,
        )
        return TileEngine(
            mapVersion = 1,
            memoryCache = memoryCache,
            coordinator = coordinator,
            metrics = metrics,
            zoomSelector = TileZoomSelector(),
            reducedMotionEnabled = false,
            httpClient = HttpClient(),
        )
    }

    @Test
    fun zeroDuplicateNetworkFetches_whenEveryTileIsRequestedOnlyOncePerPass() = runBlocking {
        val scope = CoroutineScope(Job())
        val engine = newEngine(scope, instantSuccessFetcher())
        val scheme = TileScheme(widthPx = 1024, heightPx = 1024) // maxZoom = 2

        val result = runTileZoomSweepBenchmark(
            engine = engine,
            scheme = scheme,
            nativeMinZoom = 0,
            nativeMaxZoom = 2,
            viewportWidthPx = 400f,
            viewportHeightPx = 400f,
            stepsPerLevel = 1,
            stepDelayMs = 5L,
        )

        assertEquals(
            0,
            result.duplicateNetworkFetches,
            "no tile should ever be fetched twice over the network"
        )
        scope.cancel()
    }

    @Test
    fun secondPassCacheHitRate_isAboveTheNinetyFivePercentTarget() = runBlocking {
        val scope = CoroutineScope(Job())
        val engine = newEngine(scope, instantSuccessFetcher())
        val scheme = TileScheme(widthPx = 1024, heightPx = 1024)

        val result = runTileZoomSweepBenchmark(
            engine = engine,
            scheme = scheme,
            nativeMinZoom = 0,
            nativeMaxZoom = 2,
            viewportWidthPx = 400f,
            viewportHeightPx = 400f,
            stepsPerLevel = 1,
            stepDelayMs = 5L,
        )

        assertTrue(
            result.secondPassCacheHitRate > 0.95f,
            "expected second-pass hit rate above 95%, was ${result.secondPassCacheHitRate}",
        )
        scope.cancel()
    }

    @Test
    fun reportsPositiveRequestsPerSecondAndFrameTimePercentiles() = runBlocking {
        val scope = CoroutineScope(Job())
        val engine = newEngine(scope, instantSuccessFetcher())
        val scheme = TileScheme(widthPx = 1024, heightPx = 1024)

        val result = runTileZoomSweepBenchmark(
            engine = engine,
            scheme = scheme,
            nativeMinZoom = 0,
            nativeMaxZoom = 2,
            viewportWidthPx = 400f,
            viewportHeightPx = 400f,
            stepsPerLevel = 1,
            stepDelayMs = 5L,
        )

        assertTrue(result.totalRequests > 0)
        assertTrue(result.requestsPerSecond > 0.0)
        assertTrue(result.frameTimeP95Ms >= result.frameTimeP50Ms)
        scope.cancel()
    }

    @Test
    fun buildZoomSweepSteps_goesFromMinToMaxAndBackToMin() {
        val steps = buildZoomSweepSteps(nativeMinZoom = 0, nativeMaxZoom = 2, stepsPerLevel = 2)
        assertEquals(0f, steps.first())
        assertEquals(2f, steps.max())
        assertEquals(0f, steps.last())
        // Symmetric: the up-leg and the down-leg should visit the same number of steps.
        val peakIndex = steps.indexOf(2f)
        assertEquals(peakIndex, steps.size - 1 - peakIndex)
    }
}

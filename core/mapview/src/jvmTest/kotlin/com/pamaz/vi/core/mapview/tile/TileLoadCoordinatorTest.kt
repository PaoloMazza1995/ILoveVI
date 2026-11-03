package com.pamaz.vi.core.mapview.tile

import androidx.compose.ui.graphics.ImageBitmap
import com.pamaz.vi.core.model.map.TileUrl
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [TileLoadCoordinator]'s dedup/priority/retry/stability-delay logic, against a fully
 * controllable fake [TileFetcher] instead of real network/disk I/O — exactly what that
 * interface's own KDoc says it exists for. jvmTest (not commonTest): needs a real
 * [androidx.compose.ui.graphics.ImageBitmap] (see [TileMemoryCacheTest]'s KDoc).
 *
 * Every test uses `maxParallelRequests = 1` unless stated otherwise: a single in-flight slot makes
 * the priority queue's drain order directly observable (the next fetch only starts once the
 * current one is explicitly resolved by the test), instead of racing several real workers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TileLoadCoordinatorTest {

    private val template = "https://example.com/tiles/{z}/{x}/{y}.webp"

    private fun urlFor(zoom: Int, x: Int, y: Int) = TileUrl.build(template, zoom, x, y)

    /** Suspends forever until the test explicitly [resolve]s that exact URL — lets a test control fetch completion order with no real delay. */
    private class GatedFetcher : TileFetcher {
        val calls = mutableListOf<String>()
        private val gates = HashMap<String, CompletableDeferred<TileFetchResult>>()

        override suspend fun fetch(url: String, tileSizePx: Int): TileFetchResult {
            calls += url
            return gateFor(url).await()
        }

        private fun gateFor(url: String) = gates.getOrPut(url) { CompletableDeferred() }

        fun resolve(url: String, result: TileFetchResult) {
            gateFor(url).complete(result)
        }
    }

    private fun success() =
        TileFetchResult.Success(ImageBitmap(1, 1), byteSize = 10, source = TileDataSource.NETWORK)

    /**
     * `TestScope.advanceUntilIdle()` (and even `testScheduler.advanceUntilIdle()`) does *not* drive
     * coroutines rooted in [TestScope.backgroundScope] — confirmed empirically (not documented
     * anywhere obvious): a `backgroundScope.launch { }` body never runs under either, only under a
     * plain [runCurrent]. [TileLoadCoordinator] deliberately launches its processing loop on
     * whatever `scope` it's given — on `backgroundScope` here specifically so a test's leftover
     * in-flight fetch (several tests leave one deliberately unresolved, e.g. to inspect mid-flight
     * state) doesn't fail the test with `UncompletedCoroutinesError`, matching how the coordinator
     * actually lives in production ([com.pamaz.vi.core.mapview.tile.rememberTileEngine]'s own scope
     * is never expected to "complete" either). This helper is the pragmatic workaround: repeatedly
     * interleave [runCurrent] with a bounded time advance, which *does* reach background work.
     */
    private suspend fun TestScope.pumpUntilIdle(rounds: Int = 20, stepMs: Long = 1_000L) {
        runCurrent()
        repeat(rounds) {
            advanceTimeBy(stepMs)
            runCurrent()
        }
    }

    private fun newCoordinator(
        fetcher: TileFetcher,
        memoryCache: TileMemoryCache = TileMemoryCache(maxBytes = 10_000),
        maxParallelRequests: Int = 1,
        maxRetryAttempts: Int = 3,
        retryBaseDelayMs: Long = 10L,
        viewportStabilityDelayMs: Long = 120L,
        scope: kotlinx.coroutines.CoroutineScope,
    ) = TileLoadCoordinator(
        scope = scope,
        memoryCache = memoryCache,
        fetcher = fetcher,
        metrics = TileCacheMetrics(),
        tileUrlTemplate = template,
        mapVersion = 1,
        tileSizePx = 256,
        maxParallelRequests = maxParallelRequests,
        maxRetryAttempts = maxRetryAttempts,
        retryBaseDelayMs = retryBaseDelayMs,
        viewportStabilityDelayMs = viewportStabilityDelayMs,
    )

    @Test
    fun inFlightDeduplication_100ConcurrentRequestsForTheSameTile_resultInExactlyOneFetch() =
        runTest {
            val fetcher = GatedFetcher()
            val coordinator =
                newCoordinator(fetcher, maxParallelRequests = 8, scope = backgroundScope)

            repeat(100) {
                coordinator.request(
                    zoom = 2,
                    tileX = 1,
                    tileY = 1,
                    TileRequestPriority.VIEWPORT,
                    nowMillis = 0L
                )
            }
            runCurrent()
            assertEquals(
                1,
                fetcher.calls.size,
                "100 requests for the same tile should start exactly one fetch"
            )

            fetcher.resolve(urlFor(2, 1, 1), success())
            runCurrent()
            assertEquals(
                1,
                fetcher.calls.size,
                "resolving the single fetch must not trigger any more"
            )
        }

    @Test
    fun aTileAlreadyInMemory_isNeverFetchedAgain() = runTest {
        val fetcher = GatedFetcher()
        val memoryCache = TileMemoryCache(maxBytes = 10_000)
        memoryCache.put(TileKey(1, 2, 1, 1), ImageBitmap(1, 1), bytes = 10)
        val coordinator =
            newCoordinator(fetcher, memoryCache = memoryCache, scope = backgroundScope)

        coordinator.request(
            zoom = 2,
            tileX = 1,
            tileY = 1,
            TileRequestPriority.VIEWPORT,
            nowMillis = 0L
        )
        runCurrent()

        assertEquals(0, fetcher.calls.size)
    }

    @Test
    fun viewportRequests_drainBeforeRingRequests_drainBeforeNextZoomRequests() = runTest {
        val fetcher = GatedFetcher()
        val coordinator = newCoordinator(fetcher, maxParallelRequests = 1, scope = backgroundScope)

        // Enqueued lowest-priority-first, on purpose: queue *position* must not matter, only priority.
        coordinator.request(
            zoom = 3,
            tileX = 0,
            tileY = 0,
            TileRequestPriority.NEXT_ZOOM,
            nowMillis = 0L
        )
        coordinator.request(
            zoom = 2,
            tileX = 0,
            tileY = 0,
            TileRequestPriority.PREFETCH_RING,
            nowMillis = 0L
        )
        coordinator.request(
            zoom = 1,
            tileX = 0,
            tileY = 0,
            TileRequestPriority.VIEWPORT,
            nowMillis = 0L
        )
        runCurrent()

        val nextZoomUrl = urlFor(3, 0, 0)
        val ringUrl = urlFor(2, 0, 0)
        val viewportUrl = urlFor(1, 0, 0)

        // Only the first-enqueued (NEXT_ZOOM) tile could have started before the others arrived —
        // the single parallel slot was already taken synchronously by the time VIEWPORT/RING were
        // requested, so they both had to queue behind it.
        assertEquals(listOf(nextZoomUrl), fetcher.calls)

        fetcher.resolve(nextZoomUrl, success())
        runCurrent()
        assertEquals(
            listOf(nextZoomUrl, viewportUrl), fetcher.calls,
            "VIEWPORT must drain before PREFETCH_RING even though RING was queued first",
        )

        fetcher.resolve(viewportUrl, success())
        runCurrent()
        assertEquals(listOf(nextZoomUrl, viewportUrl, ringUrl), fetcher.calls)

        fetcher.resolve(ringUrl, success())
        runCurrent()
    }

    @Test
    fun aFailedFetch_retriesWithBackoff_andSucceedsOnTheLastAttempt() = runTest {
        var attempt = 0
        val fetcher = object : TileFetcher {
            override suspend fun fetch(url: String, tileSizePx: Int): TileFetchResult {
                attempt++
                return if (attempt < 3) {
                    TileFetchResult.Failure(IllegalStateException("boom"))
                } else {
                    success()
                }
            }
        }
        val memoryCache = TileMemoryCache(maxBytes = 10_000)
        val coordinator = newCoordinator(
            fetcher,
            memoryCache = memoryCache,
            maxRetryAttempts = 3,
            scope = backgroundScope
        )

        coordinator.request(
            zoom = 1,
            tileX = 0,
            tileY = 0,
            TileRequestPriority.VIEWPORT,
            nowMillis = 0L
        )
        pumpUntilIdle()

        assertEquals(3, attempt)
        assertTrue(memoryCache.contains(TileKey(1, 1, 0, 0)))
    }

    @Test
    fun aFetchThatExhaustsEveryRetry_isMarkedFailed_andRecordsAMetric() = runTest {
        val fetcher = object : TileFetcher {
            override suspend fun fetch(url: String, tileSizePx: Int): TileFetchResult =
                TileFetchResult.Failure(IllegalStateException("always fails"))
        }
        val metrics = TileCacheMetrics()
        val coordinator = TileLoadCoordinator(
            scope = backgroundScope,
            memoryCache = TileMemoryCache(maxBytes = 10_000),
            fetcher = fetcher,
            metrics = metrics,
            tileUrlTemplate = template,
            mapVersion = 1,
            tileSizePx = 256,
            maxParallelRequests = 1,
            maxRetryAttempts = 2,
            retryBaseDelayMs = 1L,
            viewportStabilityDelayMs = 120L,
        )

        coordinator.request(
            zoom = 1,
            tileX = 0,
            tileY = 0,
            TileRequestPriority.VIEWPORT,
            nowMillis = 0L
        )
        pumpUntilIdle()

        assertEquals(1L, metrics.snapshot.value.failures)
        assertTrue(TileKey(1, 1, 0, 0) in coordinator.failedKeys.value)
    }

    @Test
    fun aFailedTile_getsAnotherChance_whenItReEntersTheViewport() = runTest {
        var callCount = 0
        val fetcher = object : TileFetcher {
            override suspend fun fetch(url: String, tileSizePx: Int): TileFetchResult {
                callCount++
                return if (callCount == 1) TileFetchResult.Failure(IllegalStateException("boom")) else success()
            }
        }
        val memoryCache = TileMemoryCache(maxBytes = 10_000)
        val coordinator = newCoordinator(
            fetcher,
            memoryCache = memoryCache,
            maxRetryAttempts = 1,
            scope = backgroundScope
        )

        coordinator.request(
            zoom = 1,
            tileX = 0,
            tileY = 0,
            TileRequestPriority.VIEWPORT,
            nowMillis = 0L
        )
        pumpUntilIdle()
        assertTrue(TileKey(1, 1, 0, 0) in coordinator.failedKeys.value)

        // Back in view later — a VIEWPORT request always gets another chance (unlike RING/NEXT_ZOOM).
        coordinator.request(
            zoom = 1,
            tileX = 0,
            tileY = 0,
            TileRequestPriority.VIEWPORT,
            nowMillis = 1_000L
        )
        pumpUntilIdle()

        assertEquals(2, callCount)
        assertTrue(memoryCache.contains(TileKey(1, 1, 0, 0)))
    }

    @Test
    fun aNonViewportRequest_forAnAlreadyFailedTile_isSkipped() = runTest {
        var callCount = 0
        val fetcher = object : TileFetcher {
            override suspend fun fetch(url: String, tileSizePx: Int): TileFetchResult {
                callCount++
                return TileFetchResult.Failure(IllegalStateException("boom"))
            }
        }
        val coordinator = newCoordinator(fetcher, maxRetryAttempts = 1, scope = backgroundScope)

        coordinator.request(
            zoom = 1,
            tileX = 0,
            tileY = 0,
            TileRequestPriority.VIEWPORT,
            nowMillis = 0L
        )
        pumpUntilIdle()
        assertEquals(1, callCount)

        coordinator.request(
            zoom = 1,
            tileX = 0,
            tileY = 0,
            TileRequestPriority.PREFETCH_RING,
            nowMillis = 1_000L
        )
        pumpUntilIdle()
        assertEquals(1, callCount, "a RING request for an already-failed tile should not retry it")
    }

    @Test
    fun aTileNoLongerWanted_afterTheStabilityDelay_isCancelled() = runTest {
        val fetcher = GatedFetcher()
        val coordinator = newCoordinator(
            fetcher,
            maxParallelRequests = 8,
            viewportStabilityDelayMs = 120L,
            scope = backgroundScope
        )

        coordinator.request(
            zoom = 1,
            tileX = 0,
            tileY = 0,
            TileRequestPriority.VIEWPORT,
            nowMillis = 0L
        )
        runCurrent()
        assertTrue(TileKey(1, 1, 0, 0) in coordinator.loadingKeys.value)

        // Nothing re-requests this tile afterward (it scrolled out of view) — after the stability
        // delay it should be swept away rather than left running forever.
        pumpUntilIdle()
        assertTrue(
            TileKey(1, 1, 0, 0) !in coordinator.loadingKeys.value,
            "stale in-flight tile should have been cancelled"
        )
    }

    @Test
    fun aTileRequestedEveryFrame_isNeverCancelled_evenPastTheStabilityWindow() = runTest {
        val fetcher = GatedFetcher()
        val coordinator = newCoordinator(
            fetcher,
            maxParallelRequests = 8,
            viewportStabilityDelayMs = 120L,
            scope = backgroundScope
        )

        var now = 0L
        repeat(5) {
            coordinator.request(
                zoom = 1,
                tileX = 0,
                tileY = 0,
                TileRequestPriority.VIEWPORT,
                nowMillis = now
            )
            runCurrent()
            now += 50L
            advanceTimeBy(50L)
            runCurrent()
        }

        assertTrue(
            TileKey(1, 1, 0, 0) in coordinator.loadingKeys.value,
            "a tile still being asked for every frame must never be swept as stale",
        )
    }

    @Test
    fun arrivedAtMillis_isNull_untilTheFetchActuallyCompletes() = runTest {
        val fetcher = GatedFetcher()
        val coordinator = newCoordinator(fetcher, scope = backgroundScope)
        val key = TileKey(1, 1, 0, 0)

        coordinator.request(
            zoom = 1,
            tileX = 0,
            tileY = 0,
            TileRequestPriority.VIEWPORT,
            nowMillis = 0L
        )
        runCurrent()
        assertNull(coordinator.arrivedAtMillis(key))

        fetcher.resolve(urlFor(1, 0, 0), success())
        runCurrent()
        assertTrue(coordinator.arrivedAtMillis(key) != null)
    }
}

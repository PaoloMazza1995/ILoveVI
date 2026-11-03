package com.pamaz.vi.core.mapview.tile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.Instant

/** One snapshot of [TileCacheMetrics] — what the debug overlay (`TileDebugOverlay.kt`) renders. */
data class TileCacheMetricsSnapshot(
    val memoryHits: Long = 0L,
    val diskHits: Long = 0L,
    val networkFetches: Long = 0L,
    val failures: Long = 0L,
    val requestsLastSecond: Int = 0,
) {
    val totalRequests: Long get() = memoryHits + diskHits + networkFetches + failures

    /** Fraction of completed requests (excludes in-flight/failed) served without hitting the network. */
    val cacheHitRate: Float
        get() {
            val served = memoryHits + diskHits + networkFetches
            return if (served == 0L) 0f else (memoryHits + diskHits).toFloat() / served
        }
}

/**
 * Counters feeding the debug overlay (MAP-10-style "report measured numbers", applied here to the
 * tile pipeline instead of clustering) — a memory hit is [TileMemoryCache.peek] succeeding before
 * ever asking [TileFetcher]; a disk hit is [coil3.ImageResult]'s own `DataSource.DISK`; a network
 * fetch is `DataSource.NETWORK`. [requestsLastSecond] is a simple rolling window, not a precise
 * histogram — good enough for an on-screen debug number, not a claim of profiler-grade accuracy.
 */
internal class TileCacheMetrics {
    private val state = MutableStateFlow(TileCacheMetricsSnapshot())
    val snapshot = state.asStateFlow()

    private val recentRequestTimestamps = ArrayDeque<Instant>()

    fun recordMemoryHit() =
        state.update { it.copy(memoryHits = it.memoryHits + 1) }.also { markRequest() }

    fun recordDiskHit() =
        state.update { it.copy(diskHits = it.diskHits + 1) }.also { markRequest() }

    fun recordNetworkFetch() =
        state.update { it.copy(networkFetches = it.networkFetches + 1) }.also { markRequest() }

    fun recordFailure() =
        state.update { it.copy(failures = it.failures + 1) }.also { markRequest() }

    private fun markRequest() {
        val now = Clock.System.now()
        recentRequestTimestamps.addLast(now)
        while (recentRequestTimestamps.isNotEmpty() && now - recentRequestTimestamps.first() > ONE_SECOND) {
            recentRequestTimestamps.removeFirst()
        }
        state.update { it.copy(requestsLastSecond = recentRequestTimestamps.size) }
    }

    fun reset() {
        state.update { TileCacheMetricsSnapshot() }
        recentRequestTimestamps.clear()
    }

    private companion object {
        val ONE_SECOND = kotlin.time.Duration.parse("1s")
    }
}

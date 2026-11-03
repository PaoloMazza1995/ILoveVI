package com.pamaz.vi.core.mapview.tile

import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.core.model.map.visibleTiles
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.measureTime

/**
 * The TESTS section's "scripted benchmark in the editor (debug only)": simulates a zoom from
 * [nativeMinZoom] to [nativeMaxZoom] and back at a fixed step rate over the real, already-loaded
 * [engine], twice in a row — the second pass should be served almost entirely from cache. The one
 * real caller is `:tools:map-editor`'s hidden benchmark shortcut (see that module's README); it
 * prints [TileBenchmarkResult] to the console. Lives here (not in the editor module) so it can
 * also be unit-tested against a fake [TileFetcher] with no real network/UI involved.
 *
 * **What the frame-time numbers do and don't prove** — same honest caveat
 * [com.pamaz.vi.core.mapview.MapViewerPerfTest]'s own KDoc gives: [frameTimeP50Ms]/[frameTimeP95Ms]
 * measure the pure-Kotlin cost of one simulated frame's tile-selection-and-request work
 * ([TileScheme.visibleTiles] plus issuing [TileLoadCoordinator.request] for every visible tile),
 * not an actual rendered Compose frame — there is no display or real gesture input in this
 * environment (desktop editor or otherwise) to measure that from.
 */
internal data class TileBenchmarkResult(
    /** Every [TileLoadCoordinator.request] call made across both passes — includes cache hits (not just the tiles that triggered a real fetch). */
    val totalRequests: Int,
    /**
     * How many times the *same* tile key was fetched over the network more than once across the
     * whole run — the task's "duplicate requests (must be 0)" metric. Computed from
     * [TileCacheMetrics.networkFetches] minus the number of *distinct* keys that were ever handed
     * to [TileLoadCoordinator.request]: every real network fetch should be for a key never fetched
     * before (in-flight de-duplication plus "don't re-fetch what's already cached" together
     * guarantee this), so any excess is a genuine duplicate.
     */
    val duplicateNetworkFetches: Int,
    /** [TileCacheMetricsSnapshot.cacheHitRate], computed only over the *second* pass's own requests — target: above 0.95 per the task's acceptance. */
    val secondPassCacheHitRate: Float,
    val requestsPerSecond: Double,
    val frameTimeP50Ms: Double,
    val frameTimeP95Ms: Double,
)

/**
 * `0 -> nativeMaxZoom -> 0`, in [stepsPerLevel] increments per integer zoom level — "a zoom from
 * level 0 to the maximum and back at fixed speed", per the task.
 */
internal fun buildZoomSweepSteps(
    nativeMinZoom: Int,
    nativeMaxZoom: Int,
    stepsPerLevel: Int
): List<Float> {
    require(stepsPerLevel >= 1) { "stepsPerLevel must be at least 1, was $stepsPerLevel" }
    val levels = nativeMaxZoom - nativeMinZoom
    val up = buildList {
        for (step in 0..(levels * stepsPerLevel)) {
            add(nativeMinZoom + step.toFloat() / stepsPerLevel)
        }
    }
    val down = up.dropLast(1).reversed()
    return up + down
}

internal suspend fun runTileZoomSweepBenchmark(
    engine: TileEngine,
    scheme: TileScheme,
    nativeMinZoom: Int,
    nativeMaxZoom: Int,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    center: NormalizedPoint = NormalizedPoint(0.5f, 0.5f),
    stepsPerLevel: Int = 10,
    /** Simulates "fixed speed": a real settle delay between steps so in-flight fetches from one step have a chance to land before the next, same as a real (slow) pinch/pan. */
    stepDelayMs: Long = 50L,
): TileBenchmarkResult {
    val frameTimesMs = mutableListOf<Double>()
    val requestedKeys = HashSet<TileKey>()
    var totalRequests = 0

    suspend fun onePass() {
        val steps = buildZoomSweepSteps(nativeMinZoom, nativeMaxZoom, stepsPerLevel)
        for (zoom in steps) {
            val renderLevel = zoom.roundToInt().coerceIn(nativeMinZoom, nativeMaxZoom)
            val elapsed = measureTime {
                val tileRange = scheme.visibleTiles(
                    center,
                    renderLevel.toFloat(),
                    viewportWidthPx,
                    viewportHeightPx
                )
                val nowMillis = Clock.System.now().toEpochMilliseconds()
                for (tile in tileRange.tiles) {
                    requestedKeys += TileKey(engine.mapVersion, renderLevel, tile.tileX, tile.tileY)
                    engine.coordinator.request(
                        renderLevel,
                        tile.tileX,
                        tile.tileY,
                        TileRequestPriority.VIEWPORT,
                        nowMillis
                    )
                    totalRequests++
                }
            }
            frameTimesMs += elapsed.inWholeMicroseconds / 1000.0
            delay(stepDelayMs)
        }
    }

    val beforeFirstPass = engine.metrics.snapshot.value
    val wallClockStart = Clock.System.now()
    onePass()
    val afterFirstPass = engine.metrics.snapshot.value
    onePass()
    val afterSecondPass = engine.metrics.snapshot.value
    val wallClockElapsedSeconds =
        (Clock.System.now() - wallClockStart).inWholeMilliseconds / 1000.0

    val secondPassMemoryHits = afterSecondPass.memoryHits - afterFirstPass.memoryHits
    val secondPassDiskHits = afterSecondPass.diskHits - afterFirstPass.diskHits
    val secondPassNetworkFetches = afterSecondPass.networkFetches - afterFirstPass.networkFetches
    val secondPassServed = secondPassMemoryHits + secondPassDiskHits + secondPassNetworkFetches
    val secondPassHitRate = if (secondPassServed == 0L) {
        1f
    } else {
        (secondPassMemoryHits + secondPassDiskHits).toFloat() / secondPassServed
    }

    val totalNetworkFetches = afterSecondPass.networkFetches - beforeFirstPass.networkFetches
    val duplicateNetworkFetches =
        (totalNetworkFetches - requestedKeys.size).coerceAtLeast(0).toInt()

    val sortedFrameTimes = frameTimesMs.sorted()
    fun percentile(p: Double): Double {
        if (sortedFrameTimes.isEmpty()) return 0.0
        val index =
            ((p * sortedFrameTimes.size).roundToInt()).coerceIn(0, sortedFrameTimes.size - 1)
        return sortedFrameTimes[index]
    }

    return TileBenchmarkResult(
        totalRequests = totalRequests,
        duplicateNetworkFetches = duplicateNetworkFetches,
        secondPassCacheHitRate = secondPassHitRate,
        requestsPerSecond = if (wallClockElapsedSeconds > 0) totalRequests / wallClockElapsedSeconds else 0.0,
        frameTimeP50Ms = percentile(0.50),
        frameTimeP95Ms = percentile(0.95),
    )
}

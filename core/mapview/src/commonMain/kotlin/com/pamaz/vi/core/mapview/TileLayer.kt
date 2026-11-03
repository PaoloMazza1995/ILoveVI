package com.pamaz.vi.core.mapview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pamaz.vi.core.mapview.tile.TileEngine
import com.pamaz.vi.core.mapview.tile.TileKey
import com.pamaz.vi.core.mapview.tile.TileRequestPriority
import com.pamaz.vi.core.mapview.tile.findAncestorFallback
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.core.model.map.visibleTiles
import com.pamaz.vi.design.IloveViColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Clock

/**
 * Draws every tile [TileScheme.visibleTiles] reports for the current viewport/center/zoom, with
 * fallback-to-ancestor rendering and a crossfade when the real tile arrives — this is the fix for
 * the "black/empty squares while zooming or panning" bug: see the module README's "Tile pipeline"
 * section for the full diagnosis (continuous-zoom tile-level flipping with no hysteresis, state
 * read in composition instead of the draw phase, and no fallback rendering at all were the three
 * confirmed causes).
 *
 * Everything draws in ONE [Canvas]/[androidx.compose.ui.graphics.drawscope.DrawScope] — [state]'s
 * `center`/`zoom` are read *inside* the draw lambda, never in this composable's own body, so a
 * pinch/pan/fling never recomposes this tree; only the draw phase re-runs (the same pattern
 * [SingleImageLayer] already established in this module). [TileEngine.coordinator] is asked for
 * tiles from that same draw lambda too — a plain, non-suspending function call, safe to make from
 * the draw phase; the actual fetch coroutines run on [engine]'s own remembered
 * [kotlinx.coroutines.CoroutineScope], not this draw call's stack.
 *
 * [nativeMaxZoom] is the sharpest zoom level real tiles exist for; [state.zoom][MapViewerState.zoom]
 * may run higher than that (overzoom, see [com.pamaz.vi.core.mapview.tile.TileCacheConfig
 * .OVERZOOM_LEVELS]/the module README's "Overzoom" section) — tiles are always requested/rendered
 * at `min(state.zoom, nativeMaxZoom)`, then the
 * whole canvas is scaled up by the leftover `2^(state.zoom - nativeMaxZoom)` factor around the
 * viewport center, with [FilterQuality.Medium] smoothing. [MapViewer] is the only caller.
 */
@Composable
internal fun TileLayer(
    scheme: TileScheme,
    mapVersion: Int,
    nativeMinZoom: Int,
    nativeMaxZoom: Int,
    state: MapViewerState,
    engine: TileEngine,
    modifier: Modifier = Modifier,
) {
    val viewportSize = state.viewportSize
    if (viewportSize.width <= 0 || viewportSize.height <= 0) return

    val zoomRequestState = remember { ZoomRequestDebounceState() }

    Canvas(modifier = modifier.fillMaxSize().background(IloveViColors.bg800)) {
        // Registers a draw-phase dependency on the memory cache's own content — see
        // TileMemoryCache.generation's KDoc: this is what makes a tile that finishes decoding in
        // the background actually appear on its own, instead of only "popping in" on the next
        // unrelated gesture (a real, reported bug: the map used to only ever redraw because a
        // drag/pinch happened to change state.zoom/state.center, which *are* draw-observed).
        @Suppress("UNUSED_EXPRESSION")
        engine.memoryCache.generation

        val now = Clock.System.now().toEpochMilliseconds()
        val continuousZoom = state.zoom

        val renderLevel = engine.zoomSelector.select(continuousZoom, nativeMinZoom, nativeMaxZoom)
        val requestLevel = zoomRequestState.decide(
            renderLevel = renderLevel,
            isAnimating = state.isZoomAnimating,
            nowMillis = now,
        )

        val scaleFactor = tileRenderScaleFactor(continuousZoom, renderLevel)

        scale(scaleFactor, pivot = Offset(size.width / 2f, size.height / 2f)) {
            val tileRange = scheme.visibleTiles(
                center = state.center,
                zoom = renderLevel.toFloat(),
                viewportWidthPx = size.width,
                viewportHeightPx = size.height,
            )

            val tilesWide = scheme.tilesWide(renderLevel)
            val tilesHigh = scheme.tilesHigh(renderLevel)

            for (tile in tileRange.tiles) {
                val key = TileKey(mapVersion, renderLevel, tile.tileX, tile.tileY)
                val dstOffset = IntOffset(tile.screenX.roundToInt(), tile.screenY.roundToInt())
                // +1px: each tile is drawn very slightly larger than its nominal size, so a
                // sub-pixel rounding difference between two adjacent tiles' independently-rounded
                // positions overlaps by a hair instead of leaving a visible 1px seam between them
                // — the standard fix for "tile grid lines visible" in any tiled-image renderer.
                val dstSize = IntSize(
                    tileRange.tileScreenSizePx.roundToInt().coerceAtLeast(1) + 1,
                    tileRange.tileScreenSizePx.roundToInt().coerceAtLeast(1) + 1,
                )

                // Only the debounced *request* level actually starts network/disk work (see
                // ZoomRequestDebounceState's KDoc) — fallback drawing below still runs every
                // frame at `renderLevel` regardless, using whatever is already resident.
                if (renderLevel == requestLevel) {
                    engine.coordinator.request(
                        renderLevel, tile.tileX, tile.tileY, TileRequestPriority.VIEWPORT, now,
                    )
                    requestRing(
                        engine,
                        mapVersion,
                        renderLevel,
                        tile.tileX,
                        tile.tileY,
                        tilesWide,
                        tilesHigh,
                        now
                    )
                }

                val bitmap = engine.memoryCache.peek(key)
                val alpha = if (bitmap == null) {
                    0f
                } else if (engine.reducedMotionEnabled) {
                    // Task item 3: "respect the system setting for reduced motion" — jump straight
                    // to the real tile instead of cross-fading into it.
                    1f
                } else {
                    val arrivedAt = engine.coordinator.arrivedAtMillis(key)
                    if (arrivedAt == null) {
                        // Shouldn't happen (a cached bitmap always came through a successful
                        // fetch, which always stamps arrival) — treat as "already there" rather
                        // than crash.
                        1f
                    } else {
                        val elapsed = now - arrivedAt
                        if (elapsed >= TileCrossfadeDurationMs) {
                            1f
                        } else {
                            (elapsed.toFloat() / TileCrossfadeDurationMs).coerceIn(0f, 1f)
                        }
                    }
                }

                if (alpha < 1f) {
                    drawAncestorFallback(
                        engine, mapVersion, renderLevel, tile.tileX, tile.tileY,
                        nativeMinZoom, scheme.tileSize, dstOffset, dstSize,
                    )
                }
                if (bitmap != null && alpha > 0f) {
                    drawImage(
                        image = bitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bitmap.width, bitmap.height),
                        dstOffset = dstOffset,
                        dstSize = dstSize,
                        alpha = alpha,
                        filterQuality = FilterQuality.Medium,
                    )
                }
            }

            // Next zoom level's center tile, once the gesture has settled on a level.
            if (requestLevel == renderLevel && renderLevel < nativeMaxZoom) {
                requestNextZoomCenter(engine, renderLevel, nativeMaxZoom, scheme, now)
            }
        }
    }
}

private val TileCrossfadeDurationMs =
    com.pamaz.vi.core.mapview.tile.TileCacheConfig.CROSSFADE_DURATION_MS

/**
 * The continuous scale to draw a discrete [renderLevel]'s tiles at so they visually track
 * [continuousZoom] smoothly — not just for overzoom (`continuousZoom > nativeMaxZoom`), but for
 * *any* gap between the continuous camera zoom and the discrete, hysteresis-selected tile level.
 *
 * A real, reported bug lived here: the previous formula was `2^(continuousZoom -
 * min(continuousZoom, nativeMaxZoom))`, which is exactly `1.0` for the entire native zoom range —
 * tiles stayed frozen at [renderLevel]'s fixed pixel size through most of a pinch gesture (the
 * render level only updates once hysteresis lets it flip), so the map visibly didn't scale at all
 * until that sudden jump. That's why zooming showed a static, unscaled tile grid (visible tile
 * boundaries) instead of smoothly enlarging like every other slippy map. This one formula covers
 * both "mid-pinch within native zoom" and "past the native max" (overzoom) continuously, since
 * [renderLevel] is already clamped to `nativeMinZoom..nativeMaxZoom` by [TileZoomSelector] before
 * this is ever called.
 *
 * Pure/extracted specifically so this can be unit-tested — the formula used to live inline inside
 * [TileLayer]'s `DrawScope` lambda, which no unit test (lacking a real Compose UI harness) could
 * ever exercise; see `TileRenderScaleFactorTest`.
 */
internal fun tileRenderScaleFactor(continuousZoom: Float, renderLevel: Int): Float =
    2.0.pow((continuousZoom - renderLevel).toDouble()).toFloat()

/** Draws the best available ancestor under a tile that's missing or still crossfading in — see [findAncestorFallback]'s own KDoc. A no-op (the [IloveViColors.bg800] Canvas background shows through) if no ancestor exists, which "can't happen" once the pinned low levels have loaded. */
private fun DrawScope.drawAncestorFallback(
    engine: TileEngine,
    mapVersion: Int,
    zoom: Int,
    tileX: Int,
    tileY: Int,
    nativeMinZoom: Int,
    tileSizePx: Int,
    dstOffset: IntOffset,
    dstSize: IntSize,
) {
    val fallback = findAncestorFallback(
        engine.memoryCache, mapVersion, zoom, tileX, tileY, nativeMinZoom, tileSizePx,
    ) ?: return
    drawImage(
        image = fallback.bitmap,
        srcOffset = IntOffset(fallback.cropPx.left.roundToInt(), fallback.cropPx.top.roundToInt()),
        srcSize = IntSize(
            fallback.cropPx.width.roundToInt().coerceAtLeast(1),
            fallback.cropPx.height.roundToInt().coerceAtLeast(1),
        ),
        dstOffset = dstOffset,
        dstSize = dstSize,
        filterQuality = FilterQuality.Medium,
    )
}

/**
 * Implements "tile requests for a new level start once the zoom has been stable for about 80ms,
 * or immediately for animated zooms" — [TileZoomSelector][com.pamaz.vi.core.mapview.tile.TileZoomSelector]'s
 * hysteresis already decides *which* level to render every frame (so fallback drawing is always
 * current); this class separately decides when that level's tiles are actually worth spending a
 * network/disk request on, so a fast pinch sweeping past several levels doesn't start (and then
 * immediately abandon) a fetch for every one of them.
 */
private class ZoomRequestDebounceState {
    private var lastRequestedLevel: Int? = null
    private var pendingLevel: Int? = null
    private var pendingSinceMillis: Long = 0L

    fun decide(renderLevel: Int, isAnimating: Boolean, nowMillis: Long): Int {
        val current = lastRequestedLevel
        if (current == renderLevel) return renderLevel
        if (current == null || isAnimating) {
            lastRequestedLevel = renderLevel
            pendingLevel = null
            return renderLevel
        }
        if (pendingLevel != renderLevel) {
            pendingLevel = renderLevel
            pendingSinceMillis = nowMillis
            return current
        }
        return if (nowMillis - pendingSinceMillis >= com.pamaz.vi.core.mapview.tile.TileCacheConfig.ZOOM_LEVEL_REQUEST_STABILITY_MS) {
            lastRequestedLevel = renderLevel
            pendingLevel = null
            renderLevel
        } else {
            current
        }
    }
}

private fun requestRing(
    engine: TileEngine,
    mapVersion: Int,
    zoom: Int,
    tileX: Int,
    tileY: Int,
    tilesWide: Int,
    tilesHigh: Int,
    nowMillis: Long,
) {
    for (dy in -1..1) {
        for (dx in -1..1) {
            if (dx == 0 && dy == 0) continue
            val x = tileX + dx
            val y = tileY + dy
            if (x < 0 || y < 0 || x >= tilesWide || y >= tilesHigh) continue
            engine.coordinator.request(zoom, x, y, TileRequestPriority.PREFETCH_RING, nowMillis)
        }
    }
}

private fun requestNextZoomCenter(
    engine: TileEngine,
    renderLevel: Int,
    nativeMaxZoom: Int,
    scheme: TileScheme,
    nowMillis: Long,
) {
    val nextZoom = min(renderLevel + 1, nativeMaxZoom)
    if (nextZoom == renderLevel) return
    val tilesWide = scheme.tilesWide(nextZoom)
    val tilesHigh = scheme.tilesHigh(nextZoom)
    val centerX = max(0, tilesWide / 2)
    val centerY = max(0, tilesHigh / 2)
    engine.coordinator.request(nextZoom, centerX, centerY, TileRequestPriority.NEXT_ZOOM, nowMillis)
}

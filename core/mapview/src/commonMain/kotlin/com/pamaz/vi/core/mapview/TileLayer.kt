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
 * **Which tile level to draw vs. which to fetch are two different decisions, on purpose** — an
 * earlier version of this file conflated them (using one hysteresis-smoothed "render level" for
 * both), which caused a real, reported bug: [TileScheme.visibleTiles] already has its own correct,
 * tested continuous-zoom handling built in (`fractionalFactor`, scaling *and* positioning every
 * tile self-consistently so the selected range always exactly covers the viewport at any
 * fractional zoom) — reusing a *different*, hysteresis-lagged level for an external canvas-level
 * `scale()` on top of that fought against it, leaving gaps at the viewport edges whenever the two
 * diverged (which hysteresis guarantees will happen). The fix: let [TileScheme.visibleTiles] pick
 * the tile level to *draw* on its own, every frame, from the true continuous zoom (clamped to
 * `nativeMinZoom..nativeMaxZoom`) — [TileRange.zoom] is whatever it resolves to. A missing tile at
 * that level falls back to the nearest cached ancestor exactly as usual, so this level changing
 * from frame to frame near a boundary is cosmetically harmless (the fallback image is the same
 * ancestor either way). [ZoomRequestDebounceState] still debounces *fetch requests* specifically
 * off that same resolved level, so a fast pinch sweeping across several integer levels doesn't
 * start (and immediately abandon) a network fetch for every single one of them — drawing can
 * change level every frame for free; spending a real request on it is the expensive part this
 * still protects.
 *
 * [nativeMaxZoom] is the sharpest zoom level real tiles exist for; [state.zoom][MapViewerState.zoom]
 * may run higher than that (overzoom, see [com.pamaz.vi.core.mapview.tile.TileCacheConfig
 * .OVERZOOM_LEVELS]/the module README's "Overzoom" section) — [overzoomScaleFactor] is exactly
 * `1.0` whenever [state.zoom][MapViewerState.zoom] is within `nativeMinZoom..nativeMaxZoom` (i.e.
 * for the entire native range, left to [TileScheme.visibleTiles] itself as described above), and
 * only scales the whole canvas up past that point, with [FilterQuality.Medium] smoothing.
 * [MapViewer] is the only caller.
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
        val nativeClampedZoom =
            continuousZoom.coerceIn(nativeMinZoom.toFloat(), nativeMaxZoom.toFloat())
        val overzoomFactor = overzoomScaleFactor(continuousZoom, nativeMinZoom, nativeMaxZoom)

        // Pivot at the ORIGIN, not the viewport center — a real, reported bug ("tiles drift away
        // from the map at heavy zoom"). `visibleTiles` below is fed the *shrunken*
        // `size / overzoomFactor` viewport, so it places `state.center` at half of THAT (smaller)
        // value, not at half of the real `size`. Scaling around the viewport's visual center would
        // move that already-off-center point even further off-center, proportionally to
        // `overzoomFactor` — invisible at `overzoomFactor == 1` (native range), increasingly wrong
        // the deeper into overzoom you go (exactly "quando faccio troppo zoom"). Scaling around
        // (0, 0) instead is the correct complement to feeding `visibleTiles` a shrunken viewport:
        // local position `size/(2*overzoomFactor)` (where `visibleTiles` places `state.center`)
        // maps to `0 + size/(2*overzoomFactor) * overzoomFactor == size/2` — the true screen
        // center — for *any* `overzoomFactor`, not just `1`.
        scale(overzoomFactor, pivot = Offset.Zero) {
            // Pre-scale coordinate space (see this function's own KDoc): `overzoomFactor` is
            // exactly 1.0 outside genuine overzoom, making this division a no-op for the entire
            // native zoom range — `visibleTiles` handles that range's own continuous scaling
            // internally via `nativeClampedZoom`, consistently, with no risk of the two
            // calculations diverging the way an external hysteresis-based one used to.
            val tileRange = scheme.visibleTiles(
                center = state.center,
                zoom = nativeClampedZoom,
                viewportWidthPx = size.width / overzoomFactor,
                viewportHeightPx = size.height / overzoomFactor,
            )
            val drawLevel = tileRange.zoom
            val requestLevel = zoomRequestState.decide(
                renderLevel = drawLevel,
                isAnimating = state.isZoomAnimating,
                nowMillis = now,
            )

            val tilesWide = scheme.tilesWide(drawLevel)
            val tilesHigh = scheme.tilesHigh(drawLevel)

            for (tile in tileRange.tiles) {
                val key = TileKey(mapVersion, drawLevel, tile.tileX, tile.tileY)
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
                // frame at `drawLevel` regardless, using whatever is already resident.
                if (drawLevel == requestLevel) {
                    engine.coordinator.request(
                        drawLevel, tile.tileX, tile.tileY, TileRequestPriority.VIEWPORT, now,
                    )
                    requestRing(
                        engine,
                        mapVersion,
                        drawLevel,
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
                        engine, mapVersion, drawLevel, tile.tileX, tile.tileY,
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
            if (requestLevel == drawLevel && drawLevel < nativeMaxZoom) {
                requestNextZoomCenter(engine, drawLevel, nativeMaxZoom, scheme, now)
            }
        }
    }
}

private val TileCrossfadeDurationMs =
    com.pamaz.vi.core.mapview.tile.TileCacheConfig.CROSSFADE_DURATION_MS

/**
 * `1.0` whenever [continuousZoom] is within `nativeMinZoom..nativeMaxZoom` — [TileScheme
 * .visibleTiles] already handles that whole range's continuous scaling internally (its own
 * `fractionalFactor`), so there is nothing left for an external canvas transform to do there.
 * Only once [continuousZoom] runs past [nativeMaxZoom] (overzoom — [state.zoom][MapViewerState
 * .zoom] can go [com.pamaz.vi.core.mapview.tile.TileCacheConfig.OVERZOOM_LEVELS] levels past the
 * native maximum) does this return something other than `1.0`: the leftover enlargement
 * [TileLayer] applies with its own `scale()`, on top of tiles [TileScheme.visibleTiles] already
 * rendered at exactly [nativeMaxZoom]'s own native resolution.
 *
 * Pure/extracted specifically so this is unit-testable — see `OverzoomScaleFactorTest`.
 */
internal fun overzoomScaleFactor(
    continuousZoom: Float,
    nativeMinZoom: Int,
    nativeMaxZoom: Int
): Float {
    val clamped = continuousZoom.coerceIn(nativeMinZoom.toFloat(), nativeMaxZoom.toFloat())
    return 2.0.pow((continuousZoom - clamped).toDouble()).toFloat()
}

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
 * or immediately for animated zooms" — [TileLayer] always knows which level to *draw* this frame
 * (whatever [com.pamaz.vi.core.model.map.TileScheme.visibleTiles] resolves to; see [TileLayer]'s
 * own KDoc), so this class is purely about *fetch* debouncing: a fast pinch sweeping past several
 * integer levels shouldn't start (and then immediately abandon) a network/disk request for every
 * one of them.
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

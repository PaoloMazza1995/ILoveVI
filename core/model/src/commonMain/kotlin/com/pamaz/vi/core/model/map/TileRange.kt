package com.pamaz.vi.core.model.map

import com.pamaz.vi.core.model.NormalizedPoint
import kotlin.math.roundToInt

/** One tile to draw on screen: its grid coordinates and the top-left pixel to draw it at. */
data class VisibleTile(
    val tileX: Int,
    val tileY: Int,
    val screenX: Float,
    val screenY: Float,
)

/**
 * The tiles needed to fill a viewport, as returned by [TileScheme.visibleTiles].
 *
 * [zoom] is the integer tile level the tiles were fetched for; [tileScreenSizePx] is how big each
 * tile must be drawn on screen, which differs from [TileScheme.tileSize] whenever the requested
 * zoom was fractional (pinch-zoom between two integer levels) and had to snap to [zoom].
 */
data class TileRange(
    val zoom: Int,
    val tileScreenSizePx: Float,
    val tiles: List<VisibleTile>,
)

/**
 * Computes which tiles are needed to fill a [viewportWidthPx] by [viewportHeightPx] viewport
 * centered on [center], at [zoom] — a fractional level is supported for pinch-zoom: the nearest
 * integer tile level is fetched and each tile is scaled by the leftover factor via
 * [TileRange.tileScreenSizePx] instead of re-fetching a new tile set for every pinch frame.
 *
 * The requested area is clamped to the image bounds, so panning near an edge or corner never
 * asks for tiles outside the valid `0..tilesWide-1` / `0..tilesHigh-1` grid; the viewport simply
 * shows blank space beyond the image edge instead, reflected in the returned screen offsets.
 */
fun TileScheme.visibleTiles(
    center: NormalizedPoint,
    zoom: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
): TileRange {
    require(viewportWidthPx > 0f && viewportHeightPx > 0f) {
        "viewport size must be positive, was ${viewportWidthPx}x$viewportHeightPx"
    }

    val clampedZoom = zoom.coerceIn(minZoom.toFloat(), maxZoom.toFloat())
    val level = clampedZoom.roundToInt().coerceIn(minZoom, maxZoom)

    val levelScale = scaleAt(level)
    val continuousScale = scaleAt(clampedZoom)
    // How much more (or less) zoomed in the continuous pinch zoom is versus the snapped tile
    // level: 1f means no leftover scaling, drawing each tile at its native tileSize.
    val fractionalFactor = (continuousScale / levelScale).toFloat()
    val tileScreenSizePx = tileSize * fractionalFactor

    val scaledWidth = (widthPx * levelScale).toFloat()
    val scaledHeight = (heightPx * levelScale).toFloat()

    val centerPxX = center.x * scaledWidth
    val centerPxY = center.y * scaledHeight

    val halfWidthLevelPx = (viewportWidthPx / 2f) / fractionalFactor
    val halfHeightLevelPx = (viewportHeightPx / 2f) / fractionalFactor

    val topLeftX = centerPxX - halfWidthLevelPx
    val topLeftY = centerPxY - halfHeightLevelPx
    val bottomRightX = centerPxX + halfWidthLevelPx
    val bottomRightY = centerPxY + halfHeightLevelPx

    val tilesWide = tilesWide(level)
    val tilesHigh = tilesHigh(level)

    val minTileX = tileIndexFor(topLeftX.coerceIn(0f, scaledWidth), tilesWide)
    val maxTileX =
        tileIndexFor((bottomRightX.coerceIn(0f, scaledWidth) - 1f).coerceAtLeast(0f), tilesWide)
    val minTileY = tileIndexFor(topLeftY.coerceIn(0f, scaledHeight), tilesHigh)
    val maxTileY =
        tileIndexFor((bottomRightY.coerceIn(0f, scaledHeight) - 1f).coerceAtLeast(0f), tilesHigh)

    val tiles = buildList {
        for (tileY in minTileY..maxTileY) {
            for (tileX in minTileX..maxTileX) {
                add(
                    VisibleTile(
                        tileX = tileX,
                        tileY = tileY,
                        screenX = (tileX * tileSize - topLeftX) * fractionalFactor,
                        screenY = (tileY * tileSize - topLeftY) * fractionalFactor,
                    )
                )
            }
        }
    }

    return TileRange(zoom = level, tileScreenSizePx = tileScreenSizePx, tiles = tiles)
}

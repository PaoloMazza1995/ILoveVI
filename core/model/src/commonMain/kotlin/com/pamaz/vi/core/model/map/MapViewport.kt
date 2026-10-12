package com.pamaz.vi.core.model.map

import com.pamaz.vi.core.model.NormalizedPoint

/** A point in on-screen pixels, relative to the top-left corner of a map viewport. */
data class ScreenPoint(val x: Float, val y: Float)

/**
 * Converts between [NormalizedPoint]s and on-screen pixels for a map viewport defined by its
 * [TileScheme], [center][NormalizedPoint] point, continuous zoom level and pixel size.
 *
 * Unlike [TileScheme.visibleTiles], these conversions use the exact continuous zoom (no snapping
 * to an integer tile level): they place a single point, not a grid of fetchable tiles, so there
 * is no tile image to snap to in the first place.
 */
object MapViewport {
    /** Clamps [zoom] to the [TileScheme.minZoom]..[TileScheme.maxZoom] range [scheme] supports. */
    fun clampZoom(zoom: Float, scheme: TileScheme): Float =
        zoom.coerceIn(scheme.minZoom.toFloat(), scheme.maxZoom.toFloat())

    /** Where [point] falls on screen, given the viewport's [center], [zoom] and pixel size. */
    fun toScreen(
        point: NormalizedPoint,
        scheme: TileScheme,
        center: NormalizedPoint,
        zoom: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): ScreenPoint {
        val scale = scheme.scaleAt(zoom)
        val scaledWidth = scheme.widthPx * scale
        val scaledHeight = scheme.heightPx * scale

        val screenX =
            viewportWidthPx / 2f + ((point.x - center.x).toDouble() * scaledWidth).toFloat()
        val screenY =
            viewportHeightPx / 2f + ((point.y - center.y).toDouble() * scaledHeight).toFloat()
        return ScreenPoint(screenX, screenY)
    }

    /** Inverse of [toScreen]: which [NormalizedPoint] a given [screen] pixel corresponds to. */
    fun toNormalized(
        screen: ScreenPoint,
        scheme: TileScheme,
        center: NormalizedPoint,
        zoom: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
    ): NormalizedPoint {
        val scale = scheme.scaleAt(zoom)
        val scaledWidth = scheme.widthPx * scale
        val scaledHeight = scheme.heightPx * scale

        val x = (center.x + ((screen.x - viewportWidthPx / 2f) / scaledWidth).toFloat()).coerceIn(
            0f,
            1f
        )
        val y = (center.y + ((screen.y - viewportHeightPx / 2f) / scaledHeight).toFloat()).coerceIn(
            0f,
            1f
        )
        return NormalizedPoint(x, y)
    }
}

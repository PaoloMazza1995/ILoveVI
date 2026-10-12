package com.pamaz.vi.core.model.map

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow

/**
 * Describes how a [MapConfig][com.pamaz.vi.core.model.MapConfig]-sized source image of
 * [widthPx] by [heightPx] is cut into [tileSize]-pixel square tiles at each zoom level.
 *
 * Zoom levels follow the standard slippy-map convention: [minZoom] is always `0` — the image
 * scaled down until it fits inside a single tile — and [maxZoom] is the number of times that
 * single tile must double in resolution to show the image at its native, 1:1 pixel size. At an
 * arbitrary level `z`, the full image is rendered at `2^(z - maxZoom)` of its native size; that
 * scale is exposed as [scaleAt] since every other calculation in this file builds on it.
 */
data class TileScheme(
    /** Native width of the source map image, in pixels. */
    val widthPx: Int,
    /** Native height of the source map image, in pixels. */
    val heightPx: Int,
    /** Edge length of one square tile, in pixels. */
    val tileSize: Int = 256,
) {
    init {
        require(widthPx > 0) { "widthPx must be positive, was $widthPx" }
        require(heightPx > 0) { "heightPx must be positive, was $heightPx" }
        require(tileSize > 0) { "tileSize must be positive, was $tileSize" }
    }

    /** Always `0`: the overview level where the whole image fits inside one [tileSize] tile. */
    val minZoom: Int = 0

    /**
     * The zoom level at which the image is shown 1:1, computed by counting how many times
     * [tileSize] must double to reach the image's largest dimension. Doubling (rather than
     * `ceil(log2(...))`) avoids floating-point error landing on the wrong side of an exact power
     * of two for image sizes that happen to be exact multiples of [tileSize].
     */
    val maxZoom: Int = run {
        val largestDim = max(widthPx, heightPx)
        var size = tileSize
        var zoom = 0
        while (size < largestDim) {
            size *= 2
            zoom++
        }
        zoom
    }

    /** The full image's rendering scale at integer zoom level [zoom], as `2^(zoom - maxZoom)`. */
    fun scaleAt(zoom: Int): Double {
        require(zoom in minZoom..maxZoom) { "zoom must be in $minZoom..$maxZoom, was $zoom" }
        return 2.0.pow(zoom - maxZoom)
    }

    /**
     * The full image's rendering scale at a fractional zoom level (pinch-zoom in progress
     * between two integer tile levels), as `2^(zoom - maxZoom)`.
     */
    fun scaleAt(zoom: Float): Double = 2.0.pow((zoom - maxZoom).toDouble())

    /** How many tile columns exist at integer zoom level [zoom]. */
    fun tilesWide(zoom: Int): Int =
        ceil(widthPx * scaleAt(zoom) / tileSize).toInt().coerceAtLeast(1)

    /** How many tile rows exist at integer zoom level [zoom]. */
    fun tilesHigh(zoom: Int): Int =
        ceil(heightPx * scaleAt(zoom) / tileSize).toInt().coerceAtLeast(1)

    /** Clamps a pixel coordinate to the `[0, tileCount)` tile index it falls into. */
    internal fun tileIndexFor(pixel: Float, tileCount: Int): Int =
        floor(pixel / tileSize).toInt().coerceIn(0, tileCount - 1)
}

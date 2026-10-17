package com.pamaz.vi.core.mapview

import com.pamaz.vi.core.model.map.TileScheme
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Single-image mode's max zoom, expressed as a multiple of the image's native 1:1 pixel scale. */
internal const val SINGLE_IMAGE_MAX_SCALE = 8f

/**
 * A [TileScheme] over one bundled/local image, built so [TileScheme.scaleAt] is exactly `2^zoom`
 * (zoom `0` == the image's native 1:1 scale) — achieved by setting [TileScheme.tileSize] to the
 * image's own longer side, which makes [TileScheme.maxZoom] always `0` (see its KDoc: zoom is
 * "how many times tileSize must double to reach the image's largest dimension"; if `tileSize`
 * already *is* that dimension, it never needs to double).
 *
 * This is what lets single-image mode reuse every tile-mode helper completely unchanged —
 * [com.pamaz.vi.core.model.map.MapViewport], [computeFocalZoomCenter], [panCenter], [hitTestPin]
 * and [mapViewerGestures] only ever call [TileScheme.scaleAt]/`.widthPx`/`.heightPx`; none of them
 * know or care that there's no actual tile pyramid behind this particular scheme.
 */
internal fun singleImageTileScheme(imageWidthPx: Int, imageHeightPx: Int): TileScheme =
    TileScheme(imageWidthPx, imageHeightPx, tileSize = max(imageWidthPx, imageHeightPx))

/**
 * `log2(`[SINGLE_IMAGE_MAX_SCALE]`)` — the zoom value at which a [singleImageTileScheme] renders
 * the image at [SINGLE_IMAGE_MAX_SCALE] times its native size.
 */
internal val SINGLE_IMAGE_MAX_ZOOM: Float = log2(SINGLE_IMAGE_MAX_SCALE)

/**
 * The zoom at which the whole image exactly fits inside a [viewportWidthPx] by [viewportHeightPx]
 * viewport ("contain" fit: the image is never cropped; the axis that doesn't exactly match the
 * viewport letterboxes). Returns `0f` if the viewport hasn't been measured yet (either dimension
 * `<= 0`) — [MapViewer] re-derives this every time [MapViewerState.viewportSize] changes, so that
 * transient `0f` only shows for a single frame before layout.
 */
internal fun singleImageMinZoom(
    scheme: TileScheme,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
): Float {
    if (viewportWidthPx <= 0f || viewportHeightPx <= 0f) return 0f
    val fitScale = min(viewportWidthPx / scheme.widthPx, viewportHeightPx / scheme.heightPx)
    return log2(fitScale.toDouble()).toFloat()
}

/**
 * The minimum fraction of the image (or viewport, whichever is smaller) that single-image mode
 * guarantees stays visible while panning — see [visibleFraction]'s KDoc for why no *extra*
 * pan-clamp function exists on top of [MapViewerState]'s own `0f..1f` center bound.
 */
internal const val SINGLE_IMAGE_MIN_VISIBLE_FRACTION = 0.25f

/**
 * Fraction (`0f..1f`) of the smaller of (the image's rendered size, the viewport's size) that
 * stays visible along one axis, for a [centerAxis] (the matching
 * [com.pamaz.vi.core.model.NormalizedPoint] coordinate), [scaledDimPx] (the image's rendered
 * width/height at the current zoom) and [viewportDimPx] (the viewport's width/height).
 *
 * This module adds no dedicated pan-clamp for single-image mode: [MapViewerState.center] is
 * already hard-bounded to `0f..1f` on both axes by its own `Animatable` bounds, and across this
 * module's whole zoom range ([singleImageMinZoom]..[SINGLE_IMAGE_MAX_ZOOM]) that existing bound
 * alone always keeps at least 50% of the smaller dimension visible — comfortably over
 * [SINGLE_IMAGE_MIN_VISIBLE_FRACTION] — so a second, redundant clamp would only duplicate
 * [mapViewerGestures] for no behavioral change (the product rule is "the image can never be
 * dragged completely out of view: keep at least 25% visible", not "panning must be artificially
 * restricted beyond the existing image-bounds clamp"). This function exists so that invariant is
 * written down and tested, rather than left as an unstated side effect of the zoom range.
 */
internal fun visibleFraction(centerAxis: Float, scaledDimPx: Float, viewportDimPx: Float): Float {
    if (viewportDimPx <= 0f || scaledDimPx <= 0f) return 1f
    val left = viewportDimPx / 2f - centerAxis * scaledDimPx
    val right = left + scaledDimPx
    val overlap = (min(right, viewportDimPx) - max(left, 0f)).coerceAtLeast(0f)
    return overlap / min(scaledDimPx, viewportDimPx)
}

/** [downscaledSize]'s result: target pixel dimensions preserving the source's aspect ratio. */
internal data class DownscaledSize(val widthPx: Int, val heightPx: Int)

/**
 * The pixel size an image of [widthPx] by [heightPx] should be decoded at so neither side exceeds
 * [maxDimensionPx], preserving aspect ratio — or the original size unchanged if it already fits.
 * Pure dimension math only; each platform's `decodeImageBitmapDownscaled` actual is responsible
 * for actually decoding at (approximately, subject to its own platform's constraints) this size
 * without ever materializing a full-resolution raster first.
 */
internal fun downscaledSize(widthPx: Int, heightPx: Int, maxDimensionPx: Int): DownscaledSize {
    require(widthPx > 0 && heightPx > 0) { "widthPx/heightPx must be positive, was ${widthPx}x$heightPx" }
    require(maxDimensionPx > 0) { "maxDimensionPx must be positive, was $maxDimensionPx" }
    val longerSide = max(widthPx, heightPx)
    if (longerSide <= maxDimensionPx) return DownscaledSize(widthPx, heightPx)
    val scale = maxDimensionPx.toFloat() / longerSide
    val targetWidth = (widthPx * scale).roundToInt().coerceAtLeast(1)
    val targetHeight = (heightPx * scale).roundToInt().coerceAtLeast(1)
    return DownscaledSize(targetWidth, targetHeight)
}

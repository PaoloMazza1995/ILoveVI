package com.pamaz.vi.core.model

import kotlinx.serialization.Serializable

/**
 * A point on the map normalized to the 0..1 range on both axes, independent of the map image's
 * pixel resolution and of any zoom/tile level. The origin (0, 0) is the top-left corner of the
 * map image; [x] grows to the right, [y] grows downward.
 *
 * Out-of-range coordinates are rejected at construction time (see [init]) rather than silently
 * clamped: a coordinate outside 0..1 means the upstream pixel-to-normalized conversion used the
 * wrong image size, and clamping would hide that bug instead of surfacing it. The same choice
 * applies to [fromPixels].
 */
@Serializable
data class NormalizedPoint(
    /** Horizontal position: 0 = left edge, 1 = right edge. */
    val x: Float,
    /** Vertical position: 0 = top edge, 1 = bottom edge. */
    val y: Float,
) {
    init {
        require(x in 0f..1f && y in 0f..1f) {
            "NormalizedPoint coordinates must be in 0f..1f, was x=$x, y=$y"
        }
    }

    /** Converts back to pixel coordinates for a map image of the given [width]/[height], as (px, py). */
    fun toPixels(width: Float, height: Float): Pair<Float, Float> = (x * width) to (y * height)

    companion object {
        /**
         * Builds a [NormalizedPoint] from pixel coordinates ([px], [py]) on a map image of the
         * given [width]/[height]. Throws [IllegalArgumentException] if [width]/[height] aren't
         * positive, or if the resulting normalized coordinate falls outside 0..1 — e.g. because
         * [px]/[py] came from a different image size than [width]/[height] (propagated from
         * [NormalizedPoint]'s own [init] check).
         */
        fun fromPixels(px: Float, py: Float, width: Float, height: Float): NormalizedPoint {
            require(width > 0f && height > 0f) {
                "width and height must be positive, was width=$width, height=$height"
            }
            return NormalizedPoint(px / width, py / height)
        }
    }
}

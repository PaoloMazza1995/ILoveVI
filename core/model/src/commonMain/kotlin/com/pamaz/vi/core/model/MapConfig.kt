package com.pamaz.vi.core.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * Describes the map image currently in use and how to fetch its tiles. [MapPin.mapVersion]
 * references [mapVersion] here, so pins placed on an older map image can be flagged for
 * repositioning once a new one is published.
 */
@Serializable
data class MapConfig(
    /** Monotonically increasing version of the map image; bump it whenever [sourceImageUrl] changes. */
    val mapVersion: Int,
    /** URL of the full-resolution source map image, [widthPx] by [heightPx]. */
    val sourceImageUrl: String,
    /** Width of [sourceImageUrl] in pixels; the denominator used by [NormalizedPoint.toPixels]/[NormalizedPoint.fromPixels]. */
    val widthPx: Int,
    /** Height of [sourceImageUrl] in pixels; the denominator used by [NormalizedPoint.toPixels]/[NormalizedPoint.fromPixels]. */
    val heightPx: Int,
    /** Tile URL template containing the `{z}`, `{x}` and `{y}` placeholders, e.g. `"https://example.com/tiles/{z}/{x}/{y}.png"`. */
    val tileUrlTemplate: String,
    /** Lowest zoom level the tile set provides. */
    val minZoom: Int,
    /** Highest zoom level the tile set provides. */
    val maxZoom: Int,
    /** Last time this config changed, used for delta sync. */
    val updatedAt: Instant,
) {
    init {
        require(widthPx > 0) { "widthPx must be positive, was $widthPx" }
        require(heightPx > 0) { "heightPx must be positive, was $heightPx" }
        require(minZoom <= maxZoom) { "minZoom ($minZoom) must be <= maxZoom ($maxZoom)" }
        require("{z}" in tileUrlTemplate && "{x}" in tileUrlTemplate && "{y}" in tileUrlTemplate) {
            "tileUrlTemplate must contain {z}, {x} and {y} placeholders, was \"$tileUrlTemplate\""
        }
    }
}

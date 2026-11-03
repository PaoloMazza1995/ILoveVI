package com.pamaz.vi.tools.tilegen

/**
 * The on-disk/uploaded format for every tile a pyramid is generated in. [WEBP] is the default:
 * the map is fully opaque (no transparency ever needed in a tile), and lossy WebP is several
 * times smaller than PNG at a visually equivalent quality — meaningfully cheaper against Supabase
 * Storage's per-plan quota (see `SupabaseStorageLimits` in `:tools:map-editor`). [PNG] remains
 * available as a fallback option (`--format png`) in case a future target can't decode WebP.
 *
 * Decode support was verified, not assumed, across every client target before defaulting to WebP:
 * Android's `BitmapFactory` has decoded WebP (lossy, lossless, alpha) natively since API 14/18 —
 * well under this app's `minSdk = 24` — and Compose Multiplatform's own Skia-backed image
 * decoding (used by this repo's `core:mapview` `decodeImageBitmapDownscaled` on both the JVM/
 * desktop and iOS targets) documents WebP as one of its supported input formats alongside PNG/
 * JPEG/BMP. No target needed a PNG fallback.
 */
enum class TileFormat(val extension: String) {
    WEBP("webp"),
    PNG("png");

    companion object {
        fun fromExtension(extension: String): TileFormat =
            entries.find { it.extension == extension.lowercase() }
                ?: throw IllegalArgumentException(
                    "unknown tile format extension \"$extension\" (expected one of ${entries.joinToString { it.extension }})"
                )
    }
}

/** WebP lossy quality used for every generated tile — chosen as "visually lossless enough for a map, several times smaller than PNG" per this tool's README. Not configurable from the CLI: the Functional Analysis names one fixed value, not a range (same convention `MapPublishPlanning.TILE_SIZE` already follows for tile size). */
const val WEBP_LOSSY_QUALITY: Float = 0.82f

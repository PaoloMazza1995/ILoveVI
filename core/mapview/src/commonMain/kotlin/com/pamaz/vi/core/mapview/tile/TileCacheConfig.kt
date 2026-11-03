package com.pamaz.vi.core.mapview.tile

/**
 * Every tunable of the tile pipeline, in one place with KDoc — the same "named constants, easy to
 * change without hunting through the algorithm" convention `feature:map`'s `PinClusterer` already
 * established for its own constants (`CLUSTER_DISTANCE_PX`/`CLUSTER_MIN_SIZE`).
 */
object TileCacheConfig {
    /** Decoded-bitmap memory cache budget on Android/iOS. A 256px ARGB8888 tile is ~256KB decoded, so this holds ~375 tiles. */
    const val MEMORY_CACHE_BYTES_MOBILE: Long = 96L * 1024 * 1024

    /** Decoded-bitmap memory cache budget on desktop/JVM, where heap pressure is far less of a concern. */
    const val MEMORY_CACHE_BYTES_DESKTOP: Long = 256L * 1024 * 1024

    /** Coil3's own disk cache budget for downloaded tile bytes, on Android/iOS. */
    const val DISK_CACHE_BYTES_MOBILE: Long = 500L * 1024 * 1024

    /** Coil3's own disk cache budget for downloaded tile bytes, on desktop/JVM. */
    const val DISK_CACHE_BYTES_DESKTOP: Long = 2L * 1024 * 1024 * 1024

    /**
     * Zoom levels `0..PINNED_MAX_ZOOM` are never evicted from the memory cache, regardless of the
     * byte budget — the map is never completely blank, even under memory pressure, because the
     * lowest-resolution overview is always resident. Combined with [PINNED_MAX_TILE_COUNT] as a
     * second, independent cap (a very wide/tall map could have far more than a handful of tiles
     * even at a low zoom level; this bounds pinning regardless of aspect ratio).
     */
    const val PINNED_MAX_ZOOM: Int = 3

    /** See [PINNED_MAX_ZOOM] — the absolute ceiling on how many tiles can ever be pinned, independent of which zoom levels that spans. */
    const val PINNED_MAX_TILE_COUNT: Int = 85

    /** How many tile fetches may be in flight at once. */
    const val MAX_PARALLEL_REQUESTS: Int = 8

    /** How many times a failed tile is retried before giving up until it next re-enters the viewport. */
    const val RETRY_MAX_ATTEMPTS: Int = 3

    /** Base delay for exponential backoff between retries of a failed tile (doubles each attempt). */
    const val RETRY_BASE_DELAY_MS: Long = 500L

    /**
     * How long the viewport must stop changing before tiles that are no longer needed get
     * cancelled/released. Deliberately *not* instant: cancelling and restarting on every gesture
     * frame is the single biggest confirmed cause of visible reload flicker (see the module
     * README's "Tile pipeline" section) — a short grace period lets a fast pinch/pan settle before
     * any cleanup happens, while new, genuinely-needed tiles still start loading immediately (see
     * [ZOOM_LEVEL_REQUEST_STABILITY_MS] and [TileLoadCoordinator]'s own KDoc for why starting and
     * cancelling use different timing).
     */
    const val VIEWPORT_STABILITY_DELAY_MS: Long = 120L

    /**
     * How long the zoom must sit still before tiles for a *new* integer zoom level start loading
     * during an organic (finger-driven) zoom gesture. An animated zoom (zoom buttons, double-tap)
     * skips this delay entirely — its target level is known the instant the animation starts, not
     * discovered gradually like a pinch, so there's nothing to debounce.
     */
    const val ZOOM_LEVEL_REQUEST_STABILITY_MS: Long = 80L

    /**
     * Hysteresis band (in zoom levels) around each integer boundary: the active tile zoom only
     * switches once the continuous zoom has moved this far *past* a boundary, not the instant it
     * crosses it. Without this, holding a pinch gesture near e.g. zoom 2.0 flips the tile level
     * back and forth on every tiny finger tremor, each flip tearing down and rebuilding the entire
     * visible tile set (see the module README's diagnosis).
     */
    const val ZOOM_HYSTERESIS: Float = 0.15f

    /** How long a fallback tile cross-fades into the real one once it arrives. No-op (instant swap) for a tile that was already showing nothing, and skipped entirely under reduced-motion. */
    const val CROSSFADE_DURATION_MS: Int = 150

    /**
     * How many zoom levels past a map's native maximum the user may still zoom in by — tiles at
     * the native maximum are simply enlarged with smooth filtering past that point. `3` levels is
     * up to 2^3 = 8x enlargement, the "beyond it the map is enlarged" ratio shown in the map
     * publish dialog (`MapPublishPlanning.kt`, `tools:map-editor`).
     */
    const val OVERZOOM_LEVELS: Int = 3
}

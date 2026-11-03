package com.pamaz.vi.core.mapview.tile

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap

/** A lower-resolution tile to draw in place of a missing one, and which quarter/eighth/... of it to crop and scale up. */
internal data class AncestorFallback(
    val bitmap: ImageBitmap,
    /** In the ancestor bitmap's own pixel coordinates — pass directly to `drawImage`'s `srcOffset`/`srcSize`. */
    val cropPx: Rect,
)

/**
 * Walks up from (`zoom`, `tileX`, `tileY`) toward `minZoom`, returning the first ancestor tile
 * found in [memoryCache] — [MapViewer]'s README calls this "the previous, lower-resolution image
 * stays visible, scaled up" behavior, the same thing Google Maps/every slippy map does instead of
 * a blank tile. Once the pinned overview levels (`TileCacheConfig.PINNED_MAX_ZOOM`) have loaded,
 * this always finds *something*: the walk only has to go as far as the nearest pinned level, which
 * is guaranteed resident.
 *
 * [tileSizePx] is the ancestor's own native tile size (same for every level in this scheme); the
 * returned crop rect is in the ancestor's pixel space, sized `tileSizePx / 2^levels` on each axis,
 * where `levels` is how many zoom levels up the ancestor was found.
 */
internal fun findAncestorFallback(
    memoryCache: TileMemoryCache,
    mapVersion: Int,
    zoom: Int,
    tileX: Int,
    tileY: Int,
    minZoom: Int,
    tileSizePx: Int,
): AncestorFallback? {
    var ancestorZoom = zoom - 1
    var ancestorX = tileX
    var ancestorY = tileY
    while (ancestorZoom >= minZoom) {
        ancestorX /= 2
        ancestorY /= 2
        val key = TileKey(mapVersion, ancestorZoom, ancestorX, ancestorY)
        val bitmap = memoryCache.peek(key)
        if (bitmap != null) {
            val levels = zoom - ancestorZoom
            val factor = 1 shl levels
            val fracX = tileX.floorModOf(factor)
            val fracY = tileY.floorModOf(factor)
            val cropSize = tileSizePx.toFloat() / factor
            return AncestorFallback(
                bitmap = bitmap,
                cropPx = Rect(
                    offset = Offset(fracX * cropSize, fracY * cropSize),
                    size = Size(cropSize, cropSize),
                ),
            )
        }
        ancestorZoom--
    }
    return null
}

/** [tileX]/[tileY] at zoom [ancestorZoom]+[levels]'s position *within* its ancestor's `factor x factor` grid of descendants, as a `0 until factor` index on this axis. */
private fun Int.floorModOf(factor: Int): Int {
    val m = this % factor
    return if (m < 0) m + factor else m
}

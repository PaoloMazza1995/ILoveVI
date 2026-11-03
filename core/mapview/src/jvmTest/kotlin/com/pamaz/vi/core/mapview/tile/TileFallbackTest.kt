package com.pamaz.vi.core.mapview.tile

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [findAncestorFallback] walks up from a missing tile toward `minZoom`, returning the nearest
 * ancestor actually resident in [TileMemoryCache] plus the quarter/eighth/sixteenth crop rect to
 * draw from it — the fix for "black squares while zooming", see that function's own KDoc. jvmTest,
 * not commonTest: needs a real [ImageBitmap] in the cache (see [TileMemoryCacheTest]'s own KDoc).
 */
class TileFallbackTest {

    private val mapVersion = 1
    private val tileSizePx = 256

    private fun bitmap() = ImageBitmap(1, 1)
    private fun key(zoom: Int, x: Int, y: Int) = TileKey(mapVersion, zoom, x, y)

    @Test
    fun findsTheImmediateParent_atZMinus1_withAQuarterCrop() {
        val cache = TileMemoryCache(maxBytes = 10_000)
        // Tile (3,3) at zoom 2 is the parent of (6,6)/(6,7)/(7,6)/(7,7) at zoom 3.
        cache.put(key(zoom = 2, x = 3, y = 3), bitmap(), bytes = 1)

        val fallback = findAncestorFallback(
            memoryCache = cache, mapVersion = mapVersion,
            zoom = 3, tileX = 7, tileY = 7, minZoom = 0, tileSizePx = tileSizePx,
        )

        requireNotNull(fallback)
        // (7,7) is the bottom-right quarter of its zoom-2 parent (3,3): factor 2, cropSize 128.
        assertEquals(128f, fallback.cropPx.width)
        assertEquals(128f, fallback.cropPx.height)
        assertEquals(128f, fallback.cropPx.left)
        assertEquals(128f, fallback.cropPx.top)
    }

    @Test
    fun findsTheGrandparent_atZMinus2_whenTheImmediateParentIsMissing_withAnEighthCrop() {
        val cache = TileMemoryCache(maxBytes = 10_000)
        // zoom-3 parent (3,3) is deliberately absent; only the zoom-2 grandparent (1,1) is resident.
        cache.put(key(zoom = 2, x = 1, y = 1), bitmap(), bytes = 1)

        val fallback = findAncestorFallback(
            memoryCache = cache, mapVersion = mapVersion,
            zoom = 4, tileX = 7, tileY = 7, minZoom = 0, tileSizePx = tileSizePx,
        )

        requireNotNull(fallback)
        // levels = 4 - 2 = 2, factor = 4, cropSize = 256/4 = 64. (7,7) mod 4 = (3,3) -> offset 3*64=192.
        assertEquals(64f, fallback.cropPx.width)
        assertEquals(192f, fallback.cropPx.left)
        assertEquals(192f, fallback.cropPx.top)
    }

    @Test
    fun findsTheGreatGrandparent_atZMinus3_whenNothingCloserExists() {
        val cache = TileMemoryCache(maxBytes = 10_000)
        cache.put(key(zoom = 0, x = 0, y = 0), bitmap(), bytes = 1)

        val fallback = findAncestorFallback(
            memoryCache = cache, mapVersion = mapVersion,
            zoom = 3, tileX = 5, tileY = 2, minZoom = 0, tileSizePx = tileSizePx,
        )

        requireNotNull(fallback)
        // levels = 3, factor = 8, cropSize = 256/8 = 32. (5,2) mod 8 = (5,2) -> offset (160, 64).
        assertEquals(32f, fallback.cropPx.width)
        assertEquals(160f, fallback.cropPx.left)
        assertEquals(64f, fallback.cropPx.top)
    }

    @Test
    fun skipsOverAMissingIntermediateLevel_toTheNearestResidentAncestor() {
        val cache = TileMemoryCache(maxBytes = 10_000)
        // zoom 3 and zoom 2 are both missing; only zoom 1 is resident.
        cache.put(key(zoom = 1, x = 0, y = 0), bitmap(), bytes = 1)

        val fallback = findAncestorFallback(
            memoryCache = cache, mapVersion = mapVersion,
            zoom = 4, tileX = 3, tileY = 1, minZoom = 0, tileSizePx = tileSizePx,
        )

        requireNotNull(fallback)
        // levels = 4 - 1 = 3, factor = 8, cropSize = 32.
        assertEquals(32f, fallback.cropPx.width)
    }

    @Test
    fun returnsNull_whenNoAncestorExistsDownToMinZoom() {
        val cache = TileMemoryCache(maxBytes = 10_000)
        val fallback = findAncestorFallback(
            memoryCache = cache, mapVersion = mapVersion,
            zoom = 3, tileX = 0, tileY = 0, minZoom = 0, tileSizePx = tileSizePx,
        )
        assertNull(fallback)
    }

    @Test
    fun aDifferentMapVersion_isNeverUsedAsAFallback() {
        val cache = TileMemoryCache(maxBytes = 10_000)
        cache.put(TileKey(mapVersion = 999, zoom = 0, tileX = 0, tileY = 0), bitmap(), bytes = 1)

        val fallback = findAncestorFallback(
            memoryCache = cache, mapVersion = mapVersion,
            zoom = 3, tileX = 0, tileY = 0, minZoom = 0, tileSizePx = tileSizePx,
        )
        assertNull(fallback)
    }
}

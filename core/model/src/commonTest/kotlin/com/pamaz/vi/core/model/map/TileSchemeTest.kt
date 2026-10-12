package com.pamaz.vi.core.model.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TileSchemeTest {

    @Test
    fun zoomLevels_8192x6144() {
        val scheme = TileScheme(widthPx = 8192, heightPx = 6144)
        assertEquals(0, scheme.minZoom)
        assertEquals(5, scheme.maxZoom)
    }

    @Test
    fun tileCounts_8192x6144() {
        val scheme = TileScheme(widthPx = 8192, heightPx = 6144)
        assertEquals(32, scheme.tilesWide(5))
        assertEquals(24, scheme.tilesHigh(5))
        assertEquals(16, scheme.tilesWide(4))
        assertEquals(12, scheme.tilesHigh(4))
        assertEquals(1, scheme.tilesWide(0))
        assertEquals(1, scheme.tilesHigh(0))
    }

    @Test
    fun zoomLevels_3000x2000() {
        val scheme = TileScheme(widthPx = 3000, heightPx = 2000)
        assertEquals(0, scheme.minZoom)
        assertEquals(4, scheme.maxZoom)
    }

    @Test
    fun tileCounts_3000x2000() {
        val scheme = TileScheme(widthPx = 3000, heightPx = 2000)
        assertEquals(12, scheme.tilesWide(4))
        assertEquals(8, scheme.tilesHigh(4))
        assertEquals(1, scheme.tilesWide(0))
        assertEquals(1, scheme.tilesHigh(0))
    }

    @Test
    fun exactMultipleOfTileSize_hasExactZoom() {
        val scheme = TileScheme(widthPx = 2048, heightPx = 2048)
        assertEquals(3, scheme.maxZoom)
        assertEquals(8, scheme.tilesWide(3))
        assertEquals(8, scheme.tilesHigh(3))
    }

    @Test
    fun smallerThanOneTile_hasSingleZoomLevel() {
        val scheme = TileScheme(widthPx = 100, heightPx = 80)
        assertEquals(0, scheme.minZoom)
        assertEquals(0, scheme.maxZoom)
        assertEquals(1, scheme.tilesWide(0))
        assertEquals(1, scheme.tilesHigh(0))
    }

    @Test
    fun rejectsNonPositiveDimensions() {
        assertFailsWith<IllegalArgumentException> { TileScheme(widthPx = 0, heightPx = 100) }
        assertFailsWith<IllegalArgumentException> { TileScheme(widthPx = 100, heightPx = -1) }
        assertFailsWith<IllegalArgumentException> {
            TileScheme(
                widthPx = 100,
                heightPx = 100,
                tileSize = 0
            )
        }
    }

    @Test
    fun rejectsZoomOutsideRange() {
        val scheme = TileScheme(widthPx = 3000, heightPx = 2000)
        assertFailsWith<IllegalArgumentException> { scheme.tilesWide(-1) }
        assertFailsWith<IllegalArgumentException> { scheme.tilesWide(5) }
        assertFailsWith<IllegalArgumentException> { scheme.tilesHigh(-1) }
        assertFailsWith<IllegalArgumentException> { scheme.tilesHigh(5) }
    }
}

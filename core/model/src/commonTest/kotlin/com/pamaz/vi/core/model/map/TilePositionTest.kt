package com.pamaz.vi.core.model.map

import com.pamaz.vi.core.model.NormalizedPoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TilePositionTest {

    private fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Float = 0.01f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $expected to be within $tolerance of $actual",
        )
    }

    @Test
    fun tilePosition_center_atMaxZoom() {
        val scheme = TileScheme(widthPx = 3000, heightPx = 2000)
        val tile = scheme.tilePosition(NormalizedPoint(0.5f, 0.5f), zoom = scheme.maxZoom)

        assertEquals(5, tile.tileX)
        assertEquals(3, tile.tileY)
        assertAlmostEquals(220f, tile.offsetX)
        assertAlmostEquals(232f, tile.offsetY)
    }

    @Test
    fun tilePosition_topLeftCorner_isTileZeroWithZeroOffset() {
        val scheme = TileScheme(widthPx = 3000, heightPx = 2000)
        val tile = scheme.tilePosition(NormalizedPoint(0f, 0f), zoom = scheme.maxZoom)

        assertEquals(0, tile.tileX)
        assertEquals(0, tile.tileY)
        assertAlmostEquals(0f, tile.offsetX)
        assertAlmostEquals(0f, tile.offsetY)
    }

    @Test
    fun tilePosition_bottomRightCorner_clampsToLastTile() {
        val scheme = TileScheme(widthPx = 2048, heightPx = 2048)
        val tile = scheme.tilePosition(NormalizedPoint(1f, 1f), zoom = scheme.maxZoom)

        // scaledWidth/Height is exactly 2048 at maxZoom; pixel 2048 would be tile index 8,
        // one past the last valid column/row (0..7), so it clamps into tile 7 at full offset.
        assertEquals(7, tile.tileX)
        assertEquals(7, tile.tileY)
        assertAlmostEquals(256f, tile.offsetX)
        assertAlmostEquals(256f, tile.offsetY)
    }

    @Test
    fun pixelToTileToNormalized_roundTrip() {
        val scheme = TileScheme(widthPx = 4096, heightPx = 2048)
        val original = NormalizedPoint.fromPixels(1234f, 567f, 4096f, 2048f)

        val tile = scheme.tilePosition(original, zoom = scheme.maxZoom)
        val restored = scheme.normalizedPoint(tile)

        assertAlmostEquals(original.x, restored.x)
        assertAlmostEquals(original.y, restored.y)
    }

    @Test
    fun tileToNormalized_roundTripAcrossEveryZoomLevel() {
        val scheme = TileScheme(widthPx = 8192, heightPx = 6144)
        val original = NormalizedPoint(0.3f, 0.8f)

        for (zoom in scheme.minZoom..scheme.maxZoom) {
            val tile = scheme.tilePosition(original, zoom)
            val restored = scheme.normalizedPoint(tile)
            assertAlmostEquals(original.x, restored.x)
            assertAlmostEquals(original.y, restored.y)
        }
    }
}

package com.pamaz.vi.core.model.map

import com.pamaz.vi.core.model.NormalizedPoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TileRangeTest {

    private fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Float = 0.1f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $expected to be within $tolerance of $actual",
        )
    }

    @Test
    fun centeredViewport_exactlyCoversFourTiles() {
        val scheme = TileScheme(widthPx = 1024, heightPx = 1024)
        val range = scheme.visibleTiles(
            center = NormalizedPoint(0.5f, 0.5f),
            zoom = scheme.maxZoom.toFloat(),
            viewportWidthPx = 512f,
            viewportHeightPx = 512f,
        )

        assertEquals(scheme.maxZoom, range.zoom)
        assertAlmostEquals(256f, range.tileScreenSizePx)
        assertEquals(
            setOf(
                VisibleTile(1, 1, 0f, 0f),
                VisibleTile(2, 1, 256f, 0f),
                VisibleTile(1, 2, 0f, 256f),
                VisibleTile(2, 2, 256f, 256f),
            ),
            range.tiles.toSet(),
        )
    }

    @Test
    fun topLeftCorner_clampsToImageBounds() {
        val scheme = TileScheme(widthPx = 1024, heightPx = 1024)
        val range = scheme.visibleTiles(
            center = NormalizedPoint(0f, 0f),
            zoom = scheme.maxZoom.toFloat(),
            viewportWidthPx = 512f,
            viewportHeightPx = 512f,
        )

        // Only the single tile touching the corner is fetched; it is drawn in the viewport's
        // bottom-right quadrant since the other three quadrants fall outside the image.
        assertEquals(listOf(VisibleTile(0, 0, 256f, 256f)), range.tiles)
    }

    @Test
    fun bottomRightCorner_clampsToImageBounds() {
        val scheme = TileScheme(widthPx = 1024, heightPx = 1024)
        val range = scheme.visibleTiles(
            center = NormalizedPoint(1f, 1f),
            zoom = scheme.maxZoom.toFloat(),
            viewportWidthPx = 512f,
            viewportHeightPx = 512f,
        )

        assertEquals(listOf(VisibleTile(3, 3, 0f, 0f)), range.tiles)
    }

    @Test
    fun fractionalZoom_snapsToNearestLevelAndScalesTileSize() {
        val scheme = TileScheme(widthPx = 2048, heightPx = 2048)
        val range = scheme.visibleTiles(
            center = NormalizedPoint(0.5f, 0.5f),
            zoom = 2.5f,
            viewportWidthPx = 100f,
            viewportHeightPx = 100f,
        )

        assertEquals(3, range.zoom)
        assertAlmostEquals(181.02f, range.tileScreenSizePx)
    }

    @Test
    fun zoomOutsideRange_isClampedToSchemeBounds() {
        val scheme = TileScheme(widthPx = 1024, heightPx = 1024)

        val tooLow = scheme.visibleTiles(NormalizedPoint(0.5f, 0.5f), -10f, 100f, 100f)
        val tooHigh = scheme.visibleTiles(NormalizedPoint(0.5f, 0.5f), 100f, 100f, 100f)

        assertEquals(scheme.minZoom, tooLow.zoom)
        assertEquals(scheme.maxZoom, tooHigh.zoom)
    }

    @Test
    fun rejectsNonPositiveViewport() {
        val scheme = TileScheme(widthPx = 1024, heightPx = 1024)
        assertFailsWith<IllegalArgumentException> {
            scheme.visibleTiles(NormalizedPoint(0.5f, 0.5f), 1f, 0f, 100f)
        }
        assertFailsWith<IllegalArgumentException> {
            scheme.visibleTiles(NormalizedPoint(0.5f, 0.5f), 1f, 100f, -1f)
        }
    }
}

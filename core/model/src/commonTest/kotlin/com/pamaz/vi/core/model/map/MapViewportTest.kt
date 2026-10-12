package com.pamaz.vi.core.model.map

import com.pamaz.vi.core.model.NormalizedPoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapViewportTest {

    private fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Float = 0.01f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $expected to be within $tolerance of $actual",
        )
    }

    @Test
    fun clampZoom_withinRange_isUnchanged() {
        val scheme = TileScheme(widthPx = 3000, heightPx = 2000)
        assertEquals(2f, MapViewport.clampZoom(2f, scheme))
    }

    @Test
    fun clampZoom_belowMin_isClampedToMinZoom() {
        val scheme = TileScheme(widthPx = 3000, heightPx = 2000)
        assertEquals(scheme.minZoom.toFloat(), MapViewport.clampZoom(-5f, scheme))
    }

    @Test
    fun clampZoom_aboveMax_isClampedToMaxZoom() {
        val scheme = TileScheme(widthPx = 3000, heightPx = 2000)
        assertEquals(scheme.maxZoom.toFloat(), MapViewport.clampZoom(50f, scheme))
    }

    @Test
    fun centerPoint_isAlwaysAtViewportCenter() {
        val scheme = TileScheme(widthPx = 4096, heightPx = 2048)
        val center = NormalizedPoint(0.4f, 0.6f)

        val screen = MapViewport.toScreen(
            point = center,
            scheme = scheme,
            center = center,
            zoom = scheme.maxZoom.toFloat(),
            viewportWidthPx = 800f,
            viewportHeightPx = 600f,
        )

        assertAlmostEquals(400f, screen.x)
        assertAlmostEquals(300f, screen.y)
    }

    @Test
    fun normalizedToScreenToNormalized_roundTrip() {
        val scheme = TileScheme(widthPx = 8192, heightPx = 6144)
        val center = NormalizedPoint(0.5f, 0.5f)
        val point = NormalizedPoint(0.2f, 0.9f)

        val screen = MapViewport.toScreen(
            point, scheme, center, zoom = 3f, viewportWidthPx = 1200f, viewportHeightPx = 800f,
        )
        val restored = MapViewport.toNormalized(
            screen, scheme, center, zoom = 3f, viewportWidthPx = 1200f, viewportHeightPx = 800f,
        )

        assertAlmostEquals(point.x, restored.x)
        assertAlmostEquals(point.y, restored.y)
    }

    @Test
    fun screenPointFarOutsideImage_clampsToImageBounds() {
        val scheme = TileScheme(widthPx = 1024, heightPx = 1024)
        val center = NormalizedPoint(0.5f, 0.5f)

        val restored = MapViewport.toNormalized(
            screen = ScreenPoint(100_000f, 100_000f),
            scheme = scheme,
            center = center,
            zoom = scheme.maxZoom.toFloat(),
            viewportWidthPx = 500f,
            viewportHeightPx = 500f,
        )

        assertEquals(1f, restored.x)
        assertEquals(1f, restored.y)
    }
}

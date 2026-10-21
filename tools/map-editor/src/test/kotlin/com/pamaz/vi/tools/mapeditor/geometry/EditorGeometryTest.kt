package com.pamaz.vi.tools.mapeditor.geometry

import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.MapViewport
import com.pamaz.vi.core.model.map.ScreenPoint
import com.pamaz.vi.core.model.map.TileScheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** "Click position -> normalized point at several zooms and window sizes" (acceptance). */
class EditorGeometryTest {

    private val scheme = TileScheme(widthPx = 8192, heightPx = 6144)

    private fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Float = 0.001f) {
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected, was $actual")
    }

    @Test
    fun normalizedPointForClick_matchesMapViewport_atSeveralZoomsAndWindowSizes() {
        val center = NormalizedPoint(0.5f, 0.5f)
        val windowSizes = listOf(800f to 600f, 1920f to 1080f, 1024f to 768f)
        val zooms = listOf(0f, 1.5f, 3f, scheme.maxZoom.toFloat())
        val clickOffsets = listOf(0f to 0f, 120f to -80f, -300f to 250f)

        for ((width, height) in windowSizes) {
            for (zoom in zooms) {
                for ((dx, dy) in clickOffsets) {
                    val screen = ScreenPoint(width / 2f + dx, height / 2f + dy)
                    val expected =
                        MapViewport.toNormalized(screen, scheme, center, zoom, width, height)
                    val actual =
                        normalizedPointForClick(screen, scheme, center, zoom, width, height)
                    assertEquals(
                        expected,
                        actual,
                        "zoom=$zoom window=${width}x$height offset=($dx,$dy)"
                    )
                }
            }
        }
    }

    @Test
    fun movePinByScreenDelta_rightAndDown_increasesXAndY() {
        val start = NormalizedPoint(0.5f, 0.5f)
        val moved = movePinByScreenDelta(
            start,
            scheme,
            zoom = scheme.maxZoom.toFloat(),
            dxPx = 100f,
            dyPx = 50f
        )
        assertTrue(moved.x > start.x)
        assertTrue(moved.y > start.y)
    }

    @Test
    fun movePinByScreenDelta_isClampedTo0f_1f() {
        val start = NormalizedPoint(0.01f, 0.99f)
        val moved = movePinByScreenDelta(
            start,
            scheme,
            zoom = scheme.maxZoom.toFloat(),
            dxPx = -1_000_000f,
            dyPx = 1_000_000f
        )
        assertEquals(0f, moved.x)
        assertEquals(1f, moved.y)
    }

    @Test
    fun movePinByScreenDelta_atHigherZoom_movesLessForTheSamePixelDelta() {
        val start = NormalizedPoint(0.5f, 0.5f)
        val atLowZoom = movePinByScreenDelta(start, scheme, zoom = 0f, dxPx = 100f, dyPx = 0f)
        val atHighZoom = movePinByScreenDelta(
            start,
            scheme,
            zoom = scheme.maxZoom.toFloat(),
            dxPx = 100f,
            dyPx = 0f
        )
        // A fixed pixel drag covers less normalized distance once the map is zoomed further in.
        assertTrue((atHighZoom.x - start.x) < (atLowZoom.x - start.x))
    }
}

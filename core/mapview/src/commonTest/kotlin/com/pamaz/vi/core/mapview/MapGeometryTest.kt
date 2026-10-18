package com.pamaz.vi.core.mapview

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.MapViewport
import com.pamaz.vi.core.model.map.ScreenPoint
import com.pamaz.vi.core.model.map.TileScheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapGeometryTest {

    private val scheme = TileScheme(widthPx = 8192, heightPx = 6144)

    private fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Float = 0.01f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $expected to be within $tolerance of $actual"
        )
    }

    // Both tests below stay well inside the image at every zoom level involved (a small zoom
    // delta, a modest focal offset from the viewport center, on an 8192x6144 image) specifically
    // so the image-edge clamp never kicks in — right at that clamp, "keep the focal point exactly
    // fixed" is geometrically impossible (there's nowhere further for the center to go), which is
    // the correct, expected behavior covered separately by the `_clampsToImageBounds` test below.

    @Test
    fun computeFocalZoomCenter_keepsFocalPointUnderTheSameScreenPosition_whenZoomingIn() {
        val center = NormalizedPoint(0.5f, 0.5f)
        val viewportWidth = 800f
        val viewportHeight = 600f
        val focal = ScreenPoint(450f, 350f) // offset from the viewport's own center (400, 300)

        val newCenter = computeFocalZoomCenter(
            scheme, center, currentZoom = 4f, newZoom = 4.5f, focalScreen = focal,
            viewportWidthPx = viewportWidth, viewportHeightPx = viewportHeight,
        )

        val focalPoint =
            MapViewport.toNormalized(focal, scheme, center, 4f, viewportWidth, viewportHeight)
        val screenAfter =
            MapViewport.toScreen(focalPoint, scheme, newCenter, 4.5f, viewportWidth, viewportHeight)
        assertAlmostEquals(focal.x, screenAfter.x)
        assertAlmostEquals(focal.y, screenAfter.y)
    }

    @Test
    fun computeFocalZoomCenter_keepsFocalPointUnderTheSameScreenPosition_whenZoomingOut() {
        val center = NormalizedPoint(0.5f, 0.5f)
        val viewportWidth = 800f
        val viewportHeight = 600f
        val focal = ScreenPoint(350f, 380f)

        val newCenter = computeFocalZoomCenter(
            scheme, center, currentZoom = 4.5f, newZoom = 4f, focalScreen = focal,
            viewportWidthPx = viewportWidth, viewportHeightPx = viewportHeight,
        )

        val focalPoint =
            MapViewport.toNormalized(focal, scheme, center, 4.5f, viewportWidth, viewportHeight)
        val screenAfter =
            MapViewport.toScreen(focalPoint, scheme, newCenter, 4f, viewportWidth, viewportHeight)
        assertAlmostEquals(focal.x, screenAfter.x)
        assertAlmostEquals(focal.y, screenAfter.y)
    }

    @Test
    fun computeFocalZoomCenter_clampsToImageBounds() {
        val center = NormalizedPoint(0f, 0f)
        val newCenter = computeFocalZoomCenter(
            scheme, center, currentZoom = 0f, newZoom = 5f,
            focalScreen = ScreenPoint(2000f, 2000f),
            viewportWidthPx = 100f, viewportHeightPx = 100f,
        )
        assertTrue(newCenter.x in 0f..1f)
        assertTrue(newCenter.y in 0f..1f)
    }

    @Test
    fun panCenter_dragRight_movesCenterLeft() {
        val center = NormalizedPoint(0.5f, 0.5f)
        val panned = panCenter(
            scheme, center, zoom = scheme.maxZoom.toFloat(), dxPx = 100f, dyPx = 0f,
            viewportWidthPx = 800f, viewportHeightPx = 600f,
        )
        assertTrue(panned.x < center.x)
        assertEquals(center.y, panned.y)
    }

    @Test
    fun panCenter_isClampedToViewportEdge_whenDraggingPastRightEdge() {
        // Dragging far to the left (negative dx) reveals content far to the right of center. At
        // scheme.maxZoom (native 1:1 scale), the scaled image (8192x6144) is far bigger than the
        // 800x600 viewport, so the clamp is centerBoundsForAxis's "half" bound, not a bare 1f: the
        // image's right edge can never clear the viewport's right edge.
        val center = NormalizedPoint(0.99f, 0.5f)
        val panned = panCenter(
            scheme, center, zoom = scheme.maxZoom.toFloat(), dxPx = -1_000_000f, dyPx = 0f,
            viewportWidthPx = 800f, viewportHeightPx = 600f,
        )
        val expectedMax = centerBoundsForAxis(8192f, 800f).endInclusive
        assertAlmostEquals(expectedMax, panned.x)
    }

    @Test
    fun panCenter_isClampedToViewportEdge_pastTopEdge() {
        val center = NormalizedPoint(0.5f, 0.01f)
        val panned = panCenter(
            scheme, center, zoom = scheme.maxZoom.toFloat(), dxPx = 0f, dyPx = 1_000_000f,
            viewportWidthPx = 800f, viewportHeightPx = 600f,
        )
        val expectedMin = centerBoundsForAxis(6144f, 600f).start
        assertAlmostEquals(expectedMin, panned.y)
    }

    @Test
    fun centerBoundsForAxis_locksToCenter_whenImageFitsInsideViewport() {
        // The letterboxed axis at "contain" fit: no value other than 0.5f avoids asymmetric
        // empty space on either side.
        val bounds = centerBoundsForAxis(scaledDimPx = 400f, viewportDimPx = 800f)
        assertEquals(0.5f, bounds.start)
        assertEquals(0.5f, bounds.endInclusive)
    }

    @Test
    fun centerBoundsForAxis_neverLetsTheImageEdgeClearTheViewport() {
        val bounds = centerBoundsForAxis(scaledDimPx = 2000f, viewportDimPx = 500f)
        // At the lower bound, the image is pushed as far right as allowed: its left edge sits
        // exactly at the viewport's left edge (x=0), never exposing blank space past it.
        val leftAtLowerBound = 500f / 2f - bounds.start * 2000f
        assertAlmostEquals(0f, leftAtLowerBound)
        // At the upper bound, the image is pushed as far left as allowed: its right edge sits
        // exactly at the viewport's right edge.
        val leftAtUpperBound = 500f / 2f - bounds.endInclusive * 2000f
        assertAlmostEquals(500f, leftAtUpperBound + 2000f)
    }

    private fun pinAt(id: String, point: NormalizedPoint) =
        MapViewerPin(id = id, position = point, category = EntryCategory.WEAPON)

    @Test
    fun hitTestPin_hitsPinUnderTap_atSeveralZoomLevels() {
        val center = NormalizedPoint(0.5f, 0.5f)
        val pinPoint = NormalizedPoint(0.5f, 0.5f)
        val pins = listOf(pinAt("p1", pinPoint))

        for (zoom in scheme.minZoom..scheme.maxZoom) {
            val screen = MapViewport.toScreen(pinPoint, scheme, center, zoom.toFloat(), 1000f, 800f)
            val hit = hitTestPin(pins, screen, scheme, center, zoom.toFloat(), 1000f, 800f)
            assertEquals("p1", hit, "expected a hit at zoom $zoom")
        }
    }

    @Test
    fun hitTestPin_missesWhenTapIsOutsideTarget() {
        val center = NormalizedPoint(0.5f, 0.5f)
        val pins = listOf(pinAt("p1", NormalizedPoint(0.5f, 0.5f)))
        val screen =
            MapViewport.toScreen(NormalizedPoint(0.5f, 0.5f), scheme, center, 3f, 1000f, 800f)

        val farTap = ScreenPoint(screen.x + 100f, screen.y + 100f)
        assertNull(hitTestPin(pins, farTap, scheme, center, 3f, 1000f, 800f))
    }

    @Test
    fun hitTestPin_withinHalfTargetBoundary_stillHits() {
        val center = NormalizedPoint(0.5f, 0.5f)
        val pins = listOf(pinAt("p1", NormalizedPoint(0.5f, 0.5f)))
        val screen =
            MapViewport.toScreen(NormalizedPoint(0.5f, 0.5f), scheme, center, 3f, 1000f, 800f)

        val edgeTap = ScreenPoint(screen.x + 21f, screen.y) // within 44/2 = 22px of center
        assertEquals("p1", hitTestPin(pins, edgeTap, scheme, center, 3f, 1000f, 800f))
    }

    @Test
    fun hitTestPin_onOverlap_lastPinInListWins() {
        val center = NormalizedPoint(0.5f, 0.5f)
        val samePoint = NormalizedPoint(0.5f, 0.5f)
        val pins = listOf(pinAt("bottom", samePoint), pinAt("top", samePoint))
        val screen = MapViewport.toScreen(samePoint, scheme, center, 3f, 1000f, 800f)

        assertEquals("top", hitTestPin(pins, screen, scheme, center, 3f, 1000f, 800f))
    }

    @Test
    fun pinScreenPosition_equalsMapViewportConversion() {
        val center = NormalizedPoint(0.42f, 0.17f)
        val zoom = 3.5f
        val pinPoint = NormalizedPoint(0.8f, 0.2f)

        val expected = MapViewport.toScreen(pinPoint, scheme, center, zoom, 1200f, 900f)
        val pins = listOf(pinAt("p1", pinPoint))
        // hitTestPin computes each pin's screen position via MapViewport.toScreen internally; a
        // tap exactly at that independently-computed position must hit, proving the two agree.
        assertEquals(
            "p1",
            hitTestPin(pins, expected, scheme, center, zoom, 1200f, 900f, minTargetPx = 1f)
        )
    }
}

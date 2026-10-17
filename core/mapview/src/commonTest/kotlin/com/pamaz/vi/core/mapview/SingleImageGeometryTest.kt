package com.pamaz.vi.core.mapview

import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.MapViewport
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SingleImageGeometryTest {

    private fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Float = 0.01f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $expected to be within $tolerance of $actual",
        )
    }

    @Test
    fun normalizedScreenRoundTrip_acrossZoomsViewportsAndAspectRatios() {
        val images = listOf(4096 to 3072, 2000 to 2000, 6000 to 1500)
        val viewports = listOf(800f to 600f, 1080f to 1920f, 400f to 400f)
        val zooms = listOf(-2f, -0.5f, 0f, 1f, 3f)
        val points = listOf(
            NormalizedPoint(0f, 0f), NormalizedPoint(1f, 1f), NormalizedPoint(0.5f, 0.5f),
            NormalizedPoint(0.1f, 0.9f), NormalizedPoint(0.73f, 0.12f),
        )
        val center = NormalizedPoint(0.5f, 0.5f)

        for ((imageW, imageH) in images) {
            val scheme = singleImageTileScheme(imageW, imageH)
            for ((viewportW, viewportH) in viewports) {
                for (zoom in zooms) {
                    for (point in points) {
                        val screen =
                            MapViewport.toScreen(point, scheme, center, zoom, viewportW, viewportH)
                        val roundTripped =
                            MapViewport.toNormalized(
                                screen,
                                scheme,
                                center,
                                zoom,
                                viewportW,
                                viewportH
                            )
                        assertAlmostEquals(point.x, roundTripped.x)
                        assertAlmostEquals(point.y, roundTripped.y)
                    }
                }
            }
        }
    }

    @Test
    fun singleImageTileScheme_scaleAtZoomZero_isNativeOneToOne() {
        val scheme = singleImageTileScheme(4096, 3072)
        assertTrue(abs(scheme.scaleAt(0f) - 1.0) < 0.0001)
    }

    @Test
    fun singleImageTileScheme_scaleAtMaxZoom_is8x() {
        val scheme = singleImageTileScheme(4096, 3072)
        assertTrue(abs(scheme.scaleAt(SINGLE_IMAGE_MAX_ZOOM) - SINGLE_IMAGE_MAX_SCALE) < 0.001)
    }

    @Test
    fun singleImageMinZoom_fitsTheWholeImageInTheViewport() {
        val scheme = singleImageTileScheme(4096, 3072)
        val minZoom = singleImageMinZoom(scheme, 1024f, 768f)
        val scale = scheme.scaleAt(minZoom).toFloat()
        val scaledWidth = scheme.widthPx * scale
        val scaledHeight = scheme.heightPx * scale

        assertTrue(
            scaledWidth <= 1024f + 0.5f,
            "scaledWidth $scaledWidth must fit within viewport width 1024"
        )
        assertTrue(
            scaledHeight <= 768f + 0.5f,
            "scaledHeight $scaledHeight must fit within viewport height 768"
        )
        assertTrue(
            abs(scaledWidth - 1024f) < 0.5f || abs(scaledHeight - 768f) < 0.5f,
            "expected a 'contain' fit: one axis should exactly match the viewport " +
                    "(got ${scaledWidth}x$scaledHeight in a 1024x768 viewport)",
        )
    }

    @Test
    fun singleImageMinZoom_returnsZero_whenViewportNotMeasuredYet() {
        val scheme = singleImageTileScheme(4096, 3072)
        assertEquals(0f, singleImageMinZoom(scheme, 0f, 0f))
        assertEquals(0f, singleImageMinZoom(scheme, 100f, 0f))
    }

    @Test
    fun pinsAtCornersAndCenter_landOnTheMatchingScreenPoint_atZoom1AndZoom4() {
        val scheme = singleImageTileScheme(4096, 3072)
        val center = NormalizedPoint(0.5f, 0.5f)
        val viewportW = 2000f
        val viewportH = 2000f

        for (zoom in listOf(1f, 4f)) {
            val scale = scheme.scaleAt(zoom).toFloat()
            val scaledWidth = scheme.widthPx * scale
            val scaledHeight = scheme.heightPx * scale

            val topLeft = MapViewport.toScreen(
                NormalizedPoint(0f, 0f),
                scheme,
                center,
                zoom,
                viewportW,
                viewportH
            )
            assertAlmostEquals(viewportW / 2f - scaledWidth / 2f, topLeft.x)
            assertAlmostEquals(viewportH / 2f - scaledHeight / 2f, topLeft.y)

            val bottomRight =
                MapViewport.toScreen(
                    NormalizedPoint(1f, 1f),
                    scheme,
                    center,
                    zoom,
                    viewportW,
                    viewportH
                )
            assertAlmostEquals(viewportW / 2f + scaledWidth / 2f, bottomRight.x)
            assertAlmostEquals(viewportH / 2f + scaledHeight / 2f, bottomRight.y)

            val middle =
                MapViewport.toScreen(
                    NormalizedPoint(0.5f, 0.5f),
                    scheme,
                    center,
                    zoom,
                    viewportW,
                    viewportH
                )
            assertAlmostEquals(viewportW / 2f, middle.x)
            assertAlmostEquals(viewportH / 2f, middle.y)
        }
    }

    @Test
    fun visibleFraction_staysAtLeast25Percent_acrossTheWholeZoomRange_forAnyCenter() {
        val scheme = singleImageTileScheme(4096, 3072)
        val viewportW = 1080f
        val viewportH = 1920f
        val minZoom = singleImageMinZoom(scheme, viewportW, viewportH)
        val zooms = listOf(minZoom, (minZoom + SINGLE_IMAGE_MAX_ZOOM) / 2f, SINGLE_IMAGE_MAX_ZOOM)
        val centers = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

        for (zoom in zooms) {
            val scale = scheme.scaleAt(zoom).toFloat()
            val scaledWidth = scheme.widthPx * scale
            val scaledHeight = scheme.heightPx * scale
            for (centerAxis in centers) {
                val fractionX = visibleFraction(centerAxis, scaledWidth, viewportW)
                val fractionY = visibleFraction(centerAxis, scaledHeight, viewportH)
                assertTrue(
                    fractionX >= SINGLE_IMAGE_MIN_VISIBLE_FRACTION,
                    "x visible fraction was $fractionX at center=$centerAxis, zoom=$zoom",
                )
                assertTrue(
                    fractionY >= SINGLE_IMAGE_MIN_VISIBLE_FRACTION,
                    "y visible fraction was $fractionY at center=$centerAxis, zoom=$zoom",
                )
            }
        }
    }

    @Test
    fun downscaledSize_8192x4096_becomes4096x2048() {
        val result = downscaledSize(8192, 4096, 4096)
        assertEquals(4096, result.widthPx)
        assertEquals(2048, result.heightPx)
    }

    @Test
    fun downscaledSize_3000x2000_staysUnchanged() {
        val result = downscaledSize(3000, 2000, 4096)
        assertEquals(3000, result.widthPx)
        assertEquals(2000, result.heightPx)
    }

    @Test
    fun downscaledSize_preservesAspectRatio_forNonSquareOverage() {
        val result = downscaledSize(10000, 2500, 4096)
        assertEquals(4096, result.widthPx)
        assertEquals(1024, result.heightPx)
    }

    @Test
    fun downscaledSize_rejectsNonPositiveInputs() {
        assertFailsWith<IllegalArgumentException> { downscaledSize(0, 100, 4096) }
        assertFailsWith<IllegalArgumentException> { downscaledSize(100, -1, 4096) }
        assertFailsWith<IllegalArgumentException> { downscaledSize(100, 100, 0) }
    }
}

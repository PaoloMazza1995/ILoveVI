package com.pamaz.vi.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NormalizedPointTest {

    @Test
    fun acceptsBoundaryValues() {
        NormalizedPoint(0f, 0f)
        NormalizedPoint(1f, 1f)
        NormalizedPoint(0f, 1f)
        NormalizedPoint(1f, 0f)
    }

    @Test
    fun rejectsBelowLowerBound() {
        assertFailsWith<IllegalArgumentException> { NormalizedPoint(-0.01f, 0.5f) }
        assertFailsWith<IllegalArgumentException> { NormalizedPoint(0.5f, -0.01f) }
    }

    @Test
    fun rejectsAboveUpperBound() {
        assertFailsWith<IllegalArgumentException> { NormalizedPoint(1.01f, 0.5f) }
        assertFailsWith<IllegalArgumentException> { NormalizedPoint(0.5f, 1.01f) }
    }

    @Test
    fun rejectsNaN() {
        assertFailsWith<IllegalArgumentException> { NormalizedPoint(Float.NaN, 0.5f) }
        assertFailsWith<IllegalArgumentException> { NormalizedPoint(0.5f, Float.NaN) }
    }

    @Test
    fun pixelRoundTrip_1000x1000() {
        val original = NormalizedPoint(0.25f, 0.75f)
        val (px, py) = original.toPixels(1000f, 1000f)
        assertEquals(original, NormalizedPoint.fromPixels(px, py, 1000f, 1000f))
    }

    @Test
    fun pixelRoundTrip_4096x2048() {
        val original = NormalizedPoint(0.125f, 0.625f)
        val (px, py) = original.toPixels(4096f, 2048f)
        assertEquals(original, NormalizedPoint.fromPixels(px, py, 4096f, 2048f))
    }

    @Test
    fun pixelRoundTrip_sameNormalizedPointAcrossDifferentMapSizes() {
        val original = NormalizedPoint(0.4f, 0.6f)

        val (px1, py1) = original.toPixels(1000f, 1000f)
        val restored1 = NormalizedPoint.fromPixels(px1, py1, 1000f, 1000f)

        val (px2, py2) = original.toPixels(4096f, 2048f)
        val restored2 = NormalizedPoint.fromPixels(px2, py2, 4096f, 2048f)

        assertEquals(original, restored1)
        assertEquals(original, restored2)
        assertEquals(restored1, restored2)
    }

    @Test
    fun fromPixels_throwsWhenResultOutOfRange() {
        assertFailsWith<IllegalArgumentException> {
            NormalizedPoint.fromPixels(
                1500f,
                500f,
                1000f,
                1000f
            )
        }
    }

    @Test
    fun fromPixels_throwsWhenMapSizeNotPositive() {
        assertFailsWith<IllegalArgumentException> {
            NormalizedPoint.fromPixels(
                10f,
                10f,
                0f,
                1000f
            )
        }
        assertFailsWith<IllegalArgumentException> {
            NormalizedPoint.fromPixels(
                10f,
                10f,
                1000f,
                -5f
            )
        }
    }
}

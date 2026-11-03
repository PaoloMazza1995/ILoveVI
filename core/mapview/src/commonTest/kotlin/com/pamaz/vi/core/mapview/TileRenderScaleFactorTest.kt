package com.pamaz.vi.core.mapview

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * [tileRenderScaleFactor] — the fix for a real, reported bug: tiles rendered at a frozen, discrete
 * zoom level's pixel size for the entire native zoom range, never scaling continuously as the user
 * pinched, only "jumping" once the hysteresis-selected render level flipped. See that function's
 * own KDoc.
 */
class TileRenderScaleFactorTest {

    private fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Float = 0.001f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $expected to be within $tolerance of $actual",
        )
    }

    @Test
    fun exactlyAtTheRenderLevel_scaleIsOne() {
        assertAlmostEquals(1f, tileRenderScaleFactor(continuousZoom = 3f, renderLevel = 3))
    }

    @Test
    fun midwayBetweenTwoLevels_withinNativeRange_scalesUp() {
        // This is exactly the bug: the old formula returned 1.0 here (within native range),
        // leaving the tile grid visually frozen for most of a pinch gesture.
        val scale = tileRenderScaleFactor(continuousZoom = 2.5f, renderLevel = 2)
        assertTrue(
            scale > 1f,
            "expected the tile grid to already be scaling up mid-pinch, was $scale"
        )
        assertAlmostEquals(kotlin.math.sqrt(2f), scale)
    }

    @Test
    fun aboveTheRenderLevel_byAFullLevel_scalesByExactlyTwo() {
        assertAlmostEquals(2f, tileRenderScaleFactor(continuousZoom = 4f, renderLevel = 3))
    }

    @Test
    fun belowTheRenderLevel_scalesDownBelowOne() {
        // The render level can briefly sit *above* the continuous zoom too (hysteresis holding
        // the previous, higher level while zooming out) — tiles must shrink to match, not stay fixed.
        val scale = tileRenderScaleFactor(continuousZoom = 2.5f, renderLevel = 3)
        assertTrue(scale < 1f, "expected the tile grid to already be scaling down, was $scale")
        assertAlmostEquals(1f / kotlin.math.sqrt(2f), scale)
    }

    @Test
    fun deepOverzoom_continuesTheSameFormula_pastTheNativeMax() {
        // nativeMaxZoom = 5, OVERZOOM_LEVELS = 3 -> camera zoom can reach 8, renderLevel pinned at 5.
        assertAlmostEquals(8f, tileRenderScaleFactor(continuousZoom = 8f, renderLevel = 5))
    }
}

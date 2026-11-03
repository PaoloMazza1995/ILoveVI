package com.pamaz.vi.core.mapview

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * [overzoomScaleFactor] — the external canvas scale [TileLayer] applies, which must be exactly
 * `1.0` for the entire native zoom range (that range's own continuous scaling is
 * [com.pamaz.vi.core.model.map.TileScheme.visibleTiles]'s own job — see [TileLayer]'s KDoc for the
 * real, reported bug this split fixes: an earlier version used a *different*, hysteresis-lagged
 * level for this canvas scale, fighting against `visibleTiles`' own math and leaving gaps at the
 * viewport edges whenever the two diverged).
 */
class OverzoomScaleFactorTest {

    private fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Float = 0.001f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $expected to be within $tolerance of $actual",
        )
    }

    @Test
    fun withinTheNativeRange_isAlwaysExactlyOne() {
        assertAlmostEquals(1f, overzoomScaleFactor(0f, nativeMinZoom = 0, nativeMaxZoom = 5))
        assertAlmostEquals(1f, overzoomScaleFactor(2.5f, nativeMinZoom = 0, nativeMaxZoom = 5))
        assertAlmostEquals(1f, overzoomScaleFactor(5f, nativeMinZoom = 0, nativeMaxZoom = 5))
    }

    @Test
    fun pastTheNativeMax_scalesUpContinuously() {
        assertAlmostEquals(2f, overzoomScaleFactor(6f, nativeMinZoom = 0, nativeMaxZoom = 5))
        assertAlmostEquals(8f, overzoomScaleFactor(8f, nativeMinZoom = 0, nativeMaxZoom = 5))
    }

    @Test
    fun belowTheNativeMin_scalesDownContinuously() {
        // Not a case the camera's own zoom range actually allows today (state.minZoom is always
        // >= nativeMinZoom), but the formula is symmetric on purpose rather than silently wrong
        // if that ever changes.
        assertAlmostEquals(0.5f, overzoomScaleFactor(-1f, nativeMinZoom = 0, nativeMaxZoom = 5))
    }
}

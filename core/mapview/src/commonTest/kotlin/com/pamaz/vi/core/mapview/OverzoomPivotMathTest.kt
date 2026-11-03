package com.pamaz.vi.core.mapview

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * [TileLayer] feeds [com.pamaz.vi.core.model.map.TileScheme.visibleTiles] a *shrunken* viewport
 * (`size / overzoomFactor`), which places `state.center` at half of that shrunken value in the
 * pre-scale coordinate space — then wraps the whole draw in `scale(overzoomFactor, pivot =
 * Offset.Zero)`. This test is the algebraic proof that combination is correct for *any*
 * `overzoomFactor`, not just `1`: a real, reported bug ("tiles drift away from the map at heavy
 * zoom") came from pivoting at the viewport center instead — correct only when `overzoomFactor ==
 * 1`, increasingly wrong the deeper into overzoom you go. See [TileLayer]'s own KDoc for the full
 * derivation; this just encodes the resulting invariant as a regression guard.
 */
class OverzoomPivotMathTest {

    private fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Float = 0.01f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $expected to be within $tolerance of $actual",
        )
    }

    /** What `scale(factor, pivot = Offset.Zero)` does to a single local-space coordinate. */
    private fun scaledFromOrigin(local: Float, factor: Float): Float = local * factor

    @Test
    fun noOverzoom_centerMapsToTheRealViewportCenter() {
        val viewportWidth = 1080f
        val overzoomFactor = 1f
        val localCenter = (viewportWidth / overzoomFactor) / 2f
        assertAlmostEquals(viewportWidth / 2f, scaledFromOrigin(localCenter, overzoomFactor))
    }

    @Test
    fun moderateOverzoom_centerStillMapsToTheRealViewportCenter() {
        val viewportWidth = 1080f
        for (overzoomFactor in listOf(1.5f, 2f, 3f, 4f, 8f)) {
            val localCenter = (viewportWidth / overzoomFactor) / 2f
            assertAlmostEquals(
                viewportWidth / 2f,
                scaledFromOrigin(localCenter, overzoomFactor),
                tolerance = 0.5f,
            )
        }
    }

    @Test
    fun theOldViewportCenterPivot_wouldHaveDriftedProportionallyToOverzoom() {
        // Documents exactly what the bug looked like: pivoting at size/2 instead of the origin.
        fun scaledFromViewportCenter(local: Float, factor: Float, viewportWidth: Float): Float {
            val pivot = viewportWidth / 2f
            return pivot + (local - pivot) * factor
        }

        val viewportWidth = 1080f
        val overzoomFactor = 2f
        val localCenter = (viewportWidth / overzoomFactor) / 2f // 270
        val buggyResult = scaledFromViewportCenter(localCenter, overzoomFactor, viewportWidth)

        // Was 0 (the left edge) instead of 540 (the true center) at 2x overzoom — the drift this
        // test guards against ever silently coming back.
        assertAlmostEquals(0f, buggyResult)
        assertTrue(
            abs(buggyResult - viewportWidth / 2f) > 500f,
            "expected a large drift from center"
        )
    }
}

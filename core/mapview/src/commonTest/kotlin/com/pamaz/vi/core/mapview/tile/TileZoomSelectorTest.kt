package com.pamaz.vi.core.mapview.tile

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [TileZoomSelector] picks the stable integer tile level for a continuous camera zoom — see its
 * own KDoc for why hysteresis exists: holding a pinch near an integer boundary must not flip the
 * whole visible tile set back and forth. Pure logic, no platform/Compose dependency, hence
 * commonTest rather than jvmTest (unlike the tests that need a real [androidx.compose.ui.graphics
 * .ImageBitmap]).
 */
class TileZoomSelectorTest {

    @Test
    fun select_firstCall_roundsToNearestInteger() {
        val selector = TileZoomSelector(hysteresis = 0.15f)
        assertEquals(2, selector.select(2.4f, minZoom = 0, maxZoom = 5))
    }

    @Test
    fun select_withinHysteresisBand_staysAtThePreviousLevel() {
        val selector = TileZoomSelector(hysteresis = 0.15f)
        assertEquals(2, selector.select(2.0f, minZoom = 0, maxZoom = 5))
        // 2.64 has rounded() == 3, but hasn't crossed 2.5 + 0.15 = 2.65 yet.
        assertEquals(2, selector.select(2.64f, minZoom = 0, maxZoom = 5))
    }

    @Test
    fun select_pastTheHysteresisThreshold_switchesUpToTheNextLevel() {
        val selector = TileZoomSelector(hysteresis = 0.15f)
        assertEquals(2, selector.select(2.0f, minZoom = 0, maxZoom = 5))
        assertEquals(3, selector.select(2.66f, minZoom = 0, maxZoom = 5))
    }

    @Test
    fun select_pastTheHysteresisThreshold_switchesDownToThePreviousLevel() {
        val selector = TileZoomSelector(hysteresis = 0.15f)
        assertEquals(3, selector.select(3.0f, minZoom = 0, maxZoom = 5))
        // 2.4 rounds to 2 but hasn't crossed 2.5 - 0.15 = 2.35 yet.
        assertEquals(3, selector.select(2.4f, minZoom = 0, maxZoom = 5))
        assertEquals(2, selector.select(2.34f, minZoom = 0, maxZoom = 5))
    }

    @Test
    fun select_repeatedlyHoveringNearABoundary_neverFlips() {
        val selector = TileZoomSelector(hysteresis = 0.15f)
        assertEquals(2, selector.select(2.0f, minZoom = 0, maxZoom = 5))
        // Tremor around the 2/3 boundary, never actually crossing the hysteresis band.
        repeat(20) { i ->
            val wobble = if (i % 2 == 0) 2.55f else 2.45f
            assertEquals(2, selector.select(wobble, minZoom = 0, maxZoom = 5))
        }
    }

    @Test
    fun select_clampsAboveMaxZoom_toMaxZoom() {
        val selector = TileZoomSelector()
        assertEquals(5, selector.select(8f, minZoom = 0, maxZoom = 5))
    }

    @Test
    fun select_clampsBelowMinZoom_toMinZoom() {
        val selector = TileZoomSelector()
        assertEquals(1, selector.select(-3f, minZoom = 1, maxZoom = 5))
    }

    @Test
    fun reset_forgetsTheRememberedLevel_soTheNextCallJustRounds() {
        val selector = TileZoomSelector(hysteresis = 0.15f)
        assertEquals(2, selector.select(2.0f, minZoom = 0, maxZoom = 5))
        selector.reset()
        // Without reset, 2.6 would stay pinned at 2 (hasn't crossed 2.65). After reset, it's a
        // fresh rounding with no memory of "2" at all.
        assertEquals(3, selector.select(2.6f, minZoom = 0, maxZoom = 5))
    }
}

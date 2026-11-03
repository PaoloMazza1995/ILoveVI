package com.pamaz.vi.core.mapview.tile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [pinnedZoomPriorityOrder] — the fix for a real, reported bug: zooming all the way out could show
 * blank/stale tiles even after they'd already loaded once, because the camera's own "zoomed out as
 * far as possible" level (which can land above [TileCacheConfig.PINNED_MAX_ZOOM] on a large
 * viewport) was never actually pinned, so it was subject to normal LRU eviction while panning
 * elsewhere. The two levels that matter — the native minimum (the universal fallback ancestor) and
 * the camera's resting ceiling — must never be the ones [TileCacheConfig.PINNED_MAX_TILE_COUNT]
 * drops if the budget is ever tight; only the in-between "nice to have" levels may be.
 */
class PinnedZoomPriorityOrderTest {

    @Test
    fun nativeMinZoom_isAlwaysFirst() {
        val order = pinnedZoomPriorityOrder(nativeMinZoom = 0, pinnedZoomCeiling = 5)
        assertEquals(0, order.first())
    }

    @Test
    fun theCeiling_isAlwaysSecond_whenDifferentFromTheMinimum() {
        val order = pinnedZoomPriorityOrder(nativeMinZoom = 0, pinnedZoomCeiling = 5)
        assertEquals(5, order[1])
    }

    @Test
    fun theInBetweenLevels_comeAfterBothPriorityLevels() {
        val order = pinnedZoomPriorityOrder(nativeMinZoom = 0, pinnedZoomCeiling = 5)
        assertEquals(listOf(0, 5, 1, 2, 3, 4), order)
    }

    @Test
    fun everyLevelInRange_appearsExactlyOnce() {
        val order = pinnedZoomPriorityOrder(nativeMinZoom = 0, pinnedZoomCeiling = 5)
        assertEquals((0..5).toSet(), order.toSet())
        assertEquals(order.size, order.toSet().size, "no zoom level should be listed twice")
    }

    @Test
    fun ceilingEqualToTheMinimum_isNotListedTwice() {
        // A small map / tiny viewport where the camera's resting level never exceeds the native minimum.
        val order = pinnedZoomPriorityOrder(nativeMinZoom = 2, pinnedZoomCeiling = 2)
        assertEquals(listOf(2), order)
    }

    @Test
    fun aHighCeiling_stillPutsBothCriticalLevelsBeforeAnyMiddleOne() {
        // Simulates the actual bug scenario: a large viewport pushes the camera's resting level
        // well above PINNED_MAX_ZOOM. Even if a byte/tile-count cap later truncates this list,
        // the first two entries — the ones that matter — are guaranteed to survive.
        val order = pinnedZoomPriorityOrder(nativeMinZoom = 0, pinnedZoomCeiling = 7)
        assertTrue(order.indexOf(0) < 2)
        assertTrue(order.indexOf(7) < 2)
    }
}

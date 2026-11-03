package com.pamaz.vi.core.mapview.tile

import androidx.compose.ui.graphics.ImageBitmap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [TileMemoryCache] LRU eviction by total bytes, and the pinned-levels-never-evicted rule — the
 * two things that keep the map's lowest-zoom overview always resident (see [TileCacheConfig
 * .PINNED_MAX_ZOOM]'s KDoc). Lives in jvmTest, not commonTest: a real [ImageBitmap] needs a native
 * Skia backing (`compose.desktop.currentOs`, a jvmTest-only dependency — see
 * [com.pamaz.vi.core.mapview.DevMapImageTest]'s own KDoc for the same constraint on commonTest).
 */
class TileMemoryCacheTest {

    private fun bitmap(sizePx: Int = 1) = ImageBitmap(sizePx, sizePx)
    private fun key(zoom: Int, x: Int, y: Int) =
        TileKey(mapVersion = 1, zoom = zoom, tileX = x, tileY = y)

    @Test
    fun put_thenPeek_returnsTheSameBitmap() {
        val cache = TileMemoryCache(maxBytes = 1_000)
        val bmp = bitmap()
        cache.put(key(0, 0, 0), bmp, bytes = 100)
        assertEquals(bmp, cache.peek(key(0, 0, 0)))
        assertTrue(cache.contains(key(0, 0, 0)))
    }

    @Test
    fun peek_missingKey_returnsNull() {
        val cache = TileMemoryCache(maxBytes = 1_000)
        assertEquals(null, cache.peek(key(0, 0, 0)))
    }

    @Test
    fun put_overBudget_evictsTheLeastRecentlyUsedEntryFirst() {
        val cache = TileMemoryCache(maxBytes = 250)
        cache.put(key(1, 0, 0), bitmap(), bytes = 100) // oldest
        cache.put(key(1, 1, 0), bitmap(), bytes = 100)
        cache.put(key(1, 0, 1), bitmap(), bytes = 100) // pushes total to 300 > 250

        // The first-inserted entry (never touched) is the LRU victim.
        assertFalse(cache.contains(key(1, 0, 0)))
        assertTrue(cache.contains(key(1, 1, 0)))
        assertTrue(cache.contains(key(1, 0, 1)))
        assertTrue(cache.totalBytes <= 250)
    }

    @Test
    fun touch_movesAnEntryToTheEndOfLruOrder_soItSurvivesTheNextEviction() {
        val cache = TileMemoryCache(maxBytes = 250)
        cache.put(key(1, 0, 0), bitmap(), bytes = 100)
        cache.put(key(1, 1, 0), bitmap(), bytes = 100)
        cache.touch(key(1, 0, 0)) // now (1,0,0) is the most-recently-used, (1,1,0) is LRU

        cache.put(key(1, 0, 1), bitmap(), bytes = 100) // over budget again

        assertTrue(cache.contains(key(1, 0, 0)), "touched entry should have survived eviction")
        assertFalse(cache.contains(key(1, 1, 0)), "untouched entry should have been evicted")
    }

    @Test
    fun pinnedKeys_areNeverEvicted_evenWhenWellOverBudget() {
        val cache = TileMemoryCache(maxBytes = 100)
        val pinned = key(0, 0, 0)
        cache.setPinnedKeys(setOf(pinned))
        cache.put(pinned, bitmap(), bytes = 100)

        // Every one of these would normally evict the pinned entry under a byte-only LRU.
        repeat(5) { i -> cache.put(key(1, i, 0), bitmap(), bytes = 100) }

        assertTrue(cache.contains(pinned), "a pinned tile must never be evicted")
    }

    @Test
    fun setPinnedKeys_reEvictsImmediately_ifTheNewBudgetCantFitTheExistingEntries() {
        val cache = TileMemoryCache(maxBytes = 150)
        cache.put(key(1, 0, 0), bitmap(), bytes = 100)
        cache.put(key(1, 1, 0), bitmap(), bytes = 100) // already over budget by normal LRU rules

        // Re-pinning a *different* key than the one that happens to still be resident shouldn't
        // resurrect anything; the cache should simply still respect its byte budget afterward.
        cache.setPinnedKeys(setOf(key(0, 0, 0)))
        assertTrue(cache.totalBytes <= 150)
    }

    @Test
    fun clearUnpinned_dropsEverythingExceptThePinnedKeys() {
        val cache = TileMemoryCache(maxBytes = 10_000)
        val pinned = key(0, 0, 0)
        cache.setPinnedKeys(setOf(pinned))
        cache.put(pinned, bitmap(), bytes = 100)
        cache.put(key(2, 3, 4), bitmap(), bytes = 100)
        cache.put(key(3, 5, 6), bitmap(), bytes = 100)

        cache.clearUnpinned()

        assertTrue(cache.contains(pinned))
        assertFalse(cache.contains(key(2, 3, 4)))
        assertFalse(cache.contains(key(3, 5, 6)))
        assertEquals(1, cache.entryCount)
    }

    @Test
    fun put_sameKeyTwice_isANoOp_doesNotDoubleCountBytes() {
        val cache = TileMemoryCache(maxBytes = 1_000)
        val key = key(1, 0, 0)
        cache.put(key, bitmap(), bytes = 100)
        cache.put(key, bitmap(), bytes = 100)
        assertEquals(100, cache.totalBytes)
        assertEquals(1, cache.entryCount)
    }
}

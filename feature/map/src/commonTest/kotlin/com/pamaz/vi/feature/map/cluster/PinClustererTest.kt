package com.pamaz.vi.feature.map.cluster

import com.pamaz.vi.core.mapview.MapViewerItem
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.TileScheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.measureTime

/**
 * [PinClusterer] unit tests (MAP-10/MAP-11's acceptance criteria). A 1000x1000 image, tile size
 * 256, gives `maxZoom = 2` ([TileScheme]'s own doubling rule: 256 -> 512 -> 1024 >= 1000) — zoom 1
 * is used as "comfortably below max" for every grouping test, so "no clustering at max zoom" has
 * a real zoom (2) left to assert separately.
 */
class PinClustererTest {
    private val scheme = TileScheme(widthPx = 1000, heightPx = 1000)
    private val clusterer = PinClusterer()
    private val viewport =
        2000f // large viewport so every pin below is on-screen regardless of center

    private fun pinAtScreen(
        id: String,
        screenX: Float,
        screenY: Float,
        category: EntryCategory = EntryCategory.WEAPON
    ): ClusterablePin {
        // zoom 1 on this scheme: scaleAt(1) = 2^(1-2) = 0.5, so imageWidthPx*0.5 = 500 scaled px;
        // center (0.5, 0.5) puts normalized (x, y) at screen (viewport/2 + (x-0.5)*500, ...).
        val scaledSize = 500f
        val half = viewport / 2f
        val x = 0.5f + (screenX - half) / scaledSize
        val y = 0.5f + (screenY - half) / scaledSize
        return ClusterablePin(
            id = id,
            position = NormalizedPoint(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f)),
            category = category
        )
    }

    private fun cluster(
        pins: List<ClusterablePin>,
        zoom: Float = 1f,
        maxZoom: Float = 2f,
        excludedPinId: String? = null,
    ): List<MapViewerItem> = clusterer.cluster(
        pins = pins,
        scheme = scheme,
        center = NormalizedPoint(0.5f, 0.5f),
        zoom = zoom,
        maxZoom = maxZoom,
        viewportWidthPx = viewport,
        viewportHeightPx = viewport,
        excludedPinId = excludedPinId,
    )

    @Test
    fun elevenClosePinsMergeIntoOneBadgeWithCountEleven() {
        val pins = (0 until 11).map { i -> pinAtScreen("p$i", 1000f + i, 1000f) }
        val items = cluster(pins)

        assertEquals(1, items.size)
        val clusterItem = assertIs<MapViewerItem.Cluster>(items.single())
        assertEquals(11, clusterItem.cluster.count)
    }

    @Test
    fun tenClosePinsStaySingle() {
        val pins = (0 until 10).map { i -> pinAtScreen("p$i", 1000f + i, 1000f) }
        val items = cluster(pins)

        assertEquals(10, items.size)
        assertTrue(items.all { it is MapViewerItem.Pin })
    }

    @Test
    fun oneHundredTwentyPinsShowNinetyNinePlus() {
        val pins = (0 until 120).map { i -> pinAtScreen("p$i", 1000f + (i % 20), 1000f + (i / 20)) }
        val items = cluster(pins)

        assertEquals(1, items.size)
        val badge = assertIs<MapViewerItem.Cluster>(items.single())
        assertEquals(120, badge.cluster.count)
        // IloveViClusterBadge (core:design) renders counts over 99 as "99+"; this class just needs
        // to report the true count so that component can decide the display text.
        assertTrue(badge.cluster.count > 99)
    }

    @Test
    fun farApartPinsStaySeparate() {
        val pins = listOf(
            pinAtScreen("near1", 1000f, 1000f),
            pinAtScreen("near2", 1010f, 1000f),
            pinAtScreen("far", 1000f + CLUSTER_DISTANCE_PX * 3, 1000f),
        )
        val items = cluster(pins)

        // The two near pins are close enough to be in one group, but a 2-pin group is below
        // CLUSTER_MIN_SIZE, so it stays as two individual pins, not a badge — three single pins
        // total, not a cluster swallowing the far one too.
        assertEquals(3, items.size)
        assertTrue(items.all { it is MapViewerItem.Pin })
        assertTrue(items.any { it is MapViewerItem.Pin && it.pin.id == "far" })
    }

    @Test
    fun badgeSplitsWhenZoomingIn() {
        // At zoom 1 (well below maxZoom 2) these 11 pins, 1px apart, merge; the test below re-runs
        // the same pins at zoom 1 but spread far apart in *screen* space by using a very different
        // zoom to prove the same normalized pins produce different groupings depending on camera.
        val pins = (0 until 11).map { i -> pinAtScreen("p$i", 1000f + i, 1000f) }
        val merged = cluster(pins, zoom = 1f)
        assertEquals(1, merged.size)
        assertIs<MapViewerItem.Cluster>(merged.single())

        // At the max zoom, clustering is skipped entirely (see [noClusteringAtMaxZoom]) — using it
        // here doubles as "zooming in enough always eventually shows every pin individually".
        val atMaxZoom = cluster(pins, zoom = 2f, maxZoom = 2f)
        assertEquals(11, atMaxZoom.size)
        assertTrue(atMaxZoom.all { it is MapViewerItem.Pin })
    }

    @Test
    fun noClusteringAtMaxZoom() {
        val pins = (0 until 50).map { i ->
            pinAtScreen(
                "p$i",
                1000f,
                1000f
            )
        } // all on the exact same point
        val items = cluster(pins, zoom = 2f, maxZoom = 2f)

        assertEquals(50, items.size)
        assertTrue(items.all { it is MapViewerItem.Pin })
    }

    @Test
    fun filtersChangeTheCount() {
        val weapons =
            (0 until 11).map { i -> pinAtScreen("w$i", 1000f + i, 1000f, EntryCategory.WEAPON) }
        val allItems = cluster(weapons)
        assertEquals(11, (assertIs<MapViewerItem.Cluster>(allItems.single())).cluster.count)

        // Simulating "sub-type filter removed half the pins" by only passing the surviving subset
        // — PinClusterer has no filtering concept itself (MAP-10's "filters applied before
        // clustering"), so this is exactly what a caller does after filtering.
        val filtered = cluster(weapons.take(9))
        assertEquals(9, filtered.size)
        assertTrue(filtered.all { it is MapViewerItem.Pin })
    }

    @Test
    fun selectedPinExcludedFromItsGroup() {
        val pins = (0 until 11).map { i -> pinAtScreen("p$i", 1000f + i, 1000f) }
        val items = cluster(pins, excludedPinId = "p0")

        // The remaining 10 pins (p1..p10) are at or below CLUSTER_MIN_SIZE, so they stay single —
        // proving the excluded pin actually left the group (otherwise this would still be an
        // 11-pin badge), and the excluded pin itself is never returned (the caller draws it).
        assertEquals(10, items.size)
        assertTrue(items.all { it is MapViewerItem.Pin })
        assertTrue(items.none { it is MapViewerItem.Pin && it.pin.id == "p0" })
    }

    @Test
    fun stableIdsForTheSameInput() {
        val pins = (0 until 11).map { i -> pinAtScreen("p$i", 1000f + i, 1000f) }
        val first = cluster(pins)
        val second = cluster(pins)

        val firstId = (assertIs<MapViewerItem.Cluster>(first.single())).cluster.id
        val secondId = (assertIs<MapViewerItem.Cluster>(second.single())).cluster.id
        assertEquals(firstId, secondId)
    }

    @Test
    fun dominantCategoryIsTheMajority() {
        val weapons =
            (0 until 8).map { i -> pinAtScreen("w$i", 1000f + i, 1000f, EntryCategory.WEAPON) }
        val vehicles =
            (0 until 3).map { i -> pinAtScreen("v$i", 1000f + 8 + i, 1000f, EntryCategory.VEHICLE) }
        val items = cluster(weapons + vehicles)

        val badge = assertIs<MapViewerItem.Cluster>(items.single())
        assertEquals(EntryCategory.WEAPON, badge.cluster.dominantCategory)
    }

    @Test
    fun tiedCategoriesHaveNoDominantCategory() {
        val weapons6 =
            (0 until 6).map { i -> pinAtScreen("w$i", 1000f + i, 1000f, EntryCategory.WEAPON) }
        val vehicles6 =
            (0 until 6).map { i -> pinAtScreen("v$i", 1000f + 6 + i, 1000f, EntryCategory.VEHICLE) }
        val items = cluster(weapons6 + vehicles6)

        val badge = assertIs<MapViewerItem.Cluster>(items.single())
        assertEquals(12, badge.cluster.count)
        assertNull(badge.cluster.dominantCategory)
    }

    @Test
    fun fiveHundredPinsClusterUnderSixteenMilliseconds() {
        val pins = (0 until 500).map { i ->
            pinAtScreen(
                "p$i",
                1000f + (i % 50) * 4f,
                1000f + (i / 50) * 4f,
                EntryCategory.entries[i % 3]
            )
        }
        // Warm up once (first call pays class-loading/JIT cost not representative of steady state).
        cluster(pins)

        val elapsed = measureTime { cluster(pins) }
        println("PinClusterer: 500 pins clustered in ${elapsed.inWholeMicroseconds} microseconds")
        assertTrue(elapsed.inWholeMilliseconds < 16, "Expected < 16ms for 500 pins, was $elapsed")
    }

    @Test
    fun twoThousandPinsClusterUnderFiftyMilliseconds() {
        val pins = (0 until 2000).map { i ->
            pinAtScreen(
                "p$i",
                1000f + (i % 100) * 4f,
                1000f + (i / 100) * 4f,
                EntryCategory.entries[i % 3]
            )
        }
        cluster(pins)

        val elapsed = measureTime { cluster(pins) }
        println("PinClusterer: 2000 pins clustered in ${elapsed.inWholeMicroseconds} microseconds")
        assertTrue(elapsed.inWholeMilliseconds < 50, "Expected < 50ms for 2000 pins, was $elapsed")
    }
}

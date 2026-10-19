package com.pamaz.vi.core.mapview

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.fail

/** [NormalizedBounds]' own validation, mirroring [com.pamaz.vi.core.model.NormalizedPoint]'s. */
class MapViewerClusterTest {

    @Test
    fun normalizedBounds_rejectsACoordinateOutsideZeroToOne() {
        assertFailsWith<IllegalArgumentException> {
            NormalizedBounds(minX = -0.1f, minY = 0f, maxX = 1f, maxY = 1f)
        }
        assertFailsWith<IllegalArgumentException> {
            NormalizedBounds(minX = 0f, minY = 0f, maxX = 1.1f, maxY = 1f)
        }
    }

    @Test
    fun normalizedBounds_rejectsMinGreaterThanMax() {
        assertFailsWith<IllegalArgumentException> {
            NormalizedBounds(minX = 0.6f, minY = 0f, maxX = 0.4f, maxY = 1f)
        }
        assertFailsWith<IllegalArgumentException> {
            NormalizedBounds(minX = 0f, minY = 0.6f, maxX = 1f, maxY = 0.4f)
        }
    }

    @Test
    fun mapViewerItem_wrapsAPinOrACluster() {
        val pin = MapViewerPin(
            id = "p1",
            position = NormalizedPoint(0.5f, 0.5f),
            category = EntryCategory.WEAPON
        )
        val cluster = MapViewerCluster(
            id = "c1",
            position = NormalizedPoint(0.5f, 0.5f),
            count = 12,
            dominantCategory = null,
            bounds = NormalizedBounds(0.4f, 0.4f, 0.6f, 0.6f),
        )

        val items: List<MapViewerItem> =
            listOf(MapViewerItem.Pin(pin), MapViewerItem.Cluster(cluster))

        when (val first = items[0]) {
            is MapViewerItem.Pin -> assertSame(pin, first.pin)
            is MapViewerItem.Cluster -> fail("expected a Pin")
        }
        when (val second = items[1]) {
            is MapViewerItem.Pin -> fail("expected a Cluster")
            is MapViewerItem.Cluster -> assertSame(cluster, second.cluster)
        }
    }
}

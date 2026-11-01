package com.pamaz.vi.feature.map.cluster

import com.pamaz.vi.core.mapview.MapViewerCluster
import com.pamaz.vi.core.mapview.MapViewerItem
import com.pamaz.vi.core.mapview.MapViewerPin
import com.pamaz.vi.core.mapview.NormalizedBounds
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.MapViewport
import com.pamaz.vi.core.model.map.ScreenPoint
import com.pamaz.vi.core.model.map.TileScheme
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Two pins closer than this, in screen pixels, are grouped together (MAP-10). Also the edge
 * length of [PinClusterer]'s spatial grid cell — see its KDoc for how the two relate. Named here,
 * in one place, specifically so it's easy to tune without hunting through the algorithm.
 */
const val CLUSTER_DISTANCE_PX = 48f

/**
 * A group with strictly more than this many pins is drawn as one [MapViewerCluster] badge;
 * groups of this size or smaller are drawn as individual [MapViewerPin]s (MAP-10). Named here, in
 * one place, specifically so it's easy to tune without hunting through the algorithm.
 */
const val CLUSTER_MIN_SIZE = 10

/** One pin [PinClusterer] can group: just enough to project it to screen space and pick a badge color/id. */
data class ClusterablePin(
    val id: String,
    val position: NormalizedPoint,
    val category: EntryCategory,
    val label: String? = null,
)

/**
 * Pure-Kotlin, UI-independent pin grouping (MAP-10/MAP-11). This class has no Compose dependency
 * and imports nothing from `:core:mapview` beyond its plain data shapes
 * ([MapViewerPin]/[MapViewerCluster]/[MapViewerItem]/[NormalizedBounds]) — it only decides *which*
 * pins belong together and what a resulting badge should show; `MapViewer` draws whatever this
 * produces and has no idea how it was computed. A caller (`feature:map`'s `MapScreen`) is expected
 * to re-invoke [cluster] whenever the zoom, the viewport size, or the set of visible pins changes
 * — this class holds no state of its own between calls, so that's purely the caller's concern
 * (typically a `remember`/`derivedStateOf` keyed on those three things).
 *
 * ## Algorithm
 * Pins are projected to screen space with the exact same [MapViewport.toScreen] conversion the
 * viewer itself uses for drawing/hit-testing, so a cluster's grouping always matches what's
 * actually rendered. They're then bucketed into a grid of [CLUSTER_DISTANCE_PX]-sized cells; for
 * each pin, only the 3x3 neighborhood of cells around it is checked for pins within
 * [CLUSTER_DISTANCE_PX] (not every other pin), which is what keeps this roughly O(n) rather than
 * O(n²). Merging uses a union-find (disjoint-set) over pin indices, so a chain of mutually-close
 * pins spanning several cells still ends up in one group, never incorrectly split at a cell
 * boundary the way naive "same cell only" bucketing would.
 *
 * A finished group becomes a [MapViewerItem.Cluster] only when it has strictly more than
 * [CLUSTER_MIN_SIZE] pins; smaller groups are returned as individual [MapViewerItem.Pin]s.
 *
 * ## Rules enforced here
 * - [cluster]'s `excludedPinId` (the current selection, if any) never takes part in grouping and
 *   is never returned — the caller always draws it separately, as its own always-on-top
 *   [MapViewerItem.Pin] (MAP-11).
 * - At `zoom >= maxZoom`, clustering is skipped entirely: every remaining pin becomes its own
 *   [MapViewerItem.Pin], so zooming all the way in always eventually shows every real pin.
 * - [MapViewerCluster.id] is built from the screen-space grid cell of the group's centroid
 *   (`"cluster:<cellX>:<cellY>:<size>"`), never randomly — the same input (same pins, same camera)
 *   always produces the same id, satisfying the "stable ids" requirement without needing to track
 *   cluster identity across calls.
 * - [MapViewerCluster.dominantCategory] is whichever category has strictly more pins than every
 *   other category in the group; `null` (drawn in the neutral brand color by `IloveViClusterBadge`)
 *   when two or more categories tie for the most pins.
 */
class PinClusterer {

    /**
     * Groups [pins] (already filtered to the active category/sub-type filters — this class has
     * no filtering concept of its own) into [MapViewerItem]s for the current camera.
     *
     * @param scheme the tile scheme the [center]/[zoom]/viewport math is relative to.
     * @param maxZoom the map's maximum zoom; at or above it, no clustering is applied.
     * @param excludedPinId the selected pin, if any — excluded from every group (MAP-11).
     */
    fun cluster(
        pins: List<ClusterablePin>,
        scheme: TileScheme,
        center: NormalizedPoint,
        zoom: Float,
        maxZoom: Float,
        viewportWidthPx: Float,
        viewportHeightPx: Float,
        excludedPinId: String? = null,
    ): List<MapViewerItem> {
        val groupable = if (excludedPinId == null) pins else pins.filter { it.id != excludedPinId }
        if (groupable.isEmpty()) return emptyList()

        if (zoom >= maxZoom || viewportWidthPx <= 0f || viewportHeightPx <= 0f) {
            return groupable.map { it.toSingleItem() }
        }

        val screenPositions = groupable.map { pin ->
            MapViewport.toScreen(
                pin.position,
                scheme,
                center,
                zoom,
                viewportWidthPx,
                viewportHeightPx
            )
        }

        val groups = groupByProximity(screenPositions)

        val items = ArrayList<MapViewerItem>(groups.size)
        for (group in groups) {
            if (group.size > CLUSTER_MIN_SIZE) {
                items += buildCluster(group, groupable, screenPositions)
            } else {
                for (index in group) items += groupable[index].toSingleItem()
            }
        }
        return items
    }

    /** Grid + 8-neighbor union-find grouping — see the class KDoc. Returns index groups into [positions]. */
    private fun groupByProximity(positions: List<ScreenPoint>): List<List<Int>> {
        fun cellOf(point: ScreenPoint): Long {
            val cx = floor(point.x / CLUSTER_DISTANCE_PX).toInt()
            val cy = floor(point.y / CLUSTER_DISTANCE_PX).toInt()
            return packCell(cx, cy)
        }

        val grid = HashMap<Long, MutableList<Int>>()
        positions.forEachIndexed { index, point ->
            grid.getOrPut(cellOf(point)) { mutableListOf() }.add(index)
        }

        val parent = IntArray(positions.size) { it }
        fun find(x: Int): Int {
            var root = x
            while (parent[root] != root) root = parent[root]
            var cur = x
            while (parent[cur] != root) {
                val next = parent[cur]
                parent[cur] = root
                cur = next
            }
            return root
        }

        fun union(a: Int, b: Int) {
            val ra = find(a)
            val rb = find(b)
            if (ra != rb) parent[ra] = rb
        }

        positions.forEachIndexed { index, point ->
            val cx = floor(point.x / CLUSTER_DISTANCE_PX).toInt()
            val cy = floor(point.y / CLUSTER_DISTANCE_PX).toInt()
            for (dx in -1..1) {
                for (dy in -1..1) {
                    val neighbors = grid[packCell(cx + dx, cy + dy)] ?: continue
                    for (other in neighbors) {
                        if (other <= index) continue
                        val op = positions[other]
                        val distance =
                            hypot((point.x - op.x).toDouble(), (point.y - op.y).toDouble())
                        if (distance <= CLUSTER_DISTANCE_PX) union(index, other)
                    }
                }
            }
        }

        val byRoot = LinkedHashMap<Int, MutableList<Int>>()
        for (index in positions.indices) {
            byRoot.getOrPut(find(index)) { mutableListOf() }.add(index)
        }
        return byRoot.values.toList()
    }

    private fun buildCluster(
        group: List<Int>,
        pins: List<ClusterablePin>,
        screenPositions: List<ScreenPoint>,
    ): MapViewerItem.Cluster {
        var sumX = 0f
        var sumY = 0f
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var sumScreenX = 0f
        var sumScreenY = 0f
        val counts = LinkedHashMap<EntryCategory, Int>()

        for (index in group) {
            val pin = pins[index]
            sumX += pin.position.x
            sumY += pin.position.y
            minX = min(minX, pin.position.x)
            minY = min(minY, pin.position.y)
            maxX = max(maxX, pin.position.x)
            maxY = max(maxY, pin.position.y)
            counts[pin.category] = (counts[pin.category] ?: 0) + 1

            val screen = screenPositions[index]
            sumScreenX += screen.x
            sumScreenY += screen.y
        }

        val size = group.size
        val centroid = NormalizedPoint(sumX / size, sumY / size)
        val bounds = NormalizedBounds(minX = minX, minY = minY, maxX = maxX, maxY = maxY)

        val maxCount = counts.values.max()
        val topCategories = counts.filterValues { it == maxCount }.keys
        val dominantCategory = if (topCategories.size == 1) topCategories.first() else null

        val cellX = floor((sumScreenX / size) / CLUSTER_DISTANCE_PX).toInt()
        val cellY = floor((sumScreenY / size) / CLUSTER_DISTANCE_PX).toInt()
        val id = "cluster:$cellX:$cellY:$size"

        return MapViewerItem.Cluster(
            MapViewerCluster(
                id = id,
                position = centroid,
                count = size,
                dominantCategory = dominantCategory,
                bounds = bounds,
            )
        )
    }

    private fun ClusterablePin.toSingleItem(): MapViewerItem.Pin =
        MapViewerItem.Pin(
            MapViewerPin(
                id = id,
                position = position,
                category = category,
                label = label
            )
        )

    /** Packs two cell coordinates into one [Long] key, avoiding a `Pair<Int, Int>` allocation per grid lookup. */
    private fun packCell(cellX: Int, cellY: Int): Long =
        (cellX.toLong() shl 32) or (cellY.toLong() and 0xFFFFFFFFL)
}

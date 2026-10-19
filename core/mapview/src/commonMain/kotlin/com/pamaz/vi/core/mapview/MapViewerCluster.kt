package com.pamaz.vi.core.mapview

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint

/**
 * A normalized (`0f..1f` on both axes), axis-aligned rectangle — [MapViewerCluster.bounds]' shape,
 * and the input to [MapViewerState.animateToBounds]. Out-of-range coordinates are rejected at
 * construction (see [init]), the same choice [NormalizedPoint] makes and for the same reason: a
 * bound outside `0f..1f` means the upstream computation used the wrong image size, and clamping
 * would hide that bug instead of surfacing it.
 */
data class NormalizedBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
) {
    init {
        require(minX in 0f..1f && minY in 0f..1f && maxX in 0f..1f && maxY in 0f..1f) {
            "NormalizedBounds coordinates must be in 0f..1f, was minX=$minX minY=$minY maxX=$maxX maxY=$maxY"
        }
        require(minX <= maxX) { "minX ($minX) must be <= maxX ($maxX)" }
        require(minY <= maxY) { "minY ($minY) must be <= maxY ($maxY)" }
    }
}

/**
 * One cluster [MapViewer] draws: several nearby [MapViewerPin]s merged into a single badge at
 * [position] (typically their centroid), showing [count] and tinted by [dominantCategory] (`null`
 * for a mixed-category cluster, drawn in `:core:designsystem`'s `IloveViClusterBadge` neutral
 * brand color). [bounds] is the normalized rectangle the clustered pins span — what
 * [MapViewerState.animateToBounds] zooms to when this cluster is tapped, so "zoom in on this
 * cluster" shows every pin it was standing in for, not just [position].
 *
 * Like [MapViewerPin], this is the viewer's own shape, not a domain type: `feature:map` owns all
 * clustering logic (which pins merge into a cluster, at which zoom, the distance threshold) and
 * simply hands this viewer the result as a [MapViewerItem.Cluster] — this module has no clustering
 * concept at all, it only knows how to draw and hit-test one.
 */
data class MapViewerCluster(
    val id: String,
    val position: NormalizedPoint,
    val count: Int,
    val dominantCategory: EntryCategory?,
    val bounds: NormalizedBounds,
)

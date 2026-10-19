package com.pamaz.vi.core.mapview

/**
 * Everything [MapViewer] can draw on its single overlay layer: either one [MapViewerPin] or one
 * [MapViewerCluster]. Mirrors [MapContent]'s own sealed-interface-over-a-wrapped-value shape for
 * the same reason: callers (`feature:map`) mix pins and clusters freely in one `List<MapViewerItem>`
 * — whichever pins a given zoom/viewport didn't merge into a cluster, plus one entry per cluster it
 * did — without this module needing a third, synthetic marker type or two parallel lists to stay
 * in sync.
 */
sealed interface MapViewerItem {
    data class Pin(val pin: MapViewerPin) : MapViewerItem
    data class Cluster(val cluster: MapViewerCluster) : MapViewerItem
}

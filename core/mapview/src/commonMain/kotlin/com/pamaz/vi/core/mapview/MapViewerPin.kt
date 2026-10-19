package com.pamaz.vi.core.mapview

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint

/**
 * One marker [MapViewer] draws: a [position] on the map, a [category] (which picks the pink/
 * orange/teal color and icon from the design system) and an optional short [label].
 *
 * This is the viewer's own marker type, not [com.pamaz.vi.core.model.MapPin] — it carries no
 * [com.pamaz.vi.core.model.EntryRef], database id shape, `mapVersion` or `updatedAt`, only what's
 * needed to draw and hit-test a dot on screen. Callers (`feature:map`) map their domain `MapPin`s
 * into this shape, so the viewer never needs to know about entries, repositories or sync.
 *
 * Clustering ([MapViewerCluster]) is a *separate* item kind, not a special [MapViewerPin] — see
 * [MapViewerItem]'s KDoc for why a caller mixes both freely in one list rather than this type
 * doubling as both a single marker and a cluster bubble.
 */
data class MapViewerPin(
    val id: String,
    val position: NormalizedPoint,
    val category: EntryCategory,
    val label: String? = null,
)

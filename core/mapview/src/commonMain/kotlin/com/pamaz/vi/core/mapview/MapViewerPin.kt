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
 * [id] is also how clustering is hooked in: `feature:map` owns all clustering logic and simply
 * hands this viewer one [MapViewerPin] per cluster (a representative [position], a [category] —
 * or a neutral one for a mixed-category cluster — and a [label] such as `"12"`), then supplies a
 * custom `pinContent` to [MapViewer] to draw cluster bubbles differently from single pins. The
 * viewer itself has no cluster concept at all.
 */
data class MapViewerPin(
    val id: String,
    val position: NormalizedPoint,
    val category: EntryCategory,
    val label: String? = null,
)

package com.pamaz.vi.tools.mapeditor.geometry

import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.MapViewport
import com.pamaz.vi.core.model.map.ScreenPoint
import com.pamaz.vi.core.model.map.TileScheme

/**
 * A plain click (PLT-04's "a click on the map creates/moves the pin") is already a solved problem
 * one level down: `:core:mapview`'s `MapViewer.onMapTap` hands this tool a [NormalizedPoint]
 * computed via [MapViewport.toNormalized] — the exact same conversion this function wraps, kept
 * here only so this module's own tests exercise it directly (several zooms/viewport sizes) without
 * depending on `core:mapview`'s private pointer-input plumbing.
 */
internal fun normalizedPointForClick(
    screen: ScreenPoint,
    scheme: TileScheme,
    center: NormalizedPoint,
    zoom: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
): NormalizedPoint =
    MapViewport.toNormalized(screen, scheme, center, zoom, viewportWidthPx, viewportHeightPx)

/**
 * Where a pin ends up after being dragged ([dxPx]/[dyPx], screen pixels) or nudged by an arrow
 * key — PLT-04's "drag to move, arrow keys for fine adjustment". Unlike
 * `core:mapview`'s `panCenter` (which moves the *camera*, so screen-right has to mean
 * normalized-left to keep the dragged point under the finger), this moves the *pin itself*:
 * screen-right means normalized-right, no sign flip, and the result is clamped to
 * [NormalizedPoint]'s own `0f..1f` range directly — there's no "camera edge" concept for a single
 * point the way there is for [com.pamaz.vi.core.mapview.centerBoundsForAxis].
 */
internal fun movePinByScreenDelta(
    currentPosition: NormalizedPoint,
    scheme: TileScheme,
    zoom: Float,
    dxPx: Float,
    dyPx: Float,
): NormalizedPoint {
    val scale = scheme.scaleAt(zoom)
    val scaledWidth = (scheme.widthPx * scale).toFloat()
    val scaledHeight = (scheme.heightPx * scale).toFloat()
    val newX = (currentPosition.x + dxPx / scaledWidth).coerceIn(0f, 1f)
    val newY = (currentPosition.y + dyPx / scaledHeight).coerceIn(0f, 1f)
    return NormalizedPoint(newX, newY)
}

/** Arrow-key nudge step in screen pixels: [NudgeStepPx] normally, [NudgeStepPxLarge] with Shift held. */
internal const val NudgeStepPx = 1f
internal const val NudgeStepPxLarge = 10f

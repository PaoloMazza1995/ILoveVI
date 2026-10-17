package com.pamaz.vi.core.mapview

import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.MapViewport
import com.pamaz.vi.core.model.map.ScreenPoint
import com.pamaz.vi.core.model.map.TileScheme

/**
 * The minimum side length of a pin's tappable area, regardless of how big it's drawn — the
 * platform-independent touch-target floor this product uses everywhere pins are hit-tested.
 */
internal const val MIN_HIT_TARGET_PX_AT_DENSITY_1X = 44f

/**
 * The new center that keeps [focalScreen] pointing at the same map location after zooming from
 * [currentZoom] to [newZoom] — the standard "zoom around your finger/cursor" behavior used by both
 * pinch-zoom and scroll-wheel/double-tap zoom.
 *
 * Derived directly from [MapViewport.toNormalized]'s own formula
 * (`point = center + (screen - viewportSize/2) / scaledSize`), solved for `center` instead of
 * `point`: first find the normalized point currently under [focalScreen] (at the *old*
 * zoom/center), then find the center that places that same point under that same screen position
 * at the *new* zoom.
 */
internal fun computeFocalZoomCenter(
    scheme: TileScheme,
    currentCenter: NormalizedPoint,
    currentZoom: Float,
    newZoom: Float,
    focalScreen: ScreenPoint,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
): NormalizedPoint {
    val focalPoint = MapViewport.toNormalized(
        focalScreen, scheme, currentCenter, currentZoom, viewportWidthPx, viewportHeightPx,
    )

    val scale = scheme.scaleAt(newZoom)
    val scaledWidth = (scheme.widthPx * scale).toFloat()
    val scaledHeight = (scheme.heightPx * scale).toFloat()

    val newX = focalPoint.x - (focalScreen.x - viewportWidthPx / 2f) / scaledWidth
    val newY = focalPoint.y - (focalScreen.y - viewportHeightPx / 2f) / scaledHeight
    return NormalizedPoint(newX.coerceIn(0f, 1f), newY.coerceIn(0f, 1f))
}

/**
 * The new center after panning the viewport by ([dxPx], [dyPx]) screen pixels (e.g. a drag delta)
 * at [zoom] — dragging right/down slides the map right/down under the finger, so the point shown
 * at the viewport's center moves the opposite way.
 */
internal fun panCenter(
    scheme: TileScheme,
    currentCenter: NormalizedPoint,
    zoom: Float,
    dxPx: Float,
    dyPx: Float,
): NormalizedPoint {
    val scale = scheme.scaleAt(zoom)
    val scaledWidth = (scheme.widthPx * scale).toFloat()
    val scaledHeight = (scheme.heightPx * scale).toFloat()

    val newX = currentCenter.x - dxPx / scaledWidth
    val newY = currentCenter.y - dyPx / scaledHeight
    return NormalizedPoint(newX.coerceIn(0f, 1f), newY.coerceIn(0f, 1f))
}

/**
 * The topmost [MapViewerPin.id] whose tap target (at least [MIN_HIT_TARGET_PX_AT_DENSITY_1X]
 * square, centered on its screen position) contains [tapScreen], or `null` if none does. Pins
 * later in [pins] are checked first (and therefore win on overlap), matching the draw order
 * [MapViewer] uses — the last pin drawn is the topmost one on screen.
 */
internal fun hitTestPin(
    pins: List<MapViewerPin>,
    tapScreen: ScreenPoint,
    scheme: TileScheme,
    center: NormalizedPoint,
    zoom: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    minTargetPx: Float = MIN_HIT_TARGET_PX_AT_DENSITY_1X,
): String? {
    val halfTarget = minTargetPx / 2f
    for (pin in pins.asReversed()) {
        val screen = MapViewport.toScreen(
            pin.position, scheme, center, zoom, viewportWidthPx, viewportHeightPx,
        )
        val withinX = tapScreen.x in (screen.x - halfTarget)..(screen.x + halfTarget)
        val withinY = tapScreen.y in (screen.y - halfTarget)..(screen.y + halfTarget)
        if (withinX && withinY) return pin.id
    }
    return null
}

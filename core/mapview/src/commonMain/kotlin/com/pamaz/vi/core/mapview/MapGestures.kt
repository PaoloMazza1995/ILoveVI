package com.pamaz.vi.core.mapview

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.MapViewport
import com.pamaz.vi.core.model.map.ScreenPoint
import com.pamaz.vi.core.model.map.TileScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.log2

/** Zoom levels per mouse-wheel notch; a notch is usually `scrollDelta.y == ±1`. */
private const val SCROLL_ZOOM_PER_NOTCH = 0.5f

/** Zoom levels a double-tap adds, focused on the tapped point. */
private const val DOUBLE_TAP_ZOOM_STEP = 1f

/**
 * Pan, pinch-zoom and fling: one combined gesture detector (rather than the stock
 * `detectTransformGestures`) so a [VelocityTracker] can be fed real pointer timestamps for the
 * fling at gesture end — `detectTransformGestures` doesn't expose those itself. Live tracking
 * uses [MapViewerState.snapTo] (1:1 with the finger); release hands off to
 * [MapViewerState.flingBy].
 */
private fun Modifier.panZoomFlingGestures(
    state: MapViewerState,
    scheme: TileScheme,
    coroutineScope: CoroutineScope,
): Modifier = pointerInput(state, scheme) {
    awaitEachGesture {
        // awaitEachGesture's scope is @RestrictsSuspension: state's own suspend functions (which
        // aren't extensions on that scope) can only be reached via launch, never called directly.
        coroutineScope.launch { state.stopCenterMotion() }
        val velocityTracker = VelocityTracker()
        var center = state.center
        var zoom = state.zoom

        do {
            val event = awaitPointerEvent()
            if (event.changes.any { it.isConsumed }) break

            val zoomChange = event.calculateZoom()
            val panChange = event.calculatePan()
            if (zoomChange != 1f || panChange != Offset.Zero) {
                val viewportWidth = state.viewportSize.width.toFloat()
                val viewportHeight = state.viewportSize.height.toFloat()

                center = if (zoomChange != 1f) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val newZoom = (zoom + log2(zoomChange)).coerceIn(state.minZoom, state.maxZoom)
                    val focal = ScreenPoint(centroid.x, centroid.y)
                    val newCenter = computeFocalZoomCenter(
                        scheme,
                        center,
                        zoom,
                        newZoom,
                        focal,
                        viewportWidth,
                        viewportHeight
                    )
                    zoom = newZoom
                    newCenter
                } else {
                    panCenter(
                        scheme,
                        center,
                        zoom,
                        panChange.x,
                        panChange.y,
                        viewportWidth,
                        viewportHeight
                    )
                }

                val liveCenter = center
                val liveZoom = zoom
                val liveScale = scheme.scaleAt(liveZoom)
                val boundsX =
                    centerBoundsForAxis((scheme.widthPx * liveScale).toFloat(), viewportWidth)
                val boundsY =
                    centerBoundsForAxis((scheme.heightPx * liveScale).toFloat(), viewportHeight)
                coroutineScope.launch { state.snapTo(liveCenter, liveZoom, boundsX, boundsY) }
                event.changes.forEach { it.consume() }
                val change = event.changes.first()
                velocityTracker.addPosition(change.uptimeMillis, change.position)
            }
        } while (event.changes.any { it.pressed })

        val velocity = velocityTracker.calculateVelocity()
        val scale = scheme.scaleAt(zoom)
        val scaledWidth = (scheme.widthPx * scale).toFloat()
        val scaledHeight = (scheme.heightPx * scale).toFloat()
        val flingVelocityX = -velocity.x / scaledWidth
        val flingVelocityY = -velocity.y / scaledHeight
        val viewportWidth = state.viewportSize.width.toFloat()
        val viewportHeight = state.viewportSize.height.toFloat()
        val boundsX = centerBoundsForAxis(scaledWidth, viewportWidth)
        val boundsY = centerBoundsForAxis(scaledHeight, viewportHeight)
        coroutineScope.launch { state.flingBy(flingVelocityX, flingVelocityY, boundsX, boundsY) }
    }
}

/**
 * Tap and double-tap-to-zoom-in, focused on the tapped point. A tap hit-tests pins first (they're
 * always drawn on top, see [PinLayer]), then clusters, falling back to [onMapTap] if neither hits.
 */
private fun Modifier.tapGestures(
    state: MapViewerState,
    scheme: TileScheme,
    items: () -> List<MapViewerItem>,
    onPinTap: (String) -> Unit,
    onClusterTap: (MapViewerCluster) -> Unit,
    onMapTap: (NormalizedPoint) -> Unit,
    coroutineScope: CoroutineScope,
): Modifier = pointerInput(state, scheme) {
    // 44dp converted at the real ambient density, matching PinLayer's own hit-target size —
    // MIN_HIT_TARGET_PX_AT_DENSITY_1X's bare-Float default only holds at density 1x.
    val minTargetPx = 44.dp.toPx()
    detectTapGestures(
        onTap = { offset ->
            val viewportWidth = state.viewportSize.width.toFloat()
            val viewportHeight = state.viewportSize.height.toFloat()
            val screen = ScreenPoint(offset.x, offset.y)
            val currentItems = items()
            val pins = currentItems.mapNotNull { (it as? MapViewerItem.Pin)?.pin }
            val clusters = currentItems.mapNotNull { (it as? MapViewerItem.Cluster)?.cluster }

            val hitPinId = hitTestPin(
                pins, screen, scheme, state.center, state.zoom, viewportWidth, viewportHeight,
                minTargetPx = minTargetPx,
            )
            val hitClusterId = if (hitPinId == null) {
                hitTestCluster(
                    clusters,
                    screen,
                    scheme,
                    state.center,
                    state.zoom,
                    viewportWidth,
                    viewportHeight,
                    minTargetPx = minTargetPx,
                )
            } else {
                null
            }

            when {
                hitPinId != null -> onPinTap(hitPinId)
                hitClusterId != null -> clusters.find { it.id == hitClusterId }?.let(onClusterTap)
                else -> onMapTap(
                    MapViewport.toNormalized(
                        screen,
                        scheme,
                        state.center,
                        state.zoom,
                        viewportWidth,
                        viewportHeight
                    )
                )
            }
        },
        onDoubleTap = { offset ->
            val viewportWidth = state.viewportSize.width.toFloat()
            val viewportHeight = state.viewportSize.height.toFloat()
            val newZoom = (state.zoom + DOUBLE_TAP_ZOOM_STEP).coerceIn(state.minZoom, state.maxZoom)
            val focal = ScreenPoint(offset.x, offset.y)
            val newCenter = computeFocalZoomCenter(
                scheme,
                state.center,
                state.zoom,
                newZoom,
                focal,
                viewportWidth,
                viewportHeight
            )
            val newScale = scheme.scaleAt(newZoom)
            val boundsX = centerBoundsForAxis((scheme.widthPx * newScale).toFloat(), viewportWidth)
            val boundsY =
                centerBoundsForAxis((scheme.heightPx * newScale).toFloat(), viewportHeight)
            coroutineScope.launch { state.animateTo(newCenter, newZoom, boundsX, boundsY) }
        },
    )
}

/** Desktop scroll-wheel zoom, focused on the cursor position. A no-op on touch-only platforms, which never emit [PointerEventType.Scroll]. */
private fun Modifier.scrollWheelZoomGestures(
    state: MapViewerState,
    scheme: TileScheme,
    coroutineScope: CoroutineScope,
): Modifier = pointerInput(state, scheme) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type != PointerEventType.Scroll) continue
            val change = event.changes.first()

            val viewportWidth = state.viewportSize.width.toFloat()
            val viewportHeight = state.viewportSize.height.toFloat()
            val newZoom = (state.zoom - change.scrollDelta.y * SCROLL_ZOOM_PER_NOTCH).coerceIn(
                state.minZoom,
                state.maxZoom
            )
            val focal = ScreenPoint(change.position.x, change.position.y)
            val newCenter = computeFocalZoomCenter(
                scheme,
                state.center,
                state.zoom,
                newZoom,
                focal,
                viewportWidth,
                viewportHeight
            )

            val newScale = scheme.scaleAt(newZoom)
            val boundsX = centerBoundsForAxis((scheme.widthPx * newScale).toFloat(), viewportWidth)
            val boundsY =
                centerBoundsForAxis((scheme.heightPx * newScale).toFloat(), viewportHeight)
            coroutineScope.launch { state.snapTo(newCenter, newZoom, boundsX, boundsY) }
            change.consume()
        }
    }
}

/**
 * Installs every [MapViewer] gesture: pan, pinch-zoom, fling, double-tap-zoom, tap (pin hit-test
 * then [onMapTap]) and desktop scroll-wheel zoom. Split into three [Modifier.pointerInput] blocks
 * (transform+fling, tap+double-tap, scroll) rather than one, so none of them has to re-implement
 * the others' well-tested detection logic (`detectTapGestures` for taps, a custom loop only where
 * velocity tracking requires it).
 *
 * **Order matters here.** Chained `pointerInput` modifiers on one node dispatch
 * [androidx.compose.ui.input.pointer.PointerEventPass.Main] — the pass every gesture detector
 * used here reads from — innermost-first, bubbling out to the outermost; "innermost" means
 * *last* in this `.then()` chain. `detectTapGestures` (inside [tapGestures])
 * unconditionally consumes the initial down change as part of its own press bookkeeping, even for
 * a gesture that turns out to be a drag. [panZoomFlingGestures]'s loop bails out the instant it
 * sees an already-consumed change (`if (event.changes.any { it.isConsumed }) break`), so if it
 * were dispatched *after* [tapGestures] had already consumed that first down, it would exit before
 * a single move event arrived — panning would never work at all. Listing [panZoomFlingGestures]
 * *last* makes it innermost, so it always reads every change while still unconsumed; once real
 * movement starts, it consumes the change itself, which correctly cancels [tapGestures]' own
 * `waitForUpOrCancellation` (it treats a consumed change as "gesture canceled, not a tap").
 * [scrollWheelZoomGestures] only ever acts on [androidx.compose.ui.input.pointer.PointerEventType.Scroll]
 * events the other two ignore, so its position in the chain doesn't matter for this ordering.
 */
internal fun Modifier.mapViewerGestures(
    state: MapViewerState,
    scheme: TileScheme,
    items: () -> List<MapViewerItem>,
    onPinTap: (String) -> Unit,
    onClusterTap: (MapViewerCluster) -> Unit,
    onMapTap: (NormalizedPoint) -> Unit,
    coroutineScope: CoroutineScope,
): Modifier = this
    .tapGestures(state, scheme, items, onPinTap, onClusterTap, onMapTap, coroutineScope)
    .scrollWheelZoomGestures(state, scheme, coroutineScope)
    .panZoomFlingGestures(state, scheme, coroutineScope)

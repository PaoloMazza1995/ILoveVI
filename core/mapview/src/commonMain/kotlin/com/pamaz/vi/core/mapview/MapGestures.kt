package com.pamaz.vi.core.mapview

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.MapViewport
import com.pamaz.vi.core.model.map.ScreenPoint
import com.pamaz.vi.core.model.map.TileScheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.math.log2

/** Zoom levels per mouse-wheel notch; a notch is usually `scrollDelta.y == ±1`. */
private const val SCROLL_ZOOM_PER_NOTCH = 0.5f

/** Zoom levels a double-tap adds, focused on the tapped point. */
private const val DOUBLE_TAP_ZOOM_STEP = 1f

/** One pending camera update produced by a touch-move event — see [panZoomFlingGestures]'s KDoc. */
private data class PendingCameraSnap(
    val center: NormalizedPoint,
    val zoom: Float,
    val boundsX: ClosedFloatingPointRange<Float>,
    val boundsY: ClosedFloatingPointRange<Float>,
)

/**
 * Pan, pinch-zoom and fling: one combined gesture detector (rather than the stock
 * `detectTransformGestures`) so a [VelocityTracker] can be fed real pointer timestamps for the
 * fling at gesture end — `detectTransformGestures` doesn't expose those itself. Live tracking
 * uses [MapViewerState.snapTo] (1:1 with the finger); release hands off to
 * [MapViewerState.flingBy].
 *
 * A touch-move event stream can report updates faster than the display can actually show new
 * frames. The previous implementation called `coroutineScope.launch { state.snapTo(...) }` for
 * *every single event*, each one a brand-new child coroutine (and, before a matching fix in
 * [MapViewerState.snapTo] itself, three more nested ones inside that) — real, measured overhead
 * paid on a genuinely hot path, a concrete source of the "panning feels slow" reports against this
 * screen. [pendingSnaps] replaces that with exactly one consumer coroutine per *gesture*, fed
 * through a `CONFLATED` channel: `trySend` never suspends and always keeps only the latest value,
 * so the consumer applies camera updates as fast as it can actually keep up, automatically
 * coalescing/dropping intermediate values once the producer (touch events) outpaces it (frame
 * rendering) — exactly the "camera position per frame", not "camera position per raw touch
 * sample", this really needs. [pendingSnaps] is closed (not cancelled) once the gesture ends, so
 * its consumer coroutine finishes applying whatever's left in the channel and exits on its own
 * rather than being torn down mid-update — `cancel()` could leave [state] one update behind right
 * as the fling animation takes over from it. The consumer isn't explicitly awaited before
 * computing the fling below (`Job.join()` isn't callable here — `awaitEachGesture`'s scope is
 * `@RestrictsSuspension`, same reason [state]'s own suspend functions need `launch` too): the fling
 * itself is computed from this function's local `center`/`zoom`/`velocityTracker`, never from
 * [state] directly, so the very last queued snap finishing a frame late is, at most, an
 * imperceptible single-frame lag on [state]'s own externally-observed position, not a correctness
 * issue for the fling calculation itself.
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

        val pendingSnaps = Channel<PendingCameraSnap>(capacity = Channel.CONFLATED)
        coroutineScope.launch {
            for (snap in pendingSnaps) {
                state.snapTo(snap.center, snap.zoom, snap.boundsX, snap.boundsY)
            }
        }
        // True only once real pan/pinch movement is actually detected below — a plain tap (and
        // the first, or only, tap of a double-tap: this detector sees it too, since it runs
        // alongside `tapGestures`, see `mapViewerGestures`'s KDoc on why) never sets this. Gates
        // the end-of-gesture fling further down: launching a `state.flingBy(...)` with essentially
        // zero velocity for a gesture that never moved is both pointless and a real race —
        // `tapGestures`'s `onDoubleTap` can be mutating this same `state` (zooming in) at roughly
        // the same moment the second tap's "gesture" here is wrapping up, and a reported bug
        // ("double-tapping the map resets the zoom") is consistent with exactly that race. A tap
        // that never moved has nothing to fling in the first place, so skipping it outright
        // removes the race rather than trying to win it.
        var hasMoved = false

        do {
            val event = awaitPointerEvent()
            if (event.changes.any { it.isConsumed }) break

            val zoomChange = event.calculateZoom()
            val panChange = event.calculatePan()
            if (zoomChange != 1f || panChange != Offset.Zero) {
                hasMoved = true
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
                pendingSnaps.trySend(PendingCameraSnap(liveCenter, liveZoom, boundsX, boundsY))
                event.changes.forEach { it.consume() }
                val change = event.changes.first()
                velocityTracker.addPosition(change.uptimeMillis, change.position)
            }
        } while (event.changes.any { it.pressed })

        pendingSnaps.close()

        if (hasMoved) {
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
            coroutineScope.launch {
                state.flingBy(flingVelocityX, flingVelocityY, boundsX, boundsY)
            }
        }
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
 * Rectangle (marquee) select for a drag that starts on empty map background — opt-in via
 * [enabled] (default `false`, zero behavior change) for `:tools:map-editor`'s Select mode (step 5B
 * extension); every other caller never installs this block at all. Dispatched at
 * [PointerEventPass.Initial] on the *outer* `Box` in [MapViewer], which Compose runs strictly
 * before any Main-pass detector below it in the chain — [tapGestures], [panZoomFlingGestures], and
 * each pin's own per-pin drag `pointerInput` inside [PinLayer] — ever sees the same event. That
 * lets this block hit-test "did this drag start on a pin" itself ([hitTestPin], the same check
 * [tapGestures] uses) and bail out immediately (consuming nothing) when it did, so an existing
 * pin's own drag-to-move/tap keeps working completely unchanged. When it didn't hit a pin and the
 * pointer actually moves, this block starts consuming — which, via the exact same
 * already-consumed-change check [tapGestures]/[panZoomFlingGestures] use to cancel *each other*,
 * correctly suppresses both the tap and the pan/zoom for the rest of this gesture: a plain
 * click-with-no-movement on the background still falls through to [tapGestures]' own [onMapTap]
 * untouched, only a real drag is redirected to [onDragStart]/[onDrag]/[onDragEnd].
 *
 * Panning by plain background drag is therefore unavailable while [enabled] — `:tools:map-editor`
 * accepts this trade-off for Select mode (scroll-wheel/double-tap zoom and Place mode's drag-pan
 * both still work); see that module's README.
 */
private fun Modifier.backgroundDragSelectGestures(
    enabled: Boolean,
    state: MapViewerState,
    scheme: TileScheme,
    items: () -> List<MapViewerItem>,
    onDragStart: (NormalizedPoint) -> Unit,
    onDrag: (NormalizedPoint) -> Unit,
    onDragEnd: () -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(state, scheme) {
        val minTargetPx = 44.dp.toPx()
        awaitEachGesture {
            val firstChange = awaitPointerEvent(PointerEventPass.Initial).changes.first()
            val viewportWidth = state.viewportSize.width.toFloat()
            val viewportHeight = state.viewportSize.height.toFloat()
            val pins = items().mapNotNull { (it as? MapViewerItem.Pin)?.pin }
            val hitPinId = hitTestPin(
                pins,
                ScreenPoint(firstChange.position.x, firstChange.position.y),
                scheme, state.center, state.zoom, viewportWidth, viewportHeight,
                minTargetPx = minTargetPx,
            )
            if (hitPinId != null) return@awaitEachGesture

            var dragStarted = false
            var event = awaitPointerEvent(PointerEventPass.Initial)
            while (event.changes.any { it.pressed }) {
                val change = event.changes.first()
                val point = MapViewport.toNormalized(
                    ScreenPoint(change.position.x, change.position.y),
                    scheme, state.center, state.zoom, viewportWidth, viewportHeight,
                )
                if (!dragStarted) {
                    dragStarted = true
                    onDragStart(point)
                } else {
                    onDrag(point)
                }
                event.changes.forEach { it.consume() }
                event = awaitPointerEvent(PointerEventPass.Initial)
            }
            if (dragStarted) onDragEnd()
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
    backgroundDragSelectEnabled: Boolean = false,
    onBackgroundDragStart: (NormalizedPoint) -> Unit = {},
    onBackgroundDrag: (NormalizedPoint) -> Unit = {},
    onBackgroundDragEnd: () -> Unit = {},
): Modifier = this
    .backgroundDragSelectGestures(
        backgroundDragSelectEnabled, state, scheme, items,
        onBackgroundDragStart, onBackgroundDrag, onBackgroundDragEnd,
    )
    .tapGestures(state, scheme, items, onPinTap, onClusterTap, onMapTap, coroutineScope)
    .scrollWheelZoomGestures(state, scheme, coroutineScope)
    .panZoomFlingGestures(state, scheme, coroutineScope)

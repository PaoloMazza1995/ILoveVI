package com.pamaz.vi.core.mapview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.TileScheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.log2
import kotlin.math.min

/**
 * Normalized-unit/second velocity below which a fling is considered stopped. Normalized units are
 * tiny (the whole map is `1.0`), so the default [exponentialDecay] threshold (tuned for pixels)
 * would stop a fling almost instantly; this is small enough to let a fling glide for a natural
 * distance at any zoom level.
 */
private const val FLING_STOP_VELOCITY = 0.00005f

/**
 * [animateTo]'s duration: an explicit [tween] rather than the default spring, so the animation
 * has a known, fixed length — both for predictable UX and so it can be driven deterministically
 * in tests (see `MapViewerStateTest`) instead of running until a physics-based velocity threshold.
 */
private const val ANIMATE_TO_DURATION_MS = 300

/**
 * Holds [MapViewer]'s camera: [center], [zoom] and the last-measured [viewportSize]. Owns no
 * [com.pamaz.vi.core.model.MapConfig] or tile/network state — it is pure camera math plus Compose
 * animation plumbing, which is what makes [animateTo] (and the pan/fling/focal-zoom used
 * internally by [MapViewer]'s gesture handling) animatable and independently testable.
 *
 * [center]/[zoom] are backed by [Animatable]s bounded to `0f..1f` / [minZoom]..[maxZoom], so every
 * mutation — [animateTo] or the internal snap/decay calls [MapViewer] uses for live dragging and
 * fling — is clamped the same way: it is not possible to end up with an out-of-range center or an
 * out-of-range zoom.
 */
@Stable
class MapViewerState internal constructor(
    initialCenter: NormalizedPoint,
    initialZoom: Float,
    minZoom: Float,
    maxZoom: Float,
) {
    /** The [MapConfig][com.pamaz.vi.core.model.MapConfig]'s zoom range; update via [setZoomRange] if the map config changes. */
    var minZoom: Float by mutableFloatStateOf(minZoom)
        private set
    var maxZoom: Float by mutableFloatStateOf(maxZoom)
        private set

    private val centerXAnimatable = Animatable(initialCenter.x.coerceIn(0f, 1f)).apply {
        updateBounds(0f, 1f)
    }
    private val centerYAnimatable = Animatable(initialCenter.y.coerceIn(0f, 1f)).apply {
        updateBounds(0f, 1f)
    }
    private val zoomAnimatable = Animatable(initialZoom.coerceIn(minZoom, maxZoom)).apply {
        updateBounds(minZoom, maxZoom)
    }

    /** Current camera center. Always within `0f..1f` on both axes. */
    val center: NormalizedPoint
        get() = NormalizedPoint(centerXAnimatable.value, centerYAnimatable.value)

    /** Current fractional zoom. Always within [minZoom]..[maxZoom]. */
    val zoom: Float
        get() = zoomAnimatable.value

    /**
     * `true` while [zoom] is mid-[Animatable.animateTo] (a double-tap or +/- button zoom, whose
     * destination is known the instant the animation starts) rather than being driven frame-by-frame
     * via [snapTo] (an organic pinch, which only ever knows "where the finger is *right now*").
     * [TileLayer] reads this to skip its 80ms "has the zoom level been stable" debounce for an
     * animated zoom — see [com.pamaz.vi.core.mapview.tile.TileCacheConfig.ZOOM_LEVEL_REQUEST_STABILITY_MS]'s
     * KDoc for why an animated zoom doesn't need that debounce at all.
     */
    val isZoomAnimating: Boolean
        get() = zoomAnimatable.isRunning

    /** Last size [MapViewer] measured itself at, in pixels; [IntSize.Zero] before the first layout pass. */
    var viewportSize: IntSize by mutableStateOf(IntSize.Zero)
        internal set

    /** Updates the allowed zoom range (e.g. after loading a new [MapConfig][com.pamaz.vi.core.model.MapConfig]), re-clamping the current zoom if needed. */
    internal suspend fun setZoomRange(minZoom: Float, maxZoom: Float) {
        this.minZoom = minZoom
        this.maxZoom = maxZoom
        zoomAnimatable.updateBounds(minZoom, maxZoom)
        if (zoomAnimatable.value < minZoom || zoomAnimatable.value > maxZoom) {
            zoomAnimatable.snapTo(zoomAnimatable.value.coerceIn(minZoom, maxZoom))
        }
    }

    /**
     * Smoothly animates [center] and [zoom] to [point]/[zoom], clamping zoom to
     * [minZoom]..[maxZoom] and [point] to [centerBoundsX]/[centerBoundsY] (the "scroll limit" for
     * the axis — see [centerBoundsForAxis]; defaults to the unrestricted `0f..1f` for callers that
     * already guarantee [point] is in range, e.g. [computeFocalZoomCenter]'s own output).
     */
    suspend fun animateTo(
        point: NormalizedPoint,
        zoom: Float,
        centerBoundsX: ClosedFloatingPointRange<Float> = 0f..1f,
        centerBoundsY: ClosedFloatingPointRange<Float> = 0f..1f,
    ) = coroutineScope {
        val spec = tween<Float>(durationMillis = ANIMATE_TO_DURATION_MS)
        val targetZoom = zoom.coerceIn(minZoom, maxZoom)
        centerXAnimatable.updateBounds(centerBoundsX.start, centerBoundsX.endInclusive)
        centerYAnimatable.updateBounds(centerBoundsY.start, centerBoundsY.endInclusive)
        launch { centerXAnimatable.animateTo(point.x.coerceIn(centerBoundsX), spec) }
        launch { centerYAnimatable.animateTo(point.y.coerceIn(centerBoundsY), spec) }
        launch { zoomAnimatable.animateTo(targetZoom, spec) }
    }

    /**
     * Instantly jumps to [point]/[zoom] — used while a drag/pinch gesture is actively tracking the
     * finger, so called on every pointer-move event (potentially dozens of times a second) for the
     * duration of a gesture. [centerBoundsX]/[centerBoundsY] re-arm [centerXAnimatable]/
     * [centerYAnimatable]'s own bounds for the *current* zoom/viewport (see [centerBoundsForAxis])
     * every call, so a bound left over from a different zoom level never lingers into the next
     * gesture.
     *
     * Previously wrapped its three [Animatable.snapTo] calls in `coroutineScope { launch { ... } }`
     * x3 — real overhead (child-coroutine creation/structured-concurrency bookkeeping) paid on
     * every single call of a genuinely hot path, for zero benefit: [Animatable.snapTo] sets its
     * value immediately rather than interpolating over frames, so there was never any actual
     * concurrent work for those three launches to overlap. Three plain sequential `snapTo` calls
     * are observably identical (all three values are updated before this function returns either
     * way) at a fraction of the per-call cost — see the caller in `MapGestures.kt` for the other,
     * larger half of this same fix (one coroutine launched per *gesture*, not per *event*).
     */
    internal suspend fun snapTo(
        point: NormalizedPoint,
        zoom: Float,
        centerBoundsX: ClosedFloatingPointRange<Float> = 0f..1f,
        centerBoundsY: ClosedFloatingPointRange<Float> = 0f..1f,
    ) {
        centerXAnimatable.updateBounds(centerBoundsX.start, centerBoundsX.endInclusive)
        centerYAnimatable.updateBounds(centerBoundsY.start, centerBoundsY.endInclusive)
        centerXAnimatable.snapTo(point.x.coerceIn(centerBoundsX))
        centerYAnimatable.snapTo(point.y.coerceIn(centerBoundsY))
        zoomAnimatable.snapTo(zoom.coerceIn(minZoom, maxZoom))
    }

    /**
     * Animates [center]/[zoom] so [bounds] (e.g. a tapped [MapViewerCluster.bounds]) fits inside
     * the viewport, inset by [paddingPx] on every side, clamped to [minZoom]..[maxZoom] — the
     * "zoom in on this cluster" action a caller wires to `onClusterTap`. [scheme] is read once for
     * this one calculation, not stored (same reason [MapGeometry.kt]'s pure functions take it as a
     * parameter instead: this class stays scheme-agnostic, see its own KDoc).
     *
     * The target zoom is the largest one at which [bounds] still fits: `fitScale = min(scaleX,
     * scaleY)` for the two axes, inverted through [TileScheme.scaleAt]'s own `2^(zoom - maxZoom)`
     * formula. A zero-size [bounds] (a single point) has no finite fit scale on that axis — treated
     * as "no constraint from this axis" (`Float.POSITIVE_INFINITY`) rather than a division by
     * zero, so a degenerate bounds still resolves to [maxZoom] via the final `coerceIn` instead of
     * `NaN`.
     */
    suspend fun animateToBounds(
        scheme: TileScheme,
        bounds: NormalizedBounds,
        paddingPx: Float = 0f,
    ) {
        val boundsWidthFraction = bounds.maxX - bounds.minX
        val boundsHeightFraction = bounds.maxY - bounds.minY
        val availableWidthPx = (viewportSize.width - 2f * paddingPx).coerceAtLeast(1f)
        val availableHeightPx = (viewportSize.height - 2f * paddingPx).coerceAtLeast(1f)

        val scaleX = if (boundsWidthFraction > 0f) {
            availableWidthPx / (boundsWidthFraction * scheme.widthPx)
        } else {
            Float.POSITIVE_INFINITY
        }
        val scaleY = if (boundsHeightFraction > 0f) {
            availableHeightPx / (boundsHeightFraction * scheme.heightPx)
        } else {
            Float.POSITIVE_INFINITY
        }
        val fitScale = min(scaleX, scaleY)
        val targetZoom =
            (log2(fitScale.toDouble()).toFloat() + scheme.maxZoom).coerceIn(minZoom, maxZoom)

        val targetCenter = NormalizedPoint(
            (bounds.minX + bounds.maxX) / 2f,
            (bounds.minY + bounds.maxY) / 2f,
        )
        val finalScale = scheme.scaleAt(targetZoom)
        val boundsX = centerBoundsForAxis(
            (scheme.widthPx * finalScale).toFloat(), viewportSize.width.toFloat(),
        )
        val boundsY = centerBoundsForAxis(
            (scheme.heightPx * finalScale).toFloat(), viewportSize.height.toFloat(),
        )

        animateTo(targetCenter, targetZoom, boundsX, boundsY)
    }

    /** Stops any in-flight fling/animation on the center axes — used when a new gesture begins. */
    internal suspend fun stopCenterMotion() = coroutineScope {
        launch { centerXAnimatable.stop() }
        launch { centerYAnimatable.stop() }
    }

    /**
     * Glides [center] from [velocityXPerSecond]/[velocityYPerSecond] (normalized units/second) to
     * a stop via exponential decay, stopping early at the edges of [centerBoundsX]/[centerBoundsY]
     * (the same "scroll limit" [snapTo] uses — zoom never changes mid-fling, so the bound computed
     * once at release time, from [MapGestures.kt]'s `panZoomFlingGestures`, stays valid for the
     * whole decay) rather than the unrestricted `0f`/`1f` [centerXAnimatable]/[centerYAnimatable]
     * started with.
     */
    internal suspend fun flingBy(
        velocityXPerSecond: Float,
        velocityYPerSecond: Float,
        centerBoundsX: ClosedFloatingPointRange<Float> = 0f..1f,
        centerBoundsY: ClosedFloatingPointRange<Float> = 0f..1f,
    ) = coroutineScope {
        centerXAnimatable.updateBounds(centerBoundsX.start, centerBoundsX.endInclusive)
        centerYAnimatable.updateBounds(centerBoundsY.start, centerBoundsY.endInclusive)
        val spec = exponentialDecay<Float>(absVelocityThreshold = FLING_STOP_VELOCITY)
        launch { centerXAnimatable.animateDecay(velocityXPerSecond, spec) }
        launch { centerYAnimatable.animateDecay(velocityYPerSecond, spec) }
    }
}

/**
 * Creates and [remember]s a [MapViewerState]. [minZoom]/[maxZoom] normally come straight from the
 * [MapConfig][com.pamaz.vi.core.model.MapConfig] passed to [MapViewer] (`config.minZoom.toFloat()`
 * / `config.maxZoom.toFloat()`); they are separate constructor arguments here (rather than taking
 * a [MapConfig][com.pamaz.vi.core.model.MapConfig] directly) so this state — and its zoom-clamping
 * behavior — can be unit tested without constructing one.
 */
@Composable
fun rememberMapViewerState(
    initialCenter: NormalizedPoint = NormalizedPoint(0.5f, 0.5f),
    initialZoom: Float = 0f,
    minZoom: Float = 0f,
    maxZoom: Float = 0f,
): MapViewerState = remember {
    MapViewerState(initialCenter, initialZoom, minZoom, maxZoom)
}

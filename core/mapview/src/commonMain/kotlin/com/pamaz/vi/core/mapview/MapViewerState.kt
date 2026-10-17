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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

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

    /** Smoothly animates [center] and [zoom] to [point]/[zoom], clamping zoom to [minZoom]..[maxZoom]. */
    suspend fun animateTo(point: NormalizedPoint, zoom: Float) = coroutineScope {
        val spec = tween<Float>(durationMillis = ANIMATE_TO_DURATION_MS)
        val targetZoom = zoom.coerceIn(minZoom, maxZoom)
        launch { centerXAnimatable.animateTo(point.x.coerceIn(0f, 1f), spec) }
        launch { centerYAnimatable.animateTo(point.y.coerceIn(0f, 1f), spec) }
        launch { zoomAnimatable.animateTo(targetZoom, spec) }
    }

    /** Instantly jumps to [point]/[zoom] — used while a drag/pinch gesture is actively tracking the finger. */
    internal suspend fun snapTo(point: NormalizedPoint, zoom: Float) = coroutineScope {
        launch { centerXAnimatable.snapTo(point.x.coerceIn(0f, 1f)) }
        launch { centerYAnimatable.snapTo(point.y.coerceIn(0f, 1f)) }
        launch { zoomAnimatable.snapTo(zoom.coerceIn(minZoom, maxZoom)) }
    }

    /** Stops any in-flight fling/animation on the center axes — used when a new gesture begins. */
    internal suspend fun stopCenterMotion() = coroutineScope {
        launch { centerXAnimatable.stop() }
        launch { centerYAnimatable.stop() }
    }

    /**
     * Glides [center] from [velocityXPerSecond]/[velocityYPerSecond] (normalized units/second) to
     * a stop via exponential decay, stopping early at `0f`/`1f` thanks to the bounds set in [centerXAnimatable]/[centerYAnimatable].
     */
    internal suspend fun flingBy(velocityXPerSecond: Float, velocityYPerSecond: Float) =
        coroutineScope {
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

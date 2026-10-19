package com.pamaz.vi.core.mapview

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.ui.unit.IntSize
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.TileScheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.math.log2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [MapViewerState.animateTo] is driven by [androidx.compose.animation.core.Animatable], which
 * needs a [androidx.compose.runtime.MonotonicFrameClock] to advance at all — `runTest`'s virtual
 * time alone doesn't provide one. [BroadcastFrameClock] (meant for exactly this kind of test)
 * lets each test manually "tick" frames until the animation completes, with no real delay.
 *
 * "Pan limits" (the other half of this class's test bullet) are covered by
 * [MapGeometryTest]'s `panCenter`/`computeFocalZoomCenter` tests: that's the pure math
 * [MapViewerState] delegates to for every pan/fling/pinch update.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewerStateTest {

    private fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Float = 0.02f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "expected $expected to be within $tolerance of $actual"
        )
    }

    private fun runAnimationTest(block: suspend () -> Unit) = runTest {
        val frameClock = BroadcastFrameClock()
        val job = launch(frameClock) { block() }
        var nanos = 0L
        // 300ms animation at ~16ms/frame, plus margin; exits early once the job completes.
        repeat(60) {
            if (!job.isActive) return@repeat
            frameClock.sendFrame(nanos)
            nanos += 16_000_000L
            runCurrent()
        }
        job.join()
    }

    @Test
    fun animateTo_movesCenterAndZoomToTarget() = runAnimationTest {
        val state =
            MapViewerState(NormalizedPoint(0f, 0f), initialZoom = 0f, minZoom = 0f, maxZoom = 5f)
        state.animateTo(NormalizedPoint(0.8f, 0.3f), zoom = 4f)
        assertAlmostEquals(0.8f, state.center.x)
        assertAlmostEquals(0.3f, state.center.y)
        assertAlmostEquals(4f, state.zoom)
    }

    @Test
    fun animateTo_clampsZoomAboveMax() = runAnimationTest {
        val state = MapViewerState(
            NormalizedPoint(0.5f, 0.5f),
            initialZoom = 0f,
            minZoom = 0f,
            maxZoom = 5f
        )
        state.animateTo(NormalizedPoint(0.5f, 0.5f), zoom = 99f)
        assertAlmostEquals(5f, state.zoom)
    }

    @Test
    fun animateTo_clampsZoomBelowMin() = runAnimationTest {
        val state = MapViewerState(
            NormalizedPoint(0.5f, 0.5f),
            initialZoom = 2f,
            minZoom = 1f,
            maxZoom = 5f
        )
        state.animateTo(NormalizedPoint(0.5f, 0.5f), zoom = -10f)
        assertAlmostEquals(1f, state.zoom)
    }

    @Test
    fun initialZoom_isClampedAtConstruction() {
        val state = MapViewerState(
            NormalizedPoint(0.5f, 0.5f),
            initialZoom = 99f,
            minZoom = 0f,
            maxZoom = 5f
        )
        assertEquals(5f, state.zoom)
    }

    @Test
    fun initialCenter_isClampedAtConstruction() {
        val state =
            MapViewerState(NormalizedPoint(0f, 1f), initialZoom = 0f, minZoom = 0f, maxZoom = 5f)
        assertEquals(0f, state.center.x)
        assertEquals(1f, state.center.y)
    }

    @Test
    fun setZoomRange_reClampsAnOutOfRangeCurrentZoom() = runTest {
        val state = MapViewerState(
            NormalizedPoint(0.5f, 0.5f),
            initialZoom = 4f,
            minZoom = 0f,
            maxZoom = 5f
        )
        state.setZoomRange(minZoom = 0f, maxZoom = 2f)
        assertEquals(2f, state.zoom)
    }

    // scheme.maxZoom is 5 here (256 doubled 5 times reaches 8192) — matches MapGeometryTest's own
    // scheme, so the zoom math below is directly comparable to that file's pan/focal-zoom tests.
    private val scheme = TileScheme(widthPx = 8192, heightPx = 6144)

    @Test
    fun animateToBounds_zoomsInToFitTheBoundsAndCentersOnIt() = runAnimationTest {
        val state =
            MapViewerState(
                NormalizedPoint(0.5f, 0.5f),
                initialZoom = 0f,
                minZoom = 0f,
                maxZoom = 5f
            )
        state.viewportSize = IntSize(1000, 800)
        val bounds = NormalizedBounds(minX = 0.4f, minY = 0.4f, maxX = 0.6f, maxY = 0.6f)

        state.animateToBounds(scheme, bounds)

        val scaleX = 1000f / (0.2f * scheme.widthPx)
        val scaleY = 800f / (0.2f * scheme.heightPx)
        val expectedZoom = log2(minOf(scaleX, scaleY).toDouble()).toFloat() + scheme.maxZoom
        assertAlmostEquals(expectedZoom, state.zoom)
        assertAlmostEquals(0.5f, state.center.x)
        assertAlmostEquals(0.5f, state.center.y)
    }

    @Test
    fun animateToBounds_clampsToMaxZoom_forADegenerateSinglePointBounds() = runAnimationTest {
        val state =
            MapViewerState(
                NormalizedPoint(0.5f, 0.5f),
                initialZoom = 0f,
                minZoom = 0f,
                maxZoom = 5f
            )
        state.viewportSize = IntSize(1000, 800)
        // A zero-size bounds (both dimensions 0) has no finite "fit" scale on either axis — this is
        // the "respects the limits" edge case: it must resolve to maxZoom, not NaN/Infinity.
        val bounds = NormalizedBounds(minX = 0.5f, minY = 0.5f, maxX = 0.5f, maxY = 0.5f)

        state.animateToBounds(scheme, bounds)

        assertAlmostEquals(5f, state.zoom)
        assertTrue(state.zoom in state.minZoom..state.maxZoom)
    }

    @Test
    fun animateToBounds_clampsToMinZoom_whenTheFitZoomWouldGoLower() = runAnimationTest {
        // minZoom raised to 3 here specifically so the bounds below (the whole map) computes a
        // natural fit zoom *below* it, forcing the lower-bound clamp this test is checking.
        val state =
            MapViewerState(
                NormalizedPoint(0.5f, 0.5f),
                initialZoom = 4f,
                minZoom = 3f,
                maxZoom = 5f
            )
        state.viewportSize = IntSize(1000, 800)
        val bounds = NormalizedBounds(minX = 0f, minY = 0f, maxX = 1f, maxY = 1f)

        state.animateToBounds(scheme, bounds)

        assertAlmostEquals(3f, state.zoom)
        assertTrue(state.zoom in state.minZoom..state.maxZoom)
    }
}

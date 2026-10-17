package com.pamaz.vi.core.mapview

import androidx.compose.runtime.BroadcastFrameClock
import com.pamaz.vi.core.model.NormalizedPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
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
}

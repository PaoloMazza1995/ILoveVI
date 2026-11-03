package com.pamaz.vi.core.mapview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.model.MapConfig
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-01-01T00:00:00Z")

private val config = MapConfig(
    mapVersion = 1,
    sourceImageUrl = "https://example.com/map.png",
    widthPx = 2048,
    heightPx = 2048,
    tileUrlTemplate = "https://example.com/{z}/{x}/{y}.png",
    minZoom = 0,
    maxZoom = 3,
    updatedAt = NOW,
)

/**
 * Regression test for a reported bug: "double-tapping the map resets the zoom to zero." Unlike
 * [MapViewerCameraStabilityUiTest] (which drives [MapViewerState] directly), this one dispatches a
 * real double-click through the full pointer-input pipeline, since a double-tap is handled by a
 * *second*, independent `pointerInput` block ([MapGestures.kt]'s `tapGestures`) running alongside
 * the pan/pinch/fling one — a bug specific to double-tapping is most likely an interaction between
 * the two, which only a real dispatched gesture (not a direct state mutation) can exercise.
 */
class MapViewerDoubleTapUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun doubleTap_zoomsIn_andNeverDropsToZero() {
        var cameraState: MapViewerState? = null
        var minZoomSeen = Float.MAX_VALUE

        composeRule.setContent {
            val state = rememberMapViewerState(
                minZoom = config.minZoom.toFloat(),
                maxZoom = config.maxZoom.toFloat(),
            )
            cameraState = state
            Box(Modifier.size(400.dp)) {
                MapViewer(
                    state = state,
                    content = MapContent.Tiles(config),
                    items = emptyList(),
                    modifier = Modifier.size(400.dp),
                )
            }
        }
        composeRule.waitForIdle()

        val camera = requireNotNull(cameraState)
        val zoomBeforeDoubleTap = camera.zoom

        composeRule.onRoot().performTouchInput {
            doubleClick(position = Offset(200f, 200f))
        }

        // Sample the zoom repeatedly while the 300ms animateTo tween is in flight — the reported
        // bug was a *transient* drop to zero mid-animation, not necessarily the final value, so a
        // single post-idle check could miss it.
        repeat(20) {
            composeRule.waitForIdle()
            minZoomSeen = minOf(minZoomSeen, camera.zoom)
            Thread.sleep(20)
        }
        composeRule.waitForIdle()

        val zoomAfterDoubleTap = camera.zoom
        assertTrue(
            zoomAfterDoubleTap > zoomBeforeDoubleTap,
            "expected double-tap to zoom in (from $zoomBeforeDoubleTap), was $zoomAfterDoubleTap",
        )
        assertTrue(
            minZoomSeen > 0f,
            "zoom dropped to $minZoomSeen at some point during/after the double-tap, expected it to stay above 0",
        )
    }
}

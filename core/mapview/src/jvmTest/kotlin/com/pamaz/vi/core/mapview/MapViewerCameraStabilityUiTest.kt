package com.pamaz.vi.core.mapview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.NormalizedPoint
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-01-01T00:00:00Z")
private val LATER = Instant.parse("2026-01-02T00:00:00Z")

private fun config(updatedAt: Instant, tileUrlTemplate: String, mapVersion: Int) = MapConfig(
    mapVersion = mapVersion,
    sourceImageUrl = "https://example.com/map.png",
    widthPx = 2048,
    heightPx = 2048,
    tileUrlTemplate = tileUrlTemplate,
    minZoom = 0,
    maxZoom = 3,
    updatedAt = updatedAt,
)

/** The original config, matching the real-world "first sync landed" moment. */
private val configA = config(NOW, "https://example.com/{z}/{x}/{y}.png", mapVersion = 1)

/**
 * Same `widthPx`/`heightPx`/`minZoom`/`maxZoom` as [configA] — only `updatedAt`, `tileUrlTemplate`
 * and `mapVersion` differ, exactly the kind of incidental field change a routine backend resync
 * can produce without the map's actual geometry changing at all.
 */
private val configB = config(LATER, "https://cdn2.example.com/{z}/{x}/{y}.png", mapVersion = 2)

/**
 * Regression test for a reported bug: "dragging the map resets the zoom [to its initial fit]."
 * Root cause: [MapContent.Tiles] is a `data class` wrapping the *entire* [MapConfig]; `MapViewer`
 * used the raw `content` value (not just its geometry-relevant fields) as the key for both its
 * `hasAppliedInitialFitZoom` flag and the `LaunchedEffect` that acts on it — so *any* `MapConfig`
 * field changing (not just `widthPx`/`heightPx`/`minZoom`/`maxZoom`) looked exactly like "the map
 * changed" and replayed the initial fit-to-viewport `snapTo`, silently discarding wherever the
 * user had actually panned/zoomed to. Fixed via `MapContent.geometryKey`. This test moves the
 * camera away from its initial position, swaps in a `MapConfig` that differs *only* in fields that
 * shouldn't matter, and asserts the camera didn't move.
 */
class MapViewerCameraStabilityUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun unrelatedMapConfigFieldChange_doesNotResetTheCamera() {
        var contentConfig by mutableStateOf(configA)
        var triggerUserPan by mutableStateOf(false)
        var cameraState: MapViewerState? = null

        composeRule.setContent {
            val state = rememberMapViewerState(
                minZoom = configA.minZoom.toFloat(),
                maxZoom = configA.maxZoom.toFloat(),
            )
            cameraState = state
            LaunchedEffect(triggerUserPan) {
                if (triggerUserPan) {
                    // Simulates the user having panned/zoomed away from wherever the initial
                    // fit-to-viewport landed — the exact scenario the bug report described.
                    state.animateTo(NormalizedPoint(0.1f, 0.15f), state.maxZoom)
                }
            }
            Box(Modifier.size(400.dp)) {
                MapViewer(
                    state = state,
                    content = MapContent.Tiles(contentConfig),
                    items = emptyList(),
                    modifier = Modifier.size(400.dp),
                )
            }
        }
        composeRule.waitForIdle()

        triggerUserPan = true
        composeRule.waitForIdle()

        val camera = requireNotNull(cameraState)
        val zoomAfterUserPan = camera.zoom
        val centerAfterUserPan = camera.center
        // Sanity check this test actually moved the camera away from its initial fit position —
        // otherwise the assertion below would trivially pass even with the bug present.
        assertAlmostEquals(0.1f, centerAfterUserPan.x)
        assertAlmostEquals(0.15f, centerAfterUserPan.y)

        // configB differs only in updatedAt/tileUrlTemplate/mapVersion — none of which should
        // affect the camera at all.
        contentConfig = configB
        composeRule.waitForIdle()

        assertAlmostEquals(zoomAfterUserPan, camera.zoom, tolerance = 0.001f)
        assertAlmostEquals(centerAfterUserPan.x, camera.center.x, tolerance = 0.001f)
        assertAlmostEquals(centerAfterUserPan.y, camera.center.y, tolerance = 0.001f)
    }
}

private fun assertAlmostEquals(expected: Float, actual: Float, tolerance: Float = 0.01f) {
    assertTrue(
        abs(expected - actual) <= tolerance,
        "expected $actual to be within $tolerance of $expected",
    )
}

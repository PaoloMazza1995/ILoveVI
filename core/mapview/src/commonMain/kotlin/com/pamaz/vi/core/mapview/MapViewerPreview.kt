package com.pamaz.vi.core.mapview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViTheme
import kotlin.random.Random
import kotlin.time.Instant

/**
 * A `MapConfig` pointing at a *local* placeholder tile pyramid — never a real or fan-made game map
 * (see `README.md`'s rights note). Generate one that actually resolves by running, from the repo
 * root:
 * ```
 * ./gradlew :tools:tilegen:run --args="generate-placeholder --out /tmp/ilovevi-preview-map.png"
 * ./gradlew :tools:tilegen:run --args="--source /tmp/ilovevi-preview-map.png --out /tmp/ilovevi-preview-tiles"
 * ```
 * Without that step, these previews still correctly demonstrate [TileLayer]'s documented
 * behavior: every tile request fails independently (no file exists yet) and each just shows its
 * neutral placeholder box — exactly the "a failed tile never breaks the rest" behavior this
 * viewer is required to have, not a broken preview.
 */
private val PreviewMapConfig = MapConfig(
    mapVersion = 1,
    sourceImageUrl = "file:///tmp/ilovevi-preview-map.png",
    widthPx = 8192,
    heightPx = 6144,
    tileUrlTemplate = "file:///tmp/ilovevi-preview-tiles/1/{z}/{x}/{y}.png",
    minZoom = 0,
    maxZoom = 5,
    updatedAt = Instant.fromEpochMilliseconds(0),
)

/** Also used by [MapViewerSampleScreen] — kept `internal` rather than `private` for that reuse. */
internal fun fakePins(count: Int, seed: Int = 0): List<MapViewerPin> {
    val random = Random(seed)
    val categories = EntryCategory.entries
    return List(count) { index ->
        MapViewerPin(
            id = "preview-pin-$index",
            position = NormalizedPoint(random.nextFloat(), random.nextFloat()),
            category = categories[index % categories.size],
            label = if (index % 7 == 0) "Pin $index" else null,
        )
    }
}

@Preview
@Composable
private fun MapViewerEmptyPreview() {
    IloveViTheme {
        val state = rememberMapViewerState(
            minZoom = PreviewMapConfig.minZoom.toFloat(),
            maxZoom = PreviewMapConfig.maxZoom.toFloat(),
        )
        MapViewer(
            state = state,
            config = PreviewMapConfig,
            pins = emptyList(),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview
@Composable
private fun MapViewerWithPinsPreview() {
    IloveViTheme {
        val state = rememberMapViewerState(
            minZoom = PreviewMapConfig.minZoom.toFloat(),
            maxZoom = PreviewMapConfig.maxZoom.toFloat(),
        )
        val pins = remember { fakePins(12) }
        MapViewer(
            state = state,
            config = PreviewMapConfig,
            pins = pins,
            selectedPinId = pins.first().id,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Step 5A-bis's single-image mode: [rememberDevMapImage] (the bundled dev map, or its
 * grid-with-labels fallback if that resource is missing/disabled — see `README.md`'s rights note)
 * with 6 fake pins across all three categories. Shows the required "loading placeholder until
 * ready" state too: [rememberDevMapImage] returns `null` for one frame while the image decodes
 * off the main thread, during which this preview shows a plain themed surface instead of the
 * viewer.
 */
@Preview
@Composable
private fun MapViewerSingleImagePreview() {
    IloveViTheme {
        val devImage = rememberDevMapImage()
        if (devImage == null) {
            Box(modifier = Modifier.fillMaxSize().background(IloveViColors.bg900))
        } else {
            val state = rememberMapViewerState()
            val pins = remember { fakePins(6) }
            MapViewer(
                state = state,
                content = MapContent.SingleImage(devImage),
                pins = pins,
                selectedPinId = pins.first().id,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Same viewer, 1000 fake pins scattered across the whole map — this repo's acceptance check for
 * "stays smooth with 1000 fake pins". Deploy or run this preview interactively (this sandboxed,
 * display-less environment can't render Compose UI or measure real frame times itself — see the
 * module README for what was and wasn't verified here) and pan/pinch around: [PinLayer]'s viewport
 * culling means only the pins actually on screen are ever composed, regardless of this list's size.
 */
@Preview
@Composable
private fun MapViewerWithOneThousandPinsPreview() {
    IloveViTheme {
        val state = rememberMapViewerState(
            minZoom = PreviewMapConfig.minZoom.toFloat(),
            maxZoom = PreviewMapConfig.maxZoom.toFloat(),
        )
        val pins = remember { fakePins(1000) }
        MapViewer(
            state = state,
            config = PreviewMapConfig,
            pins = pins,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

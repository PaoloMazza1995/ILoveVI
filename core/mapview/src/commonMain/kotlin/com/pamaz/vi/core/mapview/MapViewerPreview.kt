package com.pamaz.vi.core.mapview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViTheme
import kotlinx.coroutines.launch
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

/**
 * Also used by [MapViewerSampleScreen] — kept `internal` rather than `private` for that reuse.
 * Three fake clusters, the exact counts step 5A's cluster-support addition calls out (11, 42,
 * 120), each a different [MapViewerCluster.dominantCategory] (including one mixed/`null`) so the
 * preview exercises [com.pamaz.vi.design.components.IloveViClusterBadge]'s category/mixed/"99+"
 * variants together. [MapViewerCluster.bounds] is a small square around [MapViewerCluster.position]
 * — a real cluster's bounds would be the extent of the pins it merged, which this module has no
 * knowledge of (`feature:map` owns clustering), so a plausible placeholder span is used here.
 */
internal fun fakeClusters(seed: Int = 1): List<MapViewerCluster> {
    val random = Random(seed)
    val counts = listOf(11, 42, 120)
    val dominantCategories = listOf(EntryCategory.WEAPON, EntryCategory.VEHICLE, null)
    val boundsHalfSpan = 0.05f
    return counts.mapIndexed { index, count ->
        val x = random.nextFloat()
        val y = random.nextFloat()
        MapViewerCluster(
            id = "preview-cluster-$index",
            position = NormalizedPoint(x, y),
            count = count,
            dominantCategory = dominantCategories[index % dominantCategories.size],
            bounds = NormalizedBounds(
                minX = (x - boundsHalfSpan).coerceIn(0f, 1f),
                minY = (y - boundsHalfSpan).coerceIn(0f, 1f),
                maxX = (x + boundsHalfSpan).coerceIn(0f, 1f),
                maxY = (y + boundsHalfSpan).coerceIn(0f, 1f),
            ),
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
 * Pins and clusters together (step 5A's cluster-support addition) — [fakeClusters]' 11/42/120
 * counts, mixed with ordinary pins, the selected pin drawn over everything. Tapping a cluster
 * calls [MapViewerState.animateToBounds] with its [MapViewerCluster.bounds], demonstrating the
 * "zoom in to show every pin it stood for" action a real caller (`feature:map`) would wire the
 * same way.
 */
@Preview
@Composable
private fun MapViewerWithPinsAndClustersPreview() {
    IloveViTheme {
        val state = rememberMapViewerState(
            minZoom = PreviewMapConfig.minZoom.toFloat(),
            maxZoom = PreviewMapConfig.maxZoom.toFloat(),
        )
        val scheme = remember { TileScheme(PreviewMapConfig.widthPx, PreviewMapConfig.heightPx) }
        val pins = remember { fakePins(12) }
        val clusters = remember { fakeClusters() }
        val items = remember(pins, clusters) {
            pins.map { MapViewerItem.Pin(it) } + clusters.map { MapViewerItem.Cluster(it) }
        }
        val coroutineScope = rememberCoroutineScope()
        MapViewer(
            state = state,
            content = MapContent.Tiles(PreviewMapConfig),
            items = items,
            selectedPinId = pins.first().id,
            onClusterTap = { cluster ->
                coroutineScope.launch {
                    state.animateToBounds(
                        scheme,
                        cluster.bounds,
                        paddingPx = 48f
                    )
                }
            },
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

package com.pamaz.vi.feature.catalog.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.mapview.MapViewer
import com.pamaz.vi.core.mapview.MapViewerPin
import com.pamaz.vi.core.mapview.rememberMapViewerState
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.design.IloveViShapes
import ilovevi.feature.catalog.generated.resources.Res
import ilovevi.feature.catalog.generated.resources.detail_mini_map_content_description
import org.jetbrains.compose.resources.stringResource

private val MiniMapHeight = 160.dp

/**
 * DET-02's "Where to find it" mini-map: a small, **non-interactive** [MapViewer] centered on
 * [pin], with that pin selected (highlighted). [MapViewer] itself has no "disable gestures" flag
 * (every other caller wants pan/pinch/tap) — rather than touching that shared module to add a
 * mode option one caller needs, an invisible full-size [Modifier.pointerInput] on top consumes
 * every touch before [MapViewer]'s own gesture detector ever sees it, the same "block input
 * without the component knowing it's blocked" idiom used for any read-only embed of an
 * interactive view. Centering uses [rememberMapViewerState]'s `initialCenter`/`initialZoom`
 * directly (no `animateTo` call needed, since this view is never meant to move again) at a fixed
 * zoom two steps in from [MapConfig.minZoom] — a judgment call (not specified anywhere), clamped
 * to the config's own range so it never exceeds [MapConfig.maxZoom].
 *
 * [MapViewer] sets its own "Map. Drag to pan..." content description, which is actively wrong
 * here (nothing can be dragged); [Modifier.clearAndSetSemantics] replaces it with a description
 * that matches what this view actually is.
 */
@Composable
internal fun EntryMiniMap(mapConfig: MapConfig, pin: MapPin, modifier: Modifier = Modifier) {
    val zoom = remember(mapConfig) {
        (mapConfig.minZoom + 2).toFloat()
            .coerceIn(mapConfig.minZoom.toFloat(), mapConfig.maxZoom.toFloat())
    }
    val state = rememberMapViewerState(
        initialCenter = pin.position,
        initialZoom = zoom,
        minZoom = mapConfig.minZoom.toFloat(),
        maxZoom = mapConfig.maxZoom.toFloat(),
    )
    val miniMapDescription = stringResource(Res.string.detail_mini_map_content_description)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniMapHeight)
            .clip(IloveViShapes.radiusMd)
            .clearAndSetSemantics { contentDescription = miniMapDescription },
    ) {
        MapViewer(
            state = state,
            config = mapConfig,
            pins = listOf(
                MapViewerPin(
                    id = pin.id.value,
                    position = pin.position,
                    category = pin.category
                )
            ),
            selectedPinId = pin.id.value,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        )
    }
}

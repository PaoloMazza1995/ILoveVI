package com.pamaz.vi.core.mapview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.TileScheme

/**
 * The shared, read-only map viewer used by both the placement tool and `feature:map` — this is
 * the one reason this module exists outside the roadmap's module list (see `README.md`). It knows
 * about [content], pins and gestures only: no repositories, no network, no database, and (per the
 * product's "no user position" rule) no concept of a current/player location at all.
 *
 * [content] is either a remote tile pyramid ([MapContent.Tiles]) or one bundled/local image with
 * no tiling/network/backend ([MapContent.SingleImage], step 5A-bis) — see [MapContent]'s KDoc.
 * Both branches share this same gesture handling and [PinLayer]; only [scheme] (the size/zoom math
 * both derive from) and which layer draws the map's pixels differ. [content] is read fresh every
 * call (not stored in [state]) so swapping it — a new map version, or switching modes entirely —
 * just works; [state]'s zoom range is kept in sync via a [LaunchedEffect].
 *
 * [pinContent] is the clustering hook: `feature:map` owns all clustering logic, and hands this
 * viewer one [MapViewerPin] per visible cluster (see [MapViewerPin]'s KDoc) plus a [pinContent]
 * override that draws cluster bubbles differently from single pins — this composable itself has
 * no clustering concept.
 *
 * [MapContent.Tiles] assumes 256px tiles (matching [TileScheme]'s default and `:tools:tilegen`'s
 * output) since [MapConfig] doesn't carry a tile size today.
 */
@Composable
fun MapViewer(
    state: MapViewerState,
    content: MapContent,
    pins: List<MapViewerPin>,
    selectedPinId: String? = null,
    onPinTap: (String) -> Unit = {},
    onMapTap: (NormalizedPoint) -> Unit = {},
    modifier: Modifier = Modifier,
    pinContent: @Composable (pin: MapViewerPin, selected: Boolean) -> Unit = { pin, selected ->
        DefaultMapViewerPinContent(pin, selected)
    },
) {
    val scheme = remember(content) {
        when (content) {
            is MapContent.Tiles -> TileScheme(content.config.widthPx, content.config.heightPx)
            is MapContent.SingleImage -> singleImageTileScheme(
                content.image.width,
                content.image.height
            )
        }
    }

    val viewportSize = state.viewportSize
    LaunchedEffect(state, content, viewportSize) {
        when (content) {
            is MapContent.Tiles ->
                state.setZoomRange(
                    content.config.minZoom.toFloat(),
                    content.config.maxZoom.toFloat()
                )

            is MapContent.SingleImage -> {
                val minZoom = singleImageMinZoom(
                    scheme, viewportSize.width.toFloat(), viewportSize.height.toFloat(),
                )
                state.setZoomRange(minZoom, SINGLE_IMAGE_MAX_ZOOM)
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    // pins/callbacks can change on every recomposition; read the latest value through these
    // instead of keying the gesture pointerInput on them, so an in-progress drag/pinch is never
    // interrupted by an unrelated recomposition (e.g. a pin's label changing elsewhere).
    val latestPins by rememberUpdatedState(pins)
    val latestOnPinTap by rememberUpdatedState(onPinTap)
    val latestOnMapTap by rememberUpdatedState(onMapTap)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { state.viewportSize = it }
            .mapViewerGestures(
                state = state,
                scheme = scheme,
                pins = { latestPins },
                onPinTap = { id -> latestOnPinTap(id) },
                onMapTap = { point -> latestOnMapTap(point) },
                coroutineScope = coroutineScope,
            )
            .semantics { contentDescription = "Map. Drag to pan, pinch or double-tap to zoom." },
    ) {
        when (content) {
            is MapContent.Tiles ->
                TileLayer(
                    scheme = scheme,
                    tileUrlTemplate = content.config.tileUrlTemplate,
                    state = state
                )

            is MapContent.SingleImage ->
                SingleImageLayer(image = content.image, scheme = scheme, state = state)
        }
        PinLayer(
            pins = pins,
            scheme = scheme,
            state = state,
            selectedPinId = selectedPinId,
            onPinTap = onPinTap,
            pinContent = pinContent,
        )
    }
}

/**
 * Thin, non-breaking overload for every existing tile-mode caller: wraps [config] into
 * [MapContent.Tiles] and forwards to the [content]-based [MapViewer] above. No behavior change —
 * see that overload's KDoc.
 */
@Composable
fun MapViewer(
    state: MapViewerState,
    config: MapConfig,
    pins: List<MapViewerPin>,
    selectedPinId: String? = null,
    onPinTap: (String) -> Unit = {},
    onMapTap: (NormalizedPoint) -> Unit = {},
    modifier: Modifier = Modifier,
    pinContent: @Composable (pin: MapViewerPin, selected: Boolean) -> Unit = { pin, selected ->
        DefaultMapViewerPinContent(pin, selected)
    },
) = MapViewer(
    state = state,
    content = MapContent.Tiles(config),
    pins = pins,
    selectedPinId = selectedPinId,
    onPinTap = onPinTap,
    onMapTap = onMapTap,
    modifier = modifier,
    pinContent = pinContent,
)

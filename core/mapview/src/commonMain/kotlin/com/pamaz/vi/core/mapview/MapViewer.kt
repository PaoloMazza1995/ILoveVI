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
 * about tiles, pins and gestures only: no repositories, no network, no database, and (per the
 * product's "no user position" rule) no concept of a current/player location at all.
 *
 * [config] supplies the map's size, zoom range and [MapConfig.tileUrlTemplate]; it is read fresh
 * every call (not stored in [state]) so swapping to a new [MapConfig] — a new map version — just
 * works. [state]'s zoom range is kept in sync with [config] via a [LaunchedEffect].
 *
 * [pinContent] is the clustering hook: `feature:map` owns all clustering logic, and hands this
 * viewer one [MapViewerPin] per visible cluster (see [MapViewerPin]'s KDoc) plus a [pinContent]
 * override that draws cluster bubbles differently from single pins — this composable itself has
 * no clustering concept.
 *
 * This viewer assumes 256px tiles (matching [TileScheme]'s default and `:tools:tilegen`'s output)
 * since [MapConfig] doesn't carry a tile size today.
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
) {
    val scheme = remember(config.widthPx, config.heightPx) {
        TileScheme(config.widthPx, config.heightPx)
    }

    LaunchedEffect(state, config.minZoom, config.maxZoom) {
        state.setZoomRange(config.minZoom.toFloat(), config.maxZoom.toFloat())
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
        TileLayer(scheme = scheme, tileUrlTemplate = config.tileUrlTemplate, state = state)
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

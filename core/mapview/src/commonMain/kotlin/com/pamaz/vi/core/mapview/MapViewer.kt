package com.pamaz.vi.core.mapview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.TileScheme
import kotlin.math.max

/**
 * The shared, read-only map viewer used by both the placement tool and `feature:map` — this is
 * the one reason this module exists outside the roadmap's module list (see `README.md`). It knows
 * about [content], [items] and gestures only: no repositories, no network, no database, and (per
 * the product's "no user position" rule) no concept of a current/player location at all.
 *
 * [content] is either a remote tile pyramid ([MapContent.Tiles]) or one bundled/local image with
 * no tiling/network/backend ([MapContent.SingleImage], step 5A-bis) — see [MapContent]'s KDoc.
 * Both branches share this same gesture handling and [PinLayer]; only [scheme] (the size/zoom math
 * both derive from) and which layer draws the map's pixels differ. [content] is read fresh every
 * call (not stored in [state]) so swapping it — a new map version, or switching modes entirely —
 * just works; [state]'s zoom range is kept in sync via a [LaunchedEffect].
 *
 * [items] mixes [MapViewerPin]s and [MapViewerCluster]s freely (see [MapViewerItem]'s KDoc):
 * `feature:map` owns all clustering logic (which pins merge, at which zoom) and simply hands this
 * viewer the result — this composable itself has no clustering concept, only how to draw and
 * hit-test what it's given. [pinContent] customizes a single pin's look; clusters are always drawn
 * with `:core:designsystem`'s `IloveViClusterBadge` (not customizable here, since nothing asked
 * for that yet). A tapped cluster reports [onClusterTap]; [MapViewerState.animateToBounds] is the
 * usual way a caller reacts to it ("zoom in to show every pin this cluster was standing in for").
 *
 * [MapContent.Tiles] assumes 256px tiles (matching [TileScheme]'s default and `:tools:tilegen`'s
 * output) since [MapConfig] doesn't carry a tile size today.
 *
 * [backgroundDragSelectEnabled] opts into rectangle (marquee) select for a drag that starts on
 * empty background, reporting it via [onBackgroundDragStart]/[onBackgroundDrag]/
 * [onBackgroundDragEnd] instead of panning — off by default (zero behavior change for every
 * existing caller); see `backgroundDragSelectGestures`'s KDoc in `MapGestures.kt` for exactly how
 * it coexists with tap/pan/zoom and per-pin drag.
 */
@Composable
fun MapViewer(
    state: MapViewerState,
    content: MapContent,
    items: List<MapViewerItem>,
    selectedPinId: String? = null,
    onPinTap: (String) -> Unit = {},
    onClusterTap: (MapViewerCluster) -> Unit = {},
    onMapTap: (NormalizedPoint) -> Unit = {},
    modifier: Modifier = Modifier,
    backgroundDragSelectEnabled: Boolean = false,
    onBackgroundDragStart: (NormalizedPoint) -> Unit = {},
    onBackgroundDrag: (NormalizedPoint) -> Unit = {},
    onBackgroundDragEnd: () -> Unit = {},
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
    // Both branches need a *viewport-dependent* minZoom, not just [content]'s own declared one:
    // without it, zooming out past "the map fills the viewport" reveals empty space beyond the
    // map's edges (there is nothing to pan into out there) — [MapContent.Tiles]'s own `config
    // .minZoom` is a fixed property of the tile set (how far out real tile data exists), which is
    // usually lower than whatever zoom is needed to actually cover a given device's viewport, so
    // it alone isn't a tight enough floor. Re-applied on *every* viewport size change (not just
    // once): a device's window/inset layout can report more than one size in quick succession
    // right after a cold start while it settles, and a one-shot application would permanently lock
    // in whatever (possibly still-settling, too-small) size happened to arrive first. This is safe
    // to always re-run because [viewportSize] only changes for genuine layout reasons (resize,
    // rotation, insets settling) — a user's own pan/pinch/zoom gestures only ever change [state]'s
    // `center`/`zoom`, never `viewportSize` itself, so this can never fight a manual zoom/pan.
    var hasAppliedInitialFitZoom by remember(content) { mutableStateOf(false) }
    LaunchedEffect(state, content, viewportSize) {
        when (content) {
            is MapContent.Tiles -> {
                // [MapConfig.maxZoom] is backend-declared metadata, not derived from [scheme]
                // itself — if it's ever set higher than what [scheme] geometrically supports for
                // this image's actual `widthPx`/`heightPx` (e.g. a stale value left over from
                // before the source image was swapped for a smaller one), requesting a zoom beyond
                // `scheme.maxZoom` asks [TileUrl.build] for tile coordinates no tile pyramid
                // generated from this image could ever have — each one a guaranteed-failed network
                // request, repeated for every tile in view at that zoom. Clamping here means a
                // misconfigured backend value can never do that, regardless of whether it also gets
                // fixed at the source.
                val maxZoom = minOf(content.config.maxZoom.toFloat(), scheme.maxZoom.toFloat())
                val coverFitZoom = coverFitZoom(
                    scheme, viewportSize.width.toFloat(), viewportSize.height.toFloat(),
                )
                // Also clamped to [maxZoom]: on a viewport large enough (or an image small enough)
                // that "cover" the viewport would need upscaling past the image's own sharpest
                // available zoom, the un-clamped cover-fit zoom can exceed [maxZoom] — which would
                // otherwise make this an invalid (`minZoom > maxZoom`) range and crash
                // `Animatable.updateBounds`. Falling back to [maxZoom] itself here means the image
                // is shown at its sharpest available resolution, slightly short of a perfect
                // edge-to-edge cover in that rare case, rather than crashing.
                val minZoom =
                    max(content.config.minZoom.toFloat(), coverFitZoom).coerceAtMost(maxZoom)
                state.setZoomRange(minZoom, maxZoom)
                if (!hasAppliedInitialFitZoom && viewportSize.width > 0 && viewportSize.height > 0) {
                    hasAppliedInitialFitZoom = true
                    val fitScale = scheme.scaleAt(minZoom)
                    val boundsX = centerBoundsForAxis(
                        (scheme.widthPx * fitScale).toFloat(), viewportSize.width.toFloat(),
                    )
                    val boundsY = centerBoundsForAxis(
                        (scheme.heightPx * fitScale).toFloat(), viewportSize.height.toFloat(),
                    )
                    state.snapTo(state.center, minZoom, boundsX, boundsY)
                }
            }

            is MapContent.SingleImage -> {
                val minZoom = singleImageMinZoom(
                    scheme, viewportSize.width.toFloat(), viewportSize.height.toFloat(),
                )
                state.setZoomRange(minZoom, SINGLE_IMAGE_MAX_ZOOM)
                if (!hasAppliedInitialFitZoom && viewportSize.width > 0 && viewportSize.height > 0) {
                    hasAppliedInitialFitZoom = true
                    val fitScale = scheme.scaleAt(minZoom)
                    val boundsX = centerBoundsForAxis(
                        (scheme.widthPx * fitScale).toFloat(), viewportSize.width.toFloat(),
                    )
                    val boundsY = centerBoundsForAxis(
                        (scheme.heightPx * fitScale).toFloat(), viewportSize.height.toFloat(),
                    )
                    state.snapTo(state.center, minZoom, boundsX, boundsY)
                }
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    // items/callbacks can change on every recomposition; read the latest value through these
    // instead of keying the gesture pointerInput on them, so an in-progress drag/pinch is never
    // interrupted by an unrelated recomposition (e.g. a pin's label changing elsewhere).
    val latestItems by rememberUpdatedState(items)
    val latestOnPinTap by rememberUpdatedState(onPinTap)
    val latestOnClusterTap by rememberUpdatedState(onClusterTap)
    val latestOnMapTap by rememberUpdatedState(onMapTap)
    val latestOnBackgroundDragStart by rememberUpdatedState(onBackgroundDragStart)
    val latestOnBackgroundDrag by rememberUpdatedState(onBackgroundDrag)
    val latestOnBackgroundDragEnd by rememberUpdatedState(onBackgroundDragEnd)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { state.viewportSize = it }
            .mapViewerGestures(
                state = state,
                scheme = scheme,
                items = { latestItems },
                onPinTap = { id -> latestOnPinTap(id) },
                onClusterTap = { cluster -> latestOnClusterTap(cluster) },
                onMapTap = { point -> latestOnMapTap(point) },
                coroutineScope = coroutineScope,
                backgroundDragSelectEnabled = backgroundDragSelectEnabled,
                onBackgroundDragStart = { point -> latestOnBackgroundDragStart(point) },
                onBackgroundDrag = { point -> latestOnBackgroundDrag(point) },
                onBackgroundDragEnd = { latestOnBackgroundDragEnd() },
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
            items = items,
            scheme = scheme,
            state = state,
            selectedPinId = selectedPinId,
            onPinTap = onPinTap,
            onClusterTap = onClusterTap,
            pinContent = pinContent,
        )
    }
}

/**
 * Thin, non-breaking overload for every existing caller that only has [MapViewerPin]s, no
 * clusters: wraps each into [MapViewerItem.Pin] and forwards to the [items]-based [MapViewer]
 * above. No behavior change for an all-pins caller — see that overload's KDoc.
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
    backgroundDragSelectEnabled: Boolean = false,
    onBackgroundDragStart: (NormalizedPoint) -> Unit = {},
    onBackgroundDrag: (NormalizedPoint) -> Unit = {},
    onBackgroundDragEnd: () -> Unit = {},
    pinContent: @Composable (pin: MapViewerPin, selected: Boolean) -> Unit = { pin, selected ->
        DefaultMapViewerPinContent(pin, selected)
    },
) = MapViewer(
    state = state,
    content = content,
    items = pins.map { MapViewerItem.Pin(it) },
    selectedPinId = selectedPinId,
    onPinTap = onPinTap,
    onMapTap = onMapTap,
    modifier = modifier,
    backgroundDragSelectEnabled = backgroundDragSelectEnabled,
    onBackgroundDragStart = onBackgroundDragStart,
    onBackgroundDrag = onBackgroundDrag,
    onBackgroundDragEnd = onBackgroundDragEnd,
    pinContent = pinContent,
)

/**
 * Thin, non-breaking overload for every existing tile-mode caller: wraps [config] into
 * [MapContent.Tiles] and forwards to the pins-based [MapViewer] overload above. No behavior
 * change — see the [items]-based overload's KDoc.
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

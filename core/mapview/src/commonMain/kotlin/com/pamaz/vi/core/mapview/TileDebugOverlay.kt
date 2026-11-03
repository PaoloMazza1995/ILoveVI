package com.pamaz.vi.core.mapview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pamaz.vi.core.mapview.tile.TileEngine
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.core.model.map.visibleTiles
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Debug overlay (task's "report measured numbers" convention, applied to the tile pipeline):
 * current/tile zoom, tiles visible/in-memory/loading/failed, memory+disk hit rates, requests in
 * the last second — [com.pamaz.vi.core.mapview.tile.TileCacheMetrics]' own numbers, read via its
 * `StateFlow` rather than recomputed here. Off by default, and [MapViewBuildConfig
 * .debugOverlaySupported] makes it truly absent from a release build regardless of what a caller
 * passes — see that property's KDoc. [com.pamaz.vi.tools.mapeditor]'s hidden-shortcut toggle is
 * the one real caller; `feature:map` never enables this in the shipped app.
 *
 * Polls at a fixed, deliberately low rate ([RefreshIntervalMs]) via its own `LaunchedEffect`
 * rather than recomputing on every draw frame like [TileLayer] does — this is a debug-only
 * readout, not something the 60fps gesture path should ever pay for, so it's fine (and simpler) to
 * let it recompose normally.
 */
@Composable
internal fun TileDebugOverlay(
    scheme: TileScheme,
    state: MapViewerState,
    engine: TileEngine,
    nativeMinZoom: Int,
    nativeMaxZoom: Int,
    modifier: Modifier = Modifier,
) {
    if (!MapViewBuildConfig.debugOverlaySupported) return

    var text by remember { mutableStateOf("") }
    val metrics by engine.metrics.snapshot.collectAsState()
    val loadingKeys by engine.coordinator.loadingKeys.collectAsState()
    val failedKeys by engine.coordinator.failedKeys.collectAsState()

    LaunchedEffect(engine) {
        while (true) {
            val viewportSize = state.viewportSize
            val tileZoom =
                min(state.zoom, nativeMaxZoom.toFloat()).coerceAtLeast(nativeMinZoom.toFloat())
            val visibleCount = if (viewportSize.width > 0 && viewportSize.height > 0) {
                scheme.visibleTiles(
                    center = state.center,
                    zoom = tileZoom,
                    viewportWidthPx = viewportSize.width.toFloat(),
                    viewportHeightPx = viewportSize.height.toFloat(),
                ).tiles.size
            } else {
                0
            }
            text = buildString {
                append("zoom ${(state.zoom * 100).roundToInt() / 100f} (tile ${tileZoom.roundToInt()})\n")
                append("visible $visibleCount  memory ${engine.memoryCache.entryCount} (${engine.memoryCache.pinnedCount} pinned)\n")
                append("loading ${loadingKeys.size}  failed ${failedKeys.size}\n")
                append("hit rate ${(metrics.cacheHitRate * 100).roundToInt()}%  reqs/s ${metrics.requestsLastSecond}\n")
                append("mem ${metrics.memoryHits} disk ${metrics.diskHits} net ${metrics.networkFetches} fail ${metrics.failures}")
            }
            delay(RefreshIntervalMs)
        }
    }

    Column(
        modifier = modifier
            .padding(8.dp)
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(8.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
        )
    }
}

private const val RefreshIntervalMs = 250L

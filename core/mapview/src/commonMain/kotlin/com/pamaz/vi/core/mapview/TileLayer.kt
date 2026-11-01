package com.pamaz.vi.core.mapview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.core.model.map.TileUrl
import com.pamaz.vi.core.model.map.visibleTiles
import com.pamaz.vi.design.IloveViColors
import kotlin.math.roundToInt

/**
 * A tile is drawn very slightly larger than its own [TileRange.tileScreenSizePx]-derived size and
 * centered on its unmodified position, so neighboring tiles overlap by half this amount on every
 * shared edge instead of leaving a sub-pixel gap between them. [TileRange.tiles] positions each
 * tile at `tileIndex * tileScreenSizePx` offset from the viewport origin; when that scale isn't an
 * exact integer number of pixels (the common case at almost any zoom level, since it's a
 * continuous float, not snapped to whole pixels), each tile's *own* rounded position is internally
 * consistent but the *rounded delta* between two adjacent tiles can be off by a pixel from the
 * true (fractional) tile size — visible as a thin seam/grid line at every tile boundary. This is
 * the same fixed-overlap technique standard slippy-map libraries use for the same reason; a tiny,
 * fixed overlap is cheaper and more robust than trying to compute a per-tile size that makes every
 * adjacent pair's rounded boundary line up exactly.
 */
private val TileOverlap: Dp = 1.dp

/**
 * Draws every tile [TileScheme.visibleTiles] reports for the current viewport/center/zoom.
 *
 * Each tile is a flat [IloveViColors.bg800] box with [AsyncImage] layered on top, the same
 * "neutral placeholder drawn first, image on top" idiom `core/design`'s `NewsThumbnail` uses: while
 * a tile is loading, or if it never loads, the placeholder box simply shows through underneath —
 * no separate loading/error state plumbing, and one failed tile can never affect any other, since
 * each is an independent [AsyncImage] request. A tile that failed and scrolls back into view gets
 * a brand new composable instance (see the [key] below) and therefore a fresh request, so it's
 * retried automatically rather than staying failed forever.
 *
 * Coil3's default [coil3.ImageLoader] already does both memory and disk caching, keyed by the
 * request's `model` (here, the tile's URL) — [TileUrl.build] already encodes the map version,
 * zoom and tile coordinates into that URL, so no custom cache key is needed.
 */
@Composable
internal fun TileLayer(
    scheme: TileScheme,
    tileUrlTemplate: String,
    state: MapViewerState,
    modifier: Modifier = Modifier,
) {
    val viewportSize = state.viewportSize
    if (viewportSize.width <= 0 || viewportSize.height <= 0) return

    val tileRange = remember(scheme, state.center, state.zoom, viewportSize) {
        scheme.visibleTiles(
            center = state.center,
            zoom = state.zoom,
            viewportWidthPx = viewportSize.width.toFloat(),
            viewportHeightPx = viewportSize.height.toFloat(),
        )
    }

    val density = LocalDensity.current
    val tileSizeDp = remember(density, tileRange.tileScreenSizePx) {
        with(density) { tileRange.tileScreenSizePx.toDp() }
    }
    val tileDrawSizeDp = tileSizeDp + TileOverlap
    val halfOverlapPx = with(density) { (TileOverlap / 2).toPx() }

    Box(modifier = modifier) {
        for (tile in tileRange.tiles) {
            key(tile.tileX, tile.tileY, tileRange.zoom) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (tile.screenX - halfOverlapPx).roundToInt(),
                                (tile.screenY - halfOverlapPx).roundToInt(),
                            )
                        }
                        .size(tileDrawSizeDp)
                        .background(IloveViColors.bg800),
                ) {
                    AsyncImage(
                        model = TileUrl.build(
                            tileUrlTemplate,
                            tileRange.zoom,
                            tile.tileX,
                            tile.tileY
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(tileDrawSizeDp),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

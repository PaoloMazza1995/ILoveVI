package com.pamaz.vi.core.mapview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.design.IloveViColors
import kotlin.math.roundToInt

/**
 * Draws single-image mode's one [image], positioned via the exact same (center, zoom, viewport)
 * transform [TileLayer]/[PinLayer] use — see [singleImageTileScheme]'s KDoc for how a plain
 * [TileScheme] stands in for a real tile pyramid here, so this is the same
 * [com.pamaz.vi.core.model.map.MapViewport.toScreen] formula applied to the image's `(0, 0)`
 * corner instead of a per-tile offset.
 *
 * Drawn on an [IloveViColors.bg800] background (same "neutral surface, image on top" idiom
 * [TileLayer] uses): [singleImageMinZoom]'s "cover" fit keeps both axes filling the viewport at
 * every zoom in normal use, so this is mostly a brief placeholder for the one frame before layout
 * (`dstSize` rounding could in principle leave a sub-pixel gap at an extreme aspect ratio) rather
 * than a routinely-visible margin.
 */
@Composable
internal fun SingleImageLayer(
    image: ImageBitmap,
    scheme: TileScheme,
    state: MapViewerState,
    modifier: Modifier = Modifier,
) {
    val viewportSize = state.viewportSize
    if (viewportSize.width <= 0 || viewportSize.height <= 0) return

    Canvas(modifier = modifier.fillMaxSize().background(IloveViColors.bg800)) {
        val scale = scheme.scaleAt(state.zoom).toFloat()
        val scaledWidth = scheme.widthPx * scale
        val scaledHeight = scheme.heightPx * scale
        val left = size.width / 2f - state.center.x * scaledWidth
        val top = size.height / 2f - state.center.y * scaledHeight

        drawImage(
            image = image,
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(
                scaledWidth.roundToInt().coerceAtLeast(1),
                scaledHeight.roundToInt().coerceAtLeast(1),
            ),
            // Medium, not the drawImage default (Low/nearest-neighbor-ish): this image is panned
            // and zoomed continuously, often well below its native size, and Medium is the
            // cheapest quality step up that stays smooth while panning/zooming every frame.
            filterQuality = FilterQuality.Medium,
        )
    }
}

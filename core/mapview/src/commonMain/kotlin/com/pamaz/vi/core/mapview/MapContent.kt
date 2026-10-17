package com.pamaz.vi.core.mapview

import androidx.compose.ui.graphics.ImageBitmap
import com.pamaz.vi.core.model.MapConfig

/**
 * What [MapViewer] renders: either a remote tile pyramid, or one bundled/local image with no
 * tiling, network or backend at all (step 5A-bis). Both branches share the same camera
 * ([MapViewerState]), gesture handling and pin layer — only how the map's pixels get drawn
 * differs. [com.pamaz.vi.core.model.NormalizedPoint] pin positions are defined relative to the
 * full image either way, so a pin placed in [SingleImage] mode needs no recalculation if the same
 * map later ships as [Tiles].
 */
sealed interface MapContent {
    /**
     * One full image, already decoded, with no tiling/network/backend involved — used for the
     * bundled dev map ([rememberDevMapImage]) and, on desktop, a maintainer-chosen local PNG
     * (`loadSingleImageMapContentFromFile`, for `:tools:map-editor`).
     */
    data class SingleImage(val image: ImageBitmap) : MapContent

    /**
     * A remote tile pyramid described by [config]. This is the only field the existing
     * [TileLayer]/[MapViewer] ever needed — no separate "tile source" abstraction exists in this
     * module today (just [MapConfig.tileUrlTemplate] + Coil), so [Tiles] deliberately doesn't
     * invent one here either; see this step's summary for why.
     */
    data class Tiles(val config: MapConfig) : MapContent
}

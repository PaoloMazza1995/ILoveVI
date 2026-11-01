package com.pamaz.vi.feature.map.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.core.model.map.TileUrl

/**
 * MST-04: "the lowest zoom levels are downloaded at first launch so the map is never blank
 * offline." `:core:mapview`'s `TileLayer` already caches whatever tiles it happens to load (Coil3's
 * default `ImageLoader`, keyed by URL — see that module's README), but nothing fetches a tile
 * before it's actually on screen; a device that goes offline *before* ever panning/zooming to a
 * given area would still show a placeholder there. This warms the cache for every tile at
 * [MapConfig.minZoom] — by this module's own `TileScheme` convention, that level is always either
 * the single overview tile or a small handful of tiles, never the full high-resolution set — so
 * the map is never completely blank even on a first, offline-from-the-start launch.
 *
 * Fire-and-forget: [coil3.ImageLoader.enqueue] results aren't awaited or retried here. A failed
 * prefetch (e.g. genuinely no connectivity at all on first launch) simply means that tile stays a
 * placeholder until a later, successful load — exactly the same "a failed tile doesn't break the
 * map" behavior `TileLayer` already guarantees for every other tile.
 */
@Composable
internal fun PrefetchLowestZoomTiles(mapConfig: MapConfig, scheme: TileScheme) {
    val platformContext = LocalPlatformContext.current
    LaunchedEffect(mapConfig.mapVersion, mapConfig.tileUrlTemplate) {
        val imageLoader = SingletonImageLoader.get(platformContext)
        val minZoom = mapConfig.minZoom
        for (tileX in 0 until scheme.tilesWide(minZoom)) {
            for (tileY in 0 until scheme.tilesHigh(minZoom)) {
                val url = TileUrl.build(mapConfig.tileUrlTemplate, minZoom, tileX, tileY)
                imageLoader.enqueue(ImageRequest.Builder(platformContext).data(url).build())
            }
        }
    }
}

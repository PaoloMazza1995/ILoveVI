package com.pamaz.vi.core.mapview

/**
 * How many zoom levels beyond a map's native maximum the user can still zoom in — tiles at the
 * native maximum are simply enlarged (smooth-filtered) past that point; see [TileLayer]'s own
 * KDoc for the rendering side of this. Public re-export of
 * [com.pamaz.vi.core.mapview.tile.TileCacheConfig.OVERZOOM_LEVELS] (`internal`, so this module's
 * own tile pipeline doesn't leak its internals) so callers like `:tools:map-editor`'s "Load map
 * image..."/"Map info" dialogs can show the "1 image pixel = 1 screen pixel at zoom N; beyond it
 * the map is enlarged" copy and the total detail ratio without needing access to this module's
 * internal `tile` package — task items 7b/7c.
 */
const val TILE_OVERZOOM_LEVELS: Int = com.pamaz.vi.core.mapview.tile.TileCacheConfig.OVERZOOM_LEVELS

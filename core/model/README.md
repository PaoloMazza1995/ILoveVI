# :core:model

Pure Kotlin data model shared by every app module: `Entry` (`Weapon`/`Vehicle`/`Item`), `MapPin`/
`MapConfig`,
`Station`/`Song` and the news-feed types. No platform code, no DI, no I/O — `commonMain` only.

Conventions used throughout this module:

- Dates use `kotlin.time.Instant`.
- Ids are `@JvmInline value class`es wrapping a `String`, serializing as a plain JSON string.
- Invalid states are rejected in an `init` block (`require(...)`) rather than clamped or defaulted,
  so a bad upstream value surfaces immediately instead of being silently absorbed.

## Map math

Package `com.pamaz.vi.core.model.map` turns a `MapPin.position` (a `NormalizedPoint`, 0..1) into the
tile coordinates and screen pixels needed to actually draw the map, without touching any existing
model class:

- `TileScheme(widthPx, heightPx, tileSize = 256)` — describes how the source map image is cut into
  tiles. `minZoom` is always `0` (the whole image fits in one tile); `maxZoom` is the level where
  the image is shown 1:1. `tilesWide(z)`/`tilesHigh(z)` give the tile grid size at any level in
  between.
- `TileScheme.tilePosition(point, zoom)` / `TileScheme.normalizedPoint(tile)` — convert a
  `NormalizedPoint` to/from a `TilePosition` (tile index + in-tile pixel offset) at a given zoom
  level.
- `TileScheme.visibleTiles(center, zoom, viewportWidthPx, viewportHeightPx)` — the `TileRange`
  (which tiles to fetch, and the screen offset to draw each one) needed to fill a viewport. Supports
  fractional zoom (pinch gestures) by snapping to the nearest integer tile level and scaling the
  tile's drawn size by the leftover factor. Clamped to the image bounds, so panning past an edge
  shows blank space rather than wrapping or requesting out-of-range tiles.
- `TileUrl.build(template, z, x, y)` — substitutes `{z}`/`{x}`/`{y}` into a
  `MapConfig.tileUrlTemplate`.
- `MapViewport` — continuous (non-tile-snapped) `NormalizedPoint` <-> on-screen pixel conversion for
  a given center/zoom/viewport size, plus `clampZoom` to keep a zoom gesture within `TileScheme`'s
  range.

No user/player position is modeled anywhere in this module — the map only ever places content pins.

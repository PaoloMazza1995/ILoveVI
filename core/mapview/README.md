# :core:mapview

A shared Compose Multiplatform map viewer: pan, pinch/scroll/double-tap zoom, tile loading, and a
pin layer. `commonMain` only — pure UI, no repositories, no network, no database.

**Why this module exists outside the roadmap's module list:** both the admin placement tool
(`:tools:map-editor`, where pins are placed on the map) and `feature:map` (where players browse the
map) need the exact same pan/zoom/tile/pin rendering behavior — placing a pin has to show up in
exactly the same spot the player-facing map will later show it. Rather than duplicate that logic (or
have one depend on the other), it's factored out here as its own small module, consumed by both.
This step (5A) only builds the module itself; wiring it into either consumer is a later step.

## Dependencies

`:core:model` (tile/viewport math, `MapConfig`/`NormalizedPoint`/`EntryCategory`), `:core:design`
(`IloveViMapPin`, `IloveViClusterBadge`, colors, shapes, typography — no new colors/fonts/spacing
are introduced here) and Coil 3 (`coil-compose` only; the network engine comes transitively from
whichever app module eventually depends on `:core:design`, which already supplies it per-platform).
Deliberately **not** a dependency: any database, network-repository or `feature:*` module — this
module only knows about `MapConfig`, `NormalizedPoint` and its own `MapViewerPin`/
`MapViewerCluster`, never about entries, repositories or sync.

## Public API

- `rememberMapViewerState(initialCenter, initialZoom, minZoom, maxZoom): MapViewerState` — the
  camera (center/zoom/viewport size). `minZoom`/`maxZoom` are plain `Float`s rather than a
  `MapConfig`, so the state and its zoom-clamping behavior can be unit tested without constructing
  one; in practice they come straight from `config.minZoom.toFloat()`/`config.maxZoom.toFloat()`.
  `state.animateTo(point, zoom)` eases to a target over 300ms; `state.animateToBounds(scheme,
  bounds, paddingPx)` picks whichever zoom makes a `NormalizedBounds` fit the viewport (clamped to
  `minZoom`/`maxZoom`) and eases there — see "Cluster support" below. `state.viewportSize` is
  updated by `MapViewer` itself on every layout pass.
- `MapViewer(state, content, items, selectedPinId, onPinTap, onClusterTap, onMapTap, modifier,
  pinContent)` — the viewer. `items: List<MapViewerItem>` mixes pins and clusters freely; a thin
  `pins: List<MapViewerPin>` overload (no clusters) still exists for every caller that doesn't have
  any, and a `config: MapConfig` overload on top of that for tile-mode callers — see "Cluster
  support" below for why there are three overloads, not one.
- `MapViewerPin(id, position, category, label)` — what `MapViewer` draws for a single marker. Not
  `com.pamaz.vi.core.model.MapPin`: it carries no `EntryRef`, database id shape, `mapVersion` or
  `updatedAt`, only what's needed to draw and hit-test a dot. Callers map their own domain types
  into this shape.
- `MapViewerCluster(id, position, count, dominantCategory, bounds)` — what `MapViewer` draws for a
  group of merged pins. `dominantCategory` is `null` for a mixed-category cluster (drawn in
  `IloveViClusterBadge`'s neutral brand color); `bounds: NormalizedBounds` is the normalized
  rectangle the merged pins span, consumed by `animateToBounds`.
- `MapViewerItem` — `sealed interface { Pin(pin), Cluster(cluster) }`, what `items` is a list of.

## Cluster support

This module draws and hit-tests clusters; it has **no clustering logic** — which pins merge, at
which zoom, by what distance threshold, is entirely `feature:map`'s decision (step 11, not started
yet). `feature:map` hands this viewer the result as a `List<MapViewerItem>`: a `MapViewerItem.Pin`
for every pin that zoom/viewport didn't merge, a `MapViewerItem.Cluster` for every group it did.

- **Drawing**: pins and clusters share one `PinLayer` canvas/overlay, positioned in screen space by
  the exact same `MapViewport.toScreen` conversion either way (see "Pins and clusters" below).
  Clusters are always drawn with `:core:designsystem`'s `IloveViClusterBadge` (count capped at
  `"99+"`, `IloveViColors.brandPurple` when `dominantCategory` is `null`) — not customizable via
  `pinContent` or an equivalent hook, since nothing has asked for that yet. **Draw order**: every
  cluster first, then every non-selected pin, then the selected pin last, so the selected pin is
  always on top of any cluster (and of every other pin) regardless of `items`' own order.
- **Hit testing**: `hitTestCluster` mirrors `hitTestPin` exactly (same 44dp floor, same "topmost
  wins on overlap" rule) — a tap checks pins first (they're always drawn on top), then clusters,
  then falls back to `onMapTap`. `IloveViClusterBadge` already bundles its own 44dp hit target,
  click handling and semantics content description (`"{count} places, tap to zoom in"`, English +
  Italian) internally, unlike `IloveViMapPin` which is purely visual — `PinLayer` only has to
  position it, not wrap it in a second clickable/semantics layer.
- **"Zoom in on this cluster"**: `onClusterTap` only *reports* the tap, same as `onPinTap`/
  `onMapTap` — the viewer doesn't react on its own. `MapViewerState.animateToBounds(scheme, cluster
  .bounds, paddingPx)` is the reusable helper a caller wires to `onClusterTap` to actually zoom in:
  it picks the largest zoom at which `bounds` still fits the viewport (inset by `paddingPx`),
  clamped to `minZoom`/`maxZoom`, and eases there via the same `animateTo` pan/pinch already uses.
  `scheme` is a parameter, not stored — same reason `MapGeometry.kt`'s pure functions take it as a
  parameter: `MapViewerState` stays scheme-agnostic (see its own KDoc).
- **Three `MapViewer` overloads, not one**: the real implementation takes
  `items: List<MapViewerItem>`; a `pins: List<MapViewerPin>` overload wraps each into
  `MapViewerItem.Pin` and forwards, so every existing all-pins caller and preview keeps compiling
  with no changes; the pre-existing
  `config: MapConfig` overload forwards to that one, unchanged. (These don't collide on the JVM/
  Android targets despite `List<MapViewerPin>`/`List<MapViewerItem>` erasing to the same raw `List`
  type, because the `items` overload also has two more parameters — `onClusterTap`, no `pinContent`
  equivalent needed for clusters — so its generated JVM signature has a different arity regardless
  of the shared prefix's erasure.)

## Tile pipeline

Replaces this module's original Coil3-`AsyncImage`-per-tile approach (see git history) after it was
confirmed to be the cause of visible reload flicker/black squares while zooming or panning with a
real remote map. The fix is in `tile/` (all `internal`) plus `TileLayer.kt`'s own draw phase.

### Diagnosis (what was actually wrong)

Four confirmed root causes, found by reading the old implementation against the symptom (not
assumed):

1. **No hysteresis on the tile zoom level.** The old code picked the integer tile zoom by rounding
   the continuous camera zoom *every frame*. Holding a pinch near e.g. zoom 2.0 made that rounding
   flip between 2 and 3 on tiny finger tremor, and every flip changed every visible tile's identity
   at once — a full tear-down/rebuild of the whole visible tile set, which is what actually read as
   "tiles regenerating."
2. **State read in composition, not the draw phase.** `state.zoom`/`state.center` being read inside
   a `@Composable` body (rather than inside a `Canvas`/`DrawScope` lambda) meant every pinch/pan
   frame recomposed the tile layer's whole subtree, re-issuing every tile's Coil request from
   scratch — not just redrawing what was already there.
3. **Cancel-and-restart every gesture frame.** Combined with (2), each recomposition could cancel
   in-flight Coil requests for tiles still genuinely needed, immediately re-requesting them — wasted
   network/decode work and a flash of "unloaded" in between.
4. **No fallback rendering at all.** A tile not yet decoded (or one that failed) showed nothing —
   Coil3's own "crossfade from empty" only has a *lower-resolution image* to fade from if something
   actually draws one; nothing in the old code ever did. Hence the black/empty squares, not just
   slow loads.

[`TileZoomSelector`](src/commonMain/kotlin/com/pamaz/vi/core/mapview/tile/TileZoomSelector.kt) fixes
(1); drawing everything in one `Canvas` lambda inside [`TileLayer`](
src/commonMain/kotlin/com/pamaz/vi/core/mapview/TileLayer.kt) fixes (2); the debounced
cancellation/request-stability logic in [`TileLoadCoordinator`](
src/commonMain/kotlin/com/pamaz/vi/core/mapview/tile/TileLoadCoordinator.kt) fixes (3); ancestor
fallback rendering (same file as (2)) fixes (4).

**Three more, found by actually running the app** (none of the above required a real device to
catch; these did — no unit test can exercise a `DrawScope` lambda's actual redraw timing):

5. **The map only ever updated on a drag/pinch, never on its own.** `TileLayer`'s `Canvas` reads
   `TileMemoryCache.peek(key)` — a plain function call into a [`MutableStateFlow`](
   src/commonMain/kotlin/com/pamaz/vi/core/mapview/tile/TileMemoryCache.kt), not a Compose
   [State][androidx.compose.runtime.State] read. A tile finishing its decode in the background
   therefore never told the canvas to redraw; the map only ever picked it up as a side effect of
   `state.zoom`/`state.center` changing (which *are* draw-observed) during some later gesture —
   visible as pins (pure position math, no cache dependency) appearing to move before the map image
   itself did, since the map was really just catching up on everything that had quietly finished
   loading. Fixed by `TileMemoryCache.generation`, a real
   [`mutableIntStateOf`][androidx.compose.runtime.mutableIntStateOf]-backed counter bumped on every
   cache mutation; `TileLayer`'s draw lambda reads it once per frame purely to register the
   dependency, so a tile arriving now redraws on its own.
6. **Tiles never scaled continuously while pinching, only past the native zoom ceiling.** The scale
   factor applied to the rendered tile grid was `2^(continuousZoom - min(continuousZoom,
   nativeMaxZoom))` — exactly `1.0` for the *entire* native zoom range. Tiles stayed frozen at the
   discrete, hysteresis-selected render level's fixed pixel size for most of a pinch (the render
   level only updates once hysteresis lets it flip), so the map didn't visually zoom at all until
   that sudden jump — you were staring at a static, unscaled tile grid, which is exactly why its
   seams were visible. Fixed in [`tileRenderScaleFactor`](
   src/commonMain/kotlin/com/pamaz/vi/core/mapview/TileLayer.kt): `2^(continuousZoom -
   renderLevel)`, continuously, covering both "mid-pinch within native range" and "past the native
   max" (overzoom) with one formula — extracted as a pure function (`TileRenderScaleFactorTest`)
   specifically because the inline version, living only inside a `DrawScope` lambda, was untestable
   without a real Compose UI harness.
7. **Residual 1px seams from independent per-tile rounding.** Each tile's destination rect was
   rounded independently (`screenX.roundToInt()`, `tileScreenSizePx.roundToInt()`); mostly gap-free
   in principle (adjacent tiles differ by an exact integer, which rounds consistently), but visible
   once (6) above started actually scaling the canvas up. Each tile's `dstSize` now overlaps its
   neighbor by 1px — the standard fix for any tiled-image renderer.

### Cache sizes

[`TileCacheConfig`](src/commonMain/kotlin/com/pamaz/vi/core/mapview/tile/TileCacheConfig.kt) is
every tunable, in one place:

|                             | Mobile (Android/iOS) | Desktop/JVM |
|-----------------------------|----------------------|-------------|
| Decoded-bitmap memory cache | 96 MB                | 256 MB      |
| Downloaded-tile disk cache  | 500 MB               | 2 GB        |

Zoom levels `0..min(nativeMaxZoom, 3)` (capped at 85 tiles total, independent of aspect ratio) are
**pinned**: never evicted from the memory cache, regardless of the byte budget, and preloaded the
moment a map opens (`TileEngine.preloadPinnedLevels` — the same thing the app already did for
offline use, reused here rather than reimplemented). This guarantees `findAncestorFallback` (below)
always finds *something* to draw once a map has finished its initial load — the walk up never has to
go further than the nearest pinned level.

The disk cache is keyed by the tile's full URL and never revalidates a hit: every map version gets
its own storage folder (`:tools:tilegen`'s README), so a URL's bytes are immutable once uploaded — a
cache hit today is valid forever, no conditional-GET round trip needed.

**Mobile memory safety**: on a low-memory signal (Android `onTrimMemory`/`onLowMemory`, iOS
`UIApplication.didReceiveMemoryWarningNotification` — `observeLowMemorySignal`, a no-op on desktop),
the memory cache immediately drops everything except the pinned overview levels
(`TileMemoryCache.clearUnpinned`), so the map never goes fully blank even under real memory
pressure, and rapid zooming can't pile up decoded bitmaps faster than normal LRU eviction would
otherwise catch up with.

### Loading policy

[`TileLoadCoordinator`](src/commonMain/kotlin/com/pamaz/vi/core/mapview/tile/TileLoadCoordinator.kt)
owns all of this:

- **Priority**: viewport tiles at the target zoom first, then a one-tile prefetch ring around the
  viewport, then the next zoom level's center tile once the current level has settled — three FIFO
  queues, always drained in that order.
- **Bounded parallelism** (`MAX_PARALLEL_REQUESTS = 8`), retry with exponential backoff
  (`RETRY_MAX_ATTEMPTS = 3`) for a failed tile — a failure never breaks the map, and a failed tile
  gets another chance the moment it's requested again as a viewport tile (re-entering view).
- **In-flight de-duplication**: the same `(mapVersion, zoom, x, y)` key is only ever fetched once,
  no matter how many times `request()` is called for it while it's already queued/in-flight — every
  waiter shares the one underlying fetch.
- **Debounced cancellation** (`VIEWPORT_STABILITY_DELAY_MS = 120`): a tile that's fallen out of view
  is only cancelled once nothing has asked for it for 120ms, not the instant it's not visible — a
  fast pinch/pan settles before anything is torn down.
- **Debounced *new-level* requests** (`ZOOM_LEVEL_REQUEST_STABILITY_MS = 80`): during an organic
  (finger-driven) zoom, a new integer level's tiles only start downloading once the zoom has sat
  still for 80ms — an animated zoom (buttons, double-tap) skips this entirely, since its target
  level is known the instant the animation starts. Fallback *drawing* (below) still updates every
  frame regardless; this debounce only gates new network/disk work.

**Thread safety.** With up to 8 fetches genuinely running in parallel, their completions resume on
whatever thread the HTTP engine happens to use — a real, `MapViewerCameraStabilityUiTest`-caught
`ConcurrentModificationException` the first version of this coordinator had, from several of those
completions mutating a plain `HashMap`/`ArrayDeque` at once with no synchronization. The fix:
`TileLoadCoordinator`'s own queues/maps are touched *only* from one coroutine (started once, in
`init`), reached exclusively through a `Channel` — `request()` and a fetch job's completion both
just `Channel.trySend` into it, which is lock-free and safe from any thread by construction. This
reuses the exact confined-ownership pattern `MapGestures.kt`'s own `pendingSnaps` channel already
established for the camera, rather than inventing a lock for the same class of problem.

### Fallback rendering (no more black squares)

[`TileLayer`](src/commonMain/kotlin/com/pamaz/vi/core/mapview/TileLayer.kt) draws every visible tile
position at the target zoom from memory if it's there; otherwise it walks up to the nearest ancestor
tile actually resident (`findAncestorFallback`, z-1, z-2, ... down to the pinned levels, which are
guaranteed present), crops the right quarter/eighth/... of it, and scales that up — exactly the
"previous, lower-resolution image stays visible, enlarged" behavior every slippy map (Google Maps
included) uses instead of a blank tile. Once the real tile arrives, it cross-fades in over
`CROSSFADE_DURATION_MS = 150`ms (skipped — straight to the real tile — for a tile that was already
resident, and for every tile when the system's reduced-motion setting is on;
`TileCacheConfig`'s own KDoc has the exact mechanism). Everything — fallback, crossfade, real tile —
draws in one `Canvas`/`DrawScope` lambda, with `state.zoom`/`state.center` read *inside* that
lambda, never in `TileLayer`'s own composable body: a pinch/pan never recomposes this tree, only the
draw phase re-runs.

### Overzoom

`TileCacheConfig.OVERZOOM_LEVELS = 3` — the user's camera zoom can go 3 levels past a map's native
maximum (up to 8x enlargement): past that point, `TileLayer` simply scales up the sharpest real
tiles available (`FilterQuality.Medium`), via one `Canvas.scale(...)` wrapping the whole tile draw
loop. `MapViewer`'s camera zoom ceiling is `nativeMaxZoom + OVERZOOM_LEVELS`; `TileLayer`/
`TileLoadCoordinator` only ever request/render tiles at `min(cameraZoom, nativeMaxZoom)` — no tile
coordinate beyond the real pyramid is ever asked for. Pan limits (`centerBoundsForAxis`) stay
correct at an overzoomed scale for free: `TileScheme.scaleAt(zoom: Float)` has no upper clamp, so
the same `2^(zoom - maxZoom)` formula just keeps extrapolating past the native ceiling.

[`TILE_OVERZOOM_LEVELS`](src/commonMain/kotlin/com/pamaz/vi/core/mapview/TileOverzoom.kt) is a
public re-export of the same constant, for callers like `:tools:map-editor`'s "Load map image.../Map
info" dialogs that need to show the detail ratio without reaching into this module's internal `tile`
package.

### Debug overlay

[`TileDebugOverlay`](src/commonMain/kotlin/com/pamaz/vi/core/mapview/TileDebugOverlay.kt) (off by
default; `debugOverlayEnabled` on `MapViewer`) shows current/tile zoom, tiles visible/in-memory/
loading/failed, memory+disk hit rates and requests/sec — `TileCacheMetrics`'s own numbers, polled at
a slow fixed rate (not every draw frame). `MapViewBuildConfig.debugOverlaySupported` makes it truly
*absent* from a release build of the app, regardless of what a caller passes for
`debugOverlayEnabled` — the same release gate `MapViewBuildConfig.singleImageDevModeEnabled` already
uses. `:tools:map-editor`'s `MapArea.kt` is the one real caller: a hidden `Ctrl+Shift+D` shortcut
toggles it (no visible button/menu entry — this is a debug tool, not a product feature);
`:feature:map` never enables this in the shipped app.

### Scripted benchmark

[`rememberTileBenchmarkTrigger`](
src/commonMain/kotlin/com/pamaz/vi/core/mapview/TileBenchmarkRunner.kt) (public `@Composable`) zoom-
sweeps a fresh, throwaway tile engine from a map's native min to max zoom and back, twice, and
reports requests/sec, duplicate network fetches (should be `0`), the second pass's cache hit rate,
and simulated per-step frame-time percentiles — the TESTS section's "scripted benchmark in the
editor (debug only)". `:tools:map-editor`'s `MapArea.kt` wires it to a second hidden shortcut
(`Ctrl+Shift+B`), printing the report to stdout; `:feature:map` never calls this. The underlying
`runTileZoomSweepBenchmark`/`TileBenchmarkResult` (both `internal`) are unit-tested against a fake
`TileFetcher` in `TileBenchmarkTest`.

## Behavior notes

- **Tiles**: see "Tile pipeline" above for the full loading/fallback/overzoom story.
  `TileScheme.visibleTiles` (from `:core:model`) still decides which grid of tiles is needed at a
  given zoom; `TileUrl.build` still turns each into a URL.
- **Pins and clusters**: one overlay layer (`PinLayer`), each pin/cluster offset from its
  `NormalizedPoint` via the exact same `MapViewport.toScreen` conversion the tile layer's math is
  built on — pins, clusters and tiles can never drift apart at any zoom, since all three derive from
  the same `TileScheme`/center/zoom. Items outside the viewport (plus a margin) aren't composed at
  all, which is most of what keeps 1000+ pins smooth (see "What was and wasn't verified" below) —
  see "Cluster support" above for drawing/hit-testing details specific to clusters.
- **Gestures**: pan + pinch-zoom + fling live in one custom gesture loop (not the stock
  `detectTransformGestures`) so a real pointer-timestamp-fed `VelocityTracker` can drive the fling;
  tap/double-tap use the standard `detectTapGestures`; scroll-wheel zoom is a third, independent
  `pointerInput` block gated on `PointerEventType.Scroll` (a no-op on touch-only platforms, which
  never emit it) — see `MapGestures.kt`'s KDoc for why these are three separate blocks rather than
  one. All zoom changes are focused on the pinch centroid / cursor / double-tap point via
  `computeFocalZoomCenter`, a pure, unit-tested function.
- **No user position**: there is no location/permission concept anywhere in this module, by design.
- **Accessibility**: the viewer root has a content description explaining the gestures; every pin
  has a merged semantics node with a content description (`"<label or category>, <category>"`)
  and a semantics `onClick` action (not a competing `clickable`), so assistive tech can both read
  and activate a pin without a second gesture detector fighting the main one for touch events. Every
  cluster gets the same treatment for free from `IloveViClusterBadge` itself (content description
  `"{count} places, tap to zoom in"`, English + Italian, plus its own `onClick`).

## What was and wasn't verified here

This is a sandboxed, display-less environment: there is no Android/iOS device or emulator, no
Compose Desktop harness in this repo, and therefore no way to actually render this UI or measure a
real frame time from here. What *was* verified:

- The module builds on every configured target (JVM, Android, iOS).
- All logic that doesn't require real rendering — zoom clamping, `animateTo`/`animateToBounds`
  (driven deterministically via a manually-ticked `BroadcastFrameClock`, since `Animatable` needs a
  real frame clock), focal-point zoom math (verified by round-tripping through `MapViewport` and
  asserting the focal point lands back under the same screen position), pin *and cluster*
  hit-testing at multiple zoom levels and on overlap, that a pin's/cluster's screen position is
  literally `MapViewport.toScreen`'s output, and that `animateToBounds` always lands within
  `minZoom`..`maxZoom` (including the two edge cases: a degenerate single-point bounds, and a bounds
  whose natural fit zoom would fall below `minZoom`) — is unit tested in `commonTest`.
- A synthetic 1000-pin benchmark (`MapViewerPerfTest`) measuring the *pure computation* cost of one
  frame's tile+pin math (no rendering): **~0.11ms/frame average on the JVM**, far under a 16ms/60fps
  budget. This proves the math scales fine; it is explicitly **not** a measured on-device rendered
  frame time, which this environment cannot produce.

**Still to confirm on a real device/emulator before shipping**: actual rendered frame times while
panning/pinching with 1000 pins; that the three independent gesture `pointerInput` blocks
(pan/pinch/ fling, tap/double-tap, scroll-wheel) don't fight each other in practice on a real
touchscreen; the `MapViewerWithOneThousandPinsPreview` / placeholder-tile previews rendering
correctly in Android Studio / Xcode preview; and (new with cluster support)
`MapViewerWithPinsAndClustersPreview` actually showing pins and badges together on a real screen,
that tapping a cluster badge visibly zooms in via `animateToBounds`, and that the "selected pin
always on top of a cluster" draw order looks right when they visually overlap.

**Tile pipeline, specifically**: `TileZoomSelector` (hysteresis), `TileMemoryCache` (LRU-by-bytes,
pinned-never-evicted), `findAncestorFallback` (crop rects for z-1/z-2/z-3), and
`TileLoadCoordinator` (in-flight dedup — 100 concurrent requests for one tile = 1 fetch; priority
drain order; retry/backoff; stability-delay sweep) are all unit-tested in `jvmTest`/`commonTest`
against fakes, with real coroutines/virtual time but no real network, display, or gesture input —
see each test file's own KDoc. The scripted zoom-sweep benchmark (`TileBenchmarkRunner.kt`,
`TileBenchmarkTest`) likewise proves its *measurement logic* (duplicate counting, hit-rate
calculation) against a fake, instant-success fetcher; its "requests/sec" and "frame time p50/p95ms"
numbers, when run for real via `:tools:map-editor`'s hidden shortcut, measure the pure-Kotlin cost
of tile-selection-and-request work (same honest scope as `MapViewerPerfTest`
above), not an actual rendered Compose frame — there is no display here to measure that from. **Not
yet confirmed**: the real root-cause fixes actually eliminating visible flicker against a real
remote map on a real device (the diagnosis above was derived by reading the old implementation
against the reported symptom, not by watching the bug reproduce and then disappear); the
low-memory-signal response actually preventing an OOM crash while zooming quickly on a real low-end
Android device/emulator.

## Used by the app module (step 8)

`MapViewerSampleScreen` (public, non-`@Preview`) and `MapViewBuildConfig` (public, not `internal`)
exist so `shared`'s Map tab can show this module's bundled dev map in debug builds — see
`shared/README.md`'s "Debug-only Map sample" section. `MapViewBuildConfig.singleImageDevModeEnabled`
is the one flag both modules key off of, rather than `shared` inventing a second one.

## Single-image mode (step 5A-bis)

`MapViewer` now takes a `content: MapContent` — either `MapContent.Tiles(config)` (the original,
network/tile-pyramid mode) or `MapContent.SingleImage(image)`: one already-decoded `ImageBitmap`, no
tiling, network or backend at all. Both branches share the exact same `MapViewerState`, gesture
handling (`mapViewerGestures`) and `PinLayer` — only which layer draws the map's pixels differs. The
old `MapViewer(state, config: MapConfig, pins, ...)` signature still exists as a thin overload that
wraps `config` into `MapContent.Tiles(config)`, so no existing caller needs to change.

**Why no `TileSource` field on `MapContent.Tiles`:** the original task sketch for this step included
one (`Tiles(val config: MapConfig, val source: TileSource)`), but no `TileSource` type exists
anywhere in this module — `TileLayer` only ever needed `MapConfig` (its tile URLs come straight from
`config.tileUrlTemplate` via Coil). `Tiles` was kept as just `config: MapConfig` to match what the
code actually needs and to preserve the "thin, non-breaking overload" requirement literally; a
`TileSource` abstraction can be added later if a concrete need for it shows up (e.g. swapping Coil
for a different fetch strategy).

**How single-image mode reuses tile-mode math unchanged:** `singleImageTileScheme(imageWidthPx,
imageHeightPx)` builds a plain `TileScheme` with `tileSize = max(imageWidthPx, imageHeightPx)`,
which makes `TileScheme.maxZoom` always `0` — so `TileScheme.scaleAt(zoom)` is exactly `2^zoom`
(zoom `0` == the image's native 1:1 scale). Feeding that `TileScheme` into the *existing*
`MapViewport`, `computeFocalZoomCenter`, `panCenter`, `hitTestPin` and `mapViewerGestures` makes
every one of them work correctly for a single image with zero changes — none of them know or care
that there's no real tile pyramid behind it. Zoom range: minimum is `singleImageMinZoom` (the zoom
at which the whole image "contain"-fits the viewport — recomputed via a `LaunchedEffect` whenever
the viewport is resized, e.g. rotation), maximum is `SINGLE_IMAGE_MAX_ZOOM` (`log2(8)`, i.e. 8x the
image's native size — `SINGLE_IMAGE_MAX_SCALE`).

**Pan clamping ("keep at least 25% visible"):** there is no *extra* pan-clamp function layered on
top of gestures. `MapViewerState.center` is already hard-bounded to `0f..1f` on both axes by its own
`Animatable` bounds, and across this module's entire zoom range that existing bound alone always
keeps at least 50% of the image (or viewport, whichever is smaller) visible — comfortably over the
required 25% floor. `visibleFraction(...)` in `SingleImageGeometry.kt` states and tests that
invariant explicitly rather than leaving it as an unstated side effect; see its KDoc for the proof
sketch. Adding a second, redundant clamp would just duplicate `mapViewerGestures` for no behavioral
change.

### The bundled dev image and its rights

`composeResources/drawable/map_dev.png` and `map_dev_fallback.png` are both **generated
placeholders** (a grid with coordinate labels and an unmissable watermark, produced the same way as
`:tools:tilegen`'s `PlaceholderMapImage`) — **neither is a real or fan-made GTA VI map**, so both
are safe to commit as-is; no `.gitignore` entry was needed for this step.

If a maintainer later drops a *real* dev map image in at `map_dev.png` to replace this placeholder,
confirm its usage rights first. **Only commit a replacement `map_dev.png` once its rights are
confirmed** — until then, add it to `.gitignore` instead and keep shipping this generated
placeholder, so a fresh checkout without that file still builds and runs (that's exactly what
`rememberDevMapImage`'s fallback chain is for: real image missing/corrupt → bundled
`map_dev_fallback.png` → an in-memory flat-color placeholder as the absolute last resort).

### Release safety

The dev image (and its fallback) are gated by `SINGLE_IMAGE_DEV_MODE_ENABLED`, a `const val`
**generated at build time** by this module's `generateMapViewBuildConfig` Gradle task — `true`
unless the build is invoked with `-Pilovevi.release=true`. Being a `const val`, it's inlined by the
Kotlin compiler at every call site: there is no runtime setting to accidentally leave on, only a
Gradle property to pass at build time.

This project has no established per-module Android `buildType`/`BuildConfig` convention yet (no
other module uses one), so this step didn't invent AGP-variant-specific wiring just for one flag — a
single cross-platform Gradle property was the smallest change that satisfies "a build-config flag,
not a runtime check." **Open item:** whoever wires up real release packaging (step 15, store
readiness) needs to actually pass `-Pilovevi.release=true` to that build — it is not automatic
today. Revisit this once Android/iOS release signing exists for real.

### Loading a local image on desktop

`loadSingleImageMapContentFromFile(file: File): MapContent.SingleImage` (JVM-only, in
`jvmMain/.../DesktopSingleImageLoader.kt`) runs the same decode-and-downscale pipeline
`rememberDevMapImage` uses, over any local PNG/JPEG — this is what `:tools:map-editor` (step 5B)
is expected to call for its "Open image" mode, so a pin-placement session can start from any local
image with no backend. Call it off the UI thread (e.g. from a coroutine on `Dispatchers.IO`).

## Trying it locally

The previews in `MapViewerPreview.kt` point at a *local* placeholder tile set (never a real or
fan-made game map — see the rights note in `:tools:tilegen`'s README). To make them resolve to
actual images rather than placeholder boxes, generate one first:

```sh
./gradlew :tools:tilegen:run --args="generate-placeholder --out /tmp/ilovevi-preview-map.png"
./gradlew :tools:tilegen:run --args="--source /tmp/ilovevi-preview-map.png --out /tmp/ilovevi-preview-tiles"
```

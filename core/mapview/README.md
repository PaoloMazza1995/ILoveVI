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

## Behavior notes

- **Tiles**: `TileScheme.visibleTiles` (from `:core:model`) decides which tiles are needed;
  `TileUrl.build` turns each into a URL; Coil3's `AsyncImage` loads it. Every tile is a flat
  `bg800` box with the image layered on top — the same "neutral placeholder first, image on top"
  idiom `core/design`'s `NewsThumbnail` already uses — so a tile that's loading or that failed just
  shows the placeholder underneath, never anything broken, and one failed tile can't affect any
  other tile. Coil3's default memory+disk cache is keyed by the request's URL, which already encodes
  map version/zoom/tile coordinates, so no custom cache key was needed.
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
touchscreen; Coil3's disk cache behavior on-device; the `MapViewerWithOneThousandPinsPreview` /
placeholder-tile previews rendering correctly in Android Studio / Xcode preview; and (new with
cluster support) `MapViewerWithPinsAndClustersPreview` actually showing pins and badges together on
a real screen, that tapping a cluster badge visibly zooms in via `animateToBounds`, and that the
"selected pin always on top of a cluster" draw order looks right when they visually overlap.

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

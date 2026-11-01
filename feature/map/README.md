# :feature:map

The Map tab and its Filters secondary screen — MAP-01..11, MST-04..07 (Functional Analysis v0.4,
§6.4/7.1/7.2). Follows `:feature:catalog`'s module pattern and step 8's navigation contract.

## Dependencies

`:core:model`, `:core:data` (repositories), `:core:design` (every visual component), `:core:mapview`
(`MapViewer`, `MapViewerPin`/`MapViewerCluster`/`MapViewerItem`, `NormalizedBounds`,
`MapViewerState.animateTo`/`animateToBounds`), Compose, `androidx.lifecycle`, Koin, `coil3`.

## The "no user position" product rule

There is no location marker, no "center on player" control, and no location permission anywhere in
this module, `:core:mapview`, or any manifest in this repo (checked: `AndroidManifest.xml` has only
`INTERNET`/`ACCESS_NETWORK_STATE`; `iosApp/iosApp/Info.plist` has no `NSLocation*` key at all). The
camera only ever moves because of an explicit tap, a zoom-button press, a "Show on map"
selection, or a tapped cluster's zoom-to-fit.

## Clustering (MAP-10/MAP-11)

`cluster/PinClusterer.kt` is pure Kotlin — no Compose import, no `:core:mapview` *rendering* code,
only its plain data shapes (`MapViewerPin`/`MapViewerCluster`/`MapViewerItem`/`NormalizedBounds`).
It projects pins to screen space with the same `MapViewport.toScreen` conversion the viewer itself
uses, buckets them into a grid of `CLUSTER_DISTANCE_PX` (48px) cells, and merges a pin with any
other pin in its own or a neighboring cell that's actually within that distance, via a union-find
over pin indices — only the 3x3 cell neighborhood is checked per pin, which is what keeps this
roughly O (n) instead of O (n²). A finished group becomes one `MapViewerCluster` badge only when it
has strictly more than `CLUSTER_MIN_SIZE` (10) pins; smaller groups stay individual pins. Both
constants live in one file with KDoc, specifically so they're easy to tune.

- **Filters before clustering**: `MapViewModel` filters by category/sub-type before `MapScreen`
  ever calls `PinClusterer.cluster` — the clusterer only ever sees already-visible pins.
- **Selected pin excluded (MAP-11)**: `cluster(..., excludedPinId = state.selectedPinId)` removes it
  from every group; `MapScreen`'s own `items` computation then adds it back as its own
  `MapViewerItem.Pin` (the clusterer's KDoc is explicit that this is the *caller's* job, not
  something it does itself) — `PinLayer` (`:core:mapview`) separately guarantees the selected pin
  always paints on top of every cluster/pin regardless of list order.
- **No clustering at max zoom**: `zoom >= maxZoom` returns every pin as its own item.
- **Stable ids, not random**: a cluster's id is `"cluster:<cellX>:<cellY>:<size>"`, built from the
  screen-space grid cell of the group's centroid — the same input always produces the same id.
- **Dominant category**: whichever category has strictly more pins than every other category in the
  group; `null` (drawn in the neutral brand color) when two or more tie for the most.
- **Recomputation**: `MapScreen`'s `MapLoadedContent` recomputes `items` via `remember` keyed on the
  camera's `center`/`zoom`/`viewportSize` and the ViewModel's `pins`/`selectedPinId`/
  `showLabels` — never on every recomposition, only when one of those actually changes.

**Measured performance** (JVM, this sandboxed environment — see "What wasn't verified" below):
500 pins clustered in ~3.5-4ms (budget: <16ms); 2000 pins in ~24ms (budget: <50ms). See
`PinClustererTest.fiveHundredPinsClusterUnderSixteenMilliseconds`/
`twoThousandPinsClusterUnderFiftyMilliseconds` (the budgets are asserted, not just logged).

## Screens

- **`MapScreen`** (MAP-01..09): header (wordmark + search + filters), three category chips
  (`IloveViChip`, all on at first launch), `MapViewer` in tile mode with `IloveViMapControls`
  (zoom in/out only — no third button, per the product rule) top-right, a summary card
  (`IloveViCompactCard` + "View details") for the selected pin, an empty-hint overlay when the
  active filters leave nothing to show, and a Could-priority search field/results list. Tapping the
  map's background clears the selection (`onMapTap` -> `onBackgroundTapped`).
- **`MapFiltersScreen`** (MAP-05/MAP-09): one `IloveViChipGroup` per category, each with its own
  `WeaponType`/`VehicleType`/`ItemType` sub-type chips (icon + label from `:core:design`'s existing
  `EntryIcons.kt` mapping — nothing new added there); "Reset" turns every category and sub-type back
  on; "Show on map" is just `onBack`, since every chip tap already applies live.

Both follow `:feature:catalog`'s stateful/stateless split (`MapScreen`/`MapScreenContent`,
`MapFiltersScreen`/`MapFiltersScreenContent`) and step 8's `FeatureRegistration` contract — neither
imports `NavController`.

## Shared filter state across two routes

`MapFilterState` is a Koin **`single`**, not a `viewModel`: the Map tab and the Filters secondary
screen are two different routes/back-stack entries, so Navigation Compose's
`saveState`/`restoreState` tab-switch trick (what lets `CatalogViewModel` survive a *tab* switch)
doesn't keep a shared `ViewModelStore` alive *between* them. A plain Koin singleton has no such
lifecycle tie, so `MapViewModel` and `MapFiltersViewModel` simply read/write the same instance.

`MapRepository.pins`/`MapDao.pins` treat an empty `subTypes` set as "no sub-type filter" (match
everything) — the opposite of what turning every sub-type chip off is supposed to mean here
(MAP-09's "map shows an empty hint"). `MapViewModel` special-cases that: an empty `subTypes` set
short-circuits to an empty pin list instead of being forwarded to the repository.

## "Show on map" (MAP-06)

`selectedEntryId` (the Map tab's existing navigation-contract parameter, unchanged from step 8/9)
flows into `MapScreen`. A `LaunchedEffect(selectedEntryId, state.pins)` resolves it to a pin (by
matching `EntryRef`'s raw id), calls `viewModel.selectByEntryId(...)` and animates the camera there
via `MapViewerState.animateTo` — re-running only when `selectedEntryId` or the pin list actually
changes, so re-entering this tab with the same already-focused entry never re-animates.

## Offline tiles (MST-04)

`:core:mapview`'s `TileLayer` already gets Coil3 disk caching for free (its default `ImageLoader`
caches by URL, which already encodes map version/zoom/tile coordinates) for whatever tiles it
happens to load while panning/zooming. "The lowest zoom levels are downloaded at first launch so the
map is never blank offline" needed one more piece on top of that: `map/TilePrefetch.kt`'s
`PrefetchLowestZoomTiles`, called once from `MapLoadedContent`, `enqueue`s every tile at
`mapConfig.minZoom` (always either the single overview tile or a small handful, by `TileScheme`'s
own convention) through Coil3's `SingletonImageLoader` the moment the map config loads — so even a
tile the user never actually pans/zooms to is already cached before it's needed. Fire-and-forget,
same "a failed tile doesn't break the map" tolerance `TileLayer` itself already has.

## Tests

- `PinClustererTest` (13 tests): 11 close pins -> one badge with count 11; 10 close pins stay
  single; 120 pins report `count = 120` (the ">99" display decision belongs to
  `IloveViClusterBadge`, not this class); a badge splits into individual pins once zoom reaches
  `maxZoom`; filtering the input list changes the count; the selected pin is excluded from its
  group; no clustering at max zoom; stable ids for the same input; dominant-category and
  tied-category (-> `null`) cases; the two performance budgets.
- `MapViewModelTest` (11 tests): map-config-missing state, default-all-on filters, category toggle
  hiding a category's pins, every-sub-type-off leaving pins empty, filter reset, pin
  selection/clearing, `selectByEntryId` (including a miss), `showLabels` following the settings
  preference, a repository error surfacing as `MapUiState.Error`, the pre-collection `Loading`
  default.
- `MapFiltersViewModelTest` (5 tests): all-on at start, toggling one sub-type off,
  every-sub-type-off reporting `isEverythingOff`, reset, and that the shared `MapFilterState` is
  visible across two separately-constructed view model instances (proving the Koin-singleton sharing
  actually works).
- `MapPinToDetailUiTest` (1 test, `jvmTest`, `compose.desktop.uiTestJUnit4`): pin tap -> summary
  card -> "View details" fires with the pin's `(category, entryId)`. The tap goes through the pin's
  real `onClick` semantics action (the same one a screen reader activates) rather than a raw
  synthetic mouse click — see that test's KDoc for why: a real click has to survive `MapViewer`'s
  own custom low-level gesture detector, which `:core:mapview`'s own README already flags as
  unverified in this device-less sandbox, so going through the semantics action tests the same
  production callback (`PinMarker`'s `onClick { onPinTap(pin.id) }`) without that dependency.

## Known gaps / follow-ups

- **MST-04's prefetch is implemented but its real effect is unverified.** `PrefetchLowestZoomTiles`
  issues the `enqueue` calls, but whether they actually land in a real, persistent on-device Coil3
  disk cache — and whether that cache survives an app restart with the network truly off — could not
  be checked here (no device/emulator; see below). The code path is new and exercised only by the
  fact that the whole module still compiles and the rest of the Map screen's tests pass with it
  wired in, not by a dedicated test of its own (an `ImageLoader` is not something the fakes in this
  module's test suite construct or substitute).
- **No real device/emulator in this sandboxed environment** (same caveat every prior map-related
  step in this repo carries): pan/pinch/tap gesture behavior, actual rendered frame times with
  several hundred real pins, and Coil3's on-device disk cache were not, and could not be, verified
  here. The clustering algorithm's own performance was measured directly (see above); the
  surrounding UI's frame behavior was not.
- Search (MAP's Could-priority item) is implemented at the ViewModel/UI level (name/zone substring
  match over whatever's currently visible under the active filters) but has no dedicated test beyond
  what `MapViewModelTest` exercises indirectly; not called out as its own acceptance item, so not
  blocking.
- The search/filter icons (`IconSearch`/`IconFilter`, added to `:core:design`'s `IloveViIcons.kt`)
  have no exported design-canvas asset to match against — flagged inline in their own KDoc.

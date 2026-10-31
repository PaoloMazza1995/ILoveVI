# :feature:catalog

Catalog (list) and Detail — CAT-01..05, DET-01..04 (Functional Analysis v0.4, §6.3/7.3/7.4). The
first feature module in this repo; no precedent `feature:*` module existed to copy, so this one
follows the established `core:*` library-module pattern (`kmm-library-plugin` + Compose) and step
8's navigation contract instead.

## Dependencies

`:core:model`, `:core:data` (repositories — this module never reads `:core:database`/`:core:network`
directly), `:core:design` (every visual component), `:core:mapview` (the Detail mini-map), Compose
(ui/foundation/material3/resources), `androidx.lifecycle` (ViewModel + `viewModelScope`), Koin
(`koin-core`/`koin-compose`/`koin-compose-viewmodel`), `coil3` (images).

## Screens

Each screen is split into a stateful entry point (`CatalogScreen`/`DetailScreen`, wiring a
`koinViewModel()`) and a stateless `*ScreenContent` composable taking a plain UI state + callbacks —
the stateless half is what both the UI test and any future preview drive directly, without Koin or a
real repository.

- **`CatalogScreen`** (CAT-01..05): title, three category chips (`IloveViChip`, exactly one active),
  a debounced `IloveViSearchField`, and an `IloveViListRow` per entry (thumbnail — category-colored
  sub-type icon, or the entry's real image via Coil3 once it has one — name,
  "type - zone", chevron). Loading/empty states are plain `CatalogUiState` derivations, not separate
  screens.
- **`DetailScreen`** (DET-01..04): back control, `IloveViCategoryBadge`, name (`displayLarge` —
  tokens.json's "display-xl"), type, a hero image area, "Where to find it" (mini-map + zone +
  description, or "Location not available yet" with no "Show on map" button when the entry has no
  pin), and `AttributesTable` — one composable, driven by the concrete `Entry` subtype, rendering
  Weapon's fixed type/price/ammo or Vehicle/Item's free-form `attributes` map (label-humanized:
  `top_speed` → `Top Speed`).

Neither screen imports Navigation Compose or knows a `NavController` exists: `onOpenDetail`/
`onBack`/`onShowOnMap` are plain callbacks, matching step 8's `FeatureRegistration` contract exactly
(the app module wires the actual navigation; this module just reports what happened).

## ViewModels

`CatalogViewModel`/`DetailViewModel` (`androidx.lifecycle.ViewModel`), each exposing one
`StateFlow<...UiState>` — "a single UI state" per the roadmap step. Both are offline-first: every
read comes from `:core:data`'s `CatalogRepository`/`MapRepository`, which are themselves backed by
`:core:database`; nothing here ever touches `:core:network`.

- **Category/search surviving tab switches** costs nothing extra: `AppNavHost` (step 8) switches
  tabs via `popUpTo(startDestination){saveState=true}` + `restoreState=true`, Navigation Compose's
  documented recipe for keeping a destination's `NavBackStackEntry` — and so its `ViewModelStore`
  — alive while a different tab is in front. The same `CatalogViewModel` instance, with its
  already-set category/query `MutableStateFlow`s, is simply still there.
- **Debounced search without delaying category switches**: `query.debounce(300).onStart {
  emit(query.value) }` — the `onStart` bypasses the debounce for the *current* value every time the
  category changes, so switching categories shows that category's (already-known, unfiltered or
  still-filtered-by-the-current-query) list immediately, never waiting out 300ms first.
- **`DetailViewModel`** takes `category`/`entryId` as constructor parameters (via Koin's
  `parametersOf`, keyed `"$category/$entryId"` so two different entries get two different view model
  instances even if both happen to be alive at once) rather than mutable state — a different entry
  is a different screen, never a state change on the same one. A `category` that doesn't parse as a
  real `EntryCategory` (a malformed/stale deep link — this app itself never generates one, since it
  always writes `EntryCategory.name`) resolves to `DetailUiState.NotFound`, the same as the entry
  simply not existing, rather than crashing.

## Mini-map (DET-02)

`EntryMiniMap` is a small, **non-interactive** `:core:mapview` `MapViewer` centered on the entry's
pin, that pin selected. `MapViewer` itself has no "disable gestures" flag (every other caller wants
pan/pinch/tap) — rather than touching that shared module for the one caller that needs the opposite,
an invisible full-size `Modifier.pointerInput` on top consumes every touch before
`MapViewer`'s own gesture detector ever sees it. Centering uses `rememberMapViewerState`'s
`initialCenter`/`initialZoom` directly (never animated, since this view never moves again) at a
fixed zoom two steps in from `MapConfig.minZoom` — an unconfirmed judgment call, flagged here since
nothing in the Functional Analysis specifies one. `MapViewer`'s own "Map. Drag to pan..." content
description is actively wrong here (nothing can be dragged); `Modifier.clearAndSetSemantics`
replaces it with one that matches what this view actually is.

## Two small, justified fixes to `:core:data` found while building this module

`MapRepository`/`MusicRepository`/`SettingsRepository` used to return `:core:database`'s own view
types (`MapPinWithEntryName`/`Compilation`/`CompilationSongEntry`/`Preferences`) directly — a
judgment call flagged in `:core:data`'s own README at the time ("already a clean abstraction, not
raw persistence detail"). That turned out to be wrong: `:core:database` is only an
`implementation` dependency of `:core:data`, so none of its types are visible to a module that only
depends on `:core:data` — exactly the leak this module's own `CatalogRepository`-style boundary
exists to prevent. Hit immediately trying to implement `MapRepository` in a test fake here
(`Unresolved reference 'database'`). Fixed at the source: `:core:data` now has its own
`MapPinWithEntryName`/`Compilation`/`CompilationSongEntry`/`Preferences` (new file,
`core/data/.../repository/RepositoryTypes.kt`), with the `*RepositoryImpl` classes mapping from
`:core:database`'s rows into them. `:core:data`'s own tests needed no changes beyond one now-
redundant import; its 14/14 test suite is re-verified green.

## Resources (strings)

Every piece of Catalog/Detail-specific text — title, chip labels, search placeholder, empty-state
copy, back/location/attribute labels, "Show on map" — is its own `composeResources` string (EN +
IT), independent of `:core:design`'s own (pre-existing, not localized) `IloveViCategory.label`
used only by the reused `IloveViCategoryBadge` component. `:core:design` gained two small, exact-
precedent additions for this module: `EntryCategory.toIloveViCategory()` (the category → color
mapping, next to the existing `EntryCategory.icon()`) and `WeaponType`/`VehicleType`/`ItemType`/
`Entry.labelRes()` (the already-existing, previously-unused `design_subtype_*` strings from step I,
now actually consumed — their own KDoc already said they were "used as an icon's content description
when the icon is the only identifier," i.e. exactly here).

## Tests

- **`CatalogViewModelTest`**: category switch shows that category's own entries; search is case- and
  accent-insensitive (via a small, local fold in the fake repository — not a re-test of
  `:core:database`'s own, already-tested `foldForSearch()`); no matches is empty but not loading;
  before any collection the state is loading.
- **`DetailViewModelTest`**: an entry *without* a pin has a `null` pin in the loaded state; an entry
  *with* one has it; an unknown entry id and a malformed category both resolve to
  `NotFound`.
- **`CatalogToDetailUiTest`** (`jvmTest`, `compose.desktop.uiTestJUnit4` — the same dependency
  `:tools:map-editor`'s own, this repo's only other Compose UI test, uses): the acceptance's
  explicit scenario, "row tap -> Detail -> 'Show on map' event," driving the real
  `CatalogScreenContent`/`DetailScreenContent`. Interactive nodes are found by `testTag`
  (`catalog-row-<entryId>`/`show-on-map-button`, one small addition each to the two production call
  sites), not by their resource-backed visible text — `stringResource()` was observed to still
  resolve to an empty string after `waitForIdle()` in this plain `createComposeRule()` JVM test
  environment (no Android instrumentation), for both this module's and `:core:design`'s string
  resources alike.

Both ViewModel test classes install `kotlinx.coroutines.test`'s `StandardTestDispatcher` as
`Dispatchers.Main` in `@BeforeTest`/reset in `@AfterTest`, and pass that *same* dispatcher instance
to `runTest(dispatcher) { }` — `viewModelScope` resolves `Dispatchers.Main.immediate`, which on a
plain JVM unit test has no real implementation; without this, `advanceUntilIdle()` only drives the
test's own scheduler, never the (separate, uncontrolled) one the view model's `combine`/
`flatMapLatest`/`debounce` pipeline actually runs on, and every assertion sees nothing but the
initial state forever.

## Wiring into the real app (step 8's contract)

`AppFeatureRegistrations.kt` (in `shared`) now calls this module's real `CatalogScreen`/
`DetailScreen` instead of the stub `CatalogStubScreen`/stub `DetailScreen` step 8 left in place —
both stubs, and their now-unused string resources/imports, were deleted rather than kept around.
`AppKoinModules.kt`'s `allAppKoinModules` wires in `databaseModule`/`networkModule`/`dataModule`/
`catalogFeatureModule` for the first time — each of those three modules' own README already said
"not wired in until a consuming step needs it"; this is that step. The one piece this needed that
didn't exist yet, a real Android `Context` for `databaseModule`'s `PlatformContext` single, is
`shared`'s new `di/PlatformContextModule.kt` (`expect`/`actual` in *true* `commonMain`/
`androidMain`/`iosMain` — not the `src/appMain` alias, since `expect`/`actual` need genuinely
different compilations, not the same file compiled twice) — `@Composable` because the Android
`actual` needs `LocalContext.current`, only readable inside composition, and there is no
`Application` subclass or changed `MainActivity` anywhere in this app to get one from otherwise.

Verified: `androidApp:assembleDebug`, `shared:compileKotlinIosSimulatorArm64`,
`shared:compileKotlinJs`, `shared:compileKotlinWasmJs`, `webApp:build` and
`shared:testAndroidHostTest` all still pass with the real DI graph wired end to end — not just this
module's own isolated build.

## What wasn't verified here

Same sandboxed-environment limitation as every other step in this roadmap: no real Android/iOS
device or emulator, so nothing here was literally watched rendering, scrolling or being tapped on a
screen. `androidApp:assembleDebug` succeeding with the full real DI graph (databaseModule's
`PlatformContext`, a real on-device `AndroidSqliteDriver`, this module's view models all resolving
through Koin) is the strongest check available in this environment, but it is not the same as a
device run. The acceptance's "Catalog and Detail work offline with the seed data for all three
categories" is architecturally true (every read is local-database-only, by construction, and
`:core:data`'s own seed importer populates one row per category) and exercised by this module's own
ViewModel tests against fakes, but was not watched happening against the real, seeded, on-device
database end to end.

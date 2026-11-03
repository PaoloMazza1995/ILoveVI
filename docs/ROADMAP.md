# IloveVI Roadmap

## How to use this file

Status legend:

- `[ ]` todo
- `[~]` in progress
- `[x]` done
- `[!]` blocked (reason given inline)

Every step's prompt **starts** by setting its own line to `[~]`, and **ends** by marking it `[x]`
(or `[!] <reason>`) together with the completion date (`YYYY-MM-DD`). Leave the acceptance-summary
line and any "Notes / follow-ups" bullets in place and update them instead of deleting history —
this file is the one durable record of what shipped, what's missing, and what was decided along the
way. Never flip the status of a step you didn't just verify in the code (read the deliverable, run
its tests) — re-check it yourself rather than trusting an older note or what you were told.

## Recommended execution order

`P, H, I, 5A, 5A-bis, 8, 5B, 6, 7A, 7B, 9, 10, 11, 12A, 12B, 12C, 13A, 13B, 14, 15, 16`

(4A and 4B are prerequisites already done — see "Done so far" — so they're not repeated in this
sequence.) H and I are pulled forward, ahead of 5B onward, because 5A/5A-bis/8 already exist and
build on `core:model`'s `ItemType`/`core:design`'s icon set — better to land the HEALTH removal and
the per-sub-type icon set once, early, than to rework pin rendering and seed data twice.

---

## Done so far

- [~] **0 - Project foundation** (build-logic, app skeleton, CI, docs) - needs: - - done:
    - Gradle KMP project with a convention-plugin `build_logic` (`KmmAppPlugin`, `KmmComposePlugin`,
      `KmmKoinPlugin`, `KmmLibraryPlugin`), `androidApp`/`iosApp`/`webApp`/`shared` targets, version
      catalog. Builds successfully (`./gradlew build` across all targets). The wizard template is
      fully gone (`shared`'s `App()` is the real 5-tab shell as of step 8, not "Click me!").
    - Notes / follow-ups: **CI is still missing** — no `.github/workflows` or any other CI config
      anywhere in the repo, so this step's deliverable isn't fully there. README also links to
      `CONTRIBUTING.md` and references `assets/logo.png`, neither of which exist. No `CLAUDE.md`
      either. **2026-10-18**: found and fixed a real bug in `build_logic/.../KmmLibraryPlugin.kt`
      (used by every `:core:*` library module, so squarely a foundation-level fix): it configured
      the
      `com.android.kotlin.multiplatform.library` Android target without `androidResources { enable =
    true }`, which silently disabled the Gradle task that copies Compose Multiplatform resources
      (fonts, drawables) into the Android app's assets — confirmed by unzipping the built debug APK
      before/after the fix (the Anton/Barlow `.ttf` files and the bundled map PNGs were completely
      absent from `assets/composeResources/...` before, present after). See the note under 5A-bis
      for the user-visible symptoms this caused. Everything else (build-logic, app skeleton,
      docs/README)
      is in place — CI remains the main thing blocking `[x]`.

- [x] **1 - core:design (named `core:designsystem` in the spec)** (core/design) - needs: - - done:
  2026-10-14
    - Night theme (`Theme.kt`), color/spacing/shape/type tokens, Anton + Barlow fonts bundled as
      `composeResources`, and 12 components (Badge, Button, Card, Chip, IconButton, ListRow,
      MapControls, MapPin, NavBar, NewsRow, SearchField, Switch) plus a component gallery screen.
      Module compiles on every target (JVM/Android/iOS); verified via `./gradlew :core:design:...`
      as part of the module build.
    - Notes / follow-ups: no automated tests (expected — pure Compose UI, no logic to unit test).
      The design spec (`documentation/ilovevi-design-system 2/components/Cover`) also calls for a
      `Cover`
      component that isn't implemented yet — minor gap, not blocking. **2026-10-18**: the Android
      resource-packaging bug described under step 0 also affected this module specifically — the
      Anton/Barlow fonts never reached the Android APK's assets, so Android showed the system font
      while iOS was fine. Fixed at the `build_logic` level (not in this module's own files); worth a
      real-device re-check once a device/emulator is available (see step 0/5A-bis). The icon set is
      still category-level only (`IconWeaponCrosshair`/`IconVehicle`/`IconStar`), not per sub-type —
      that's step I, not started.

- [x] **2 - core:model** (core/model) - needs: - - done: 2026-10-15
    - Pure Kotlin model: `Entry` (`Weapon`/`Vehicle`/`Item`), `MapPin`/`MapConfig`, `Station`/
      `Song`,
      `News`, value-class ids, `NormalizedPoint`. `./gradlew :core:model:jvmTest` — **all tests
      pass**
      (`EntryTest`, `JsonRoundTripTest`, `MapConfigTest`, `NormalizedPointTest`,
      `ValueClassSerializationTest`, plus the map-math tests counted under step 4A). Re-verified
      2026-10-18: still green, and also passes on `iosSimulatorArm64Test` (not just JVM).

- [~] **3 - Backend schema (Supabase)** (backend/supabase) - needs: 2 - done:
    - 6 versioned migrations (extensions/helpers, weapons/vehicles/items, stations/songs, map,
      feed_sources, storage bucket `map-tiles`), RLS (anon/auth read, admin-only write via
      `app_metadata.role`), soft-delete + `updated_at` delta-sync convention, `seed.sql`, and 3
      pgTAP test files (`001_select_and_write_rls`, `002_updated_at_and_soft_delete`,
      `003_map_pin_constraints`) all present and read as correct.
    - Notes / follow-ups: **tests still could not be executed in this environment** (re-checked
      2026-10-18: `supabase` CLI 2.120.0 is installed, but `docker` is not on PATH and `supabase test
    db`/`supabase db reset` both require it). Per this file's own rule (done = deliverable exists
      *and* tests pass, verified by running them), this stays `[~]` until someone runs `supabase db
    reset && supabase test db` on a machine with Docker and confirms green. **2026-10-18**: step H
      added a 7th migration (`20261018120001_items_subtype_remove_health.sql`, narrowing
      `weapons_vehicles_items`'s `sub_type` CHECK on `items` to `('AMMO', 'ARMOR')` without editing
      the already-applied migration that first defined it) — reviewed carefully by eye but, same as
      the rest of this step, not actually executed here.

- [x] **4A - core:model map and tile math** (core/model/.../map) - needs: 2 - done: 2026-10-16
    - `TileScheme`, `TilePosition`, `TileRange`, `TileUrl`, `MapViewport` (continuous point↔screen
      conversion, zoom clamping). `./gradlew :core:model:jvmTest` — **all tests pass**
      (`MapViewportTest`, `TilePositionTest`, `TileRangeTest`, `TileSchemeTest`, `TileUrlTest`).

- [x] **4B - tools:tilegen** (tools/tilegen) - needs: 3, 4A - done: 2026-10-17
    - JVM CLI + library: streams a source image into a tile pyramid (memory-safe, strip-based),
      uploads to Supabase Storage, registers a `map_config` row; `generate-placeholder` subcommand
      produces a clearly-watermarked dev-only map image (no real/fan GTA VI map ever touches this
      tool). `./gradlew :tools:tilegen:test` — **all tests pass** (`SourceImageFetcherTest`,
      `SupabaseAdminClientTest`, `TileGenCliArgsTest`, `TilePyramidGeneratorTest`,
      `PlaceholderMapImageTest`). Re-verified 2026-10-18.

---

## Next

- [!] **H - Remove the HEALTH item type** (core/model) - needs: 2 - done: 2026-10-18
    - `HEALTH` removed from `core:model`'s `ItemType` (now `AMMO, ARMOR`; KDoc still says these are
      placeholders, per the prompt). `EntryTest`/`JsonRoundTripTest` updated (HEALTH-themed
      fixtures → AMMO/ARMOR-themed ones). `./gradlew :core:model:jvmTest` and
      `:core:model:iosSimulatorArm64Test` pass; a full `./gradlew build` (every module/target,
      including `:webApp`) also passed. Backend: new migration
      `20261018120001_items_subtype_remove_health.sql` narrows `items.sub_type`'s CHECK to
      `('AMMO', 'ARMOR')` without editing the already-applied migration that first defined it — it
      looks up the constraint's real (auto-generated) name via `pg_constraint` rather than assuming
      it, logs any pre-existing `sub_type = 'HEALTH'` row via `RAISE NOTICE`, then hard-deletes it
      (soft-delete can't satisfy a CHECK constraint) along with any `map_pins` row pointing at it
      (no real FK — would otherwise orphan silently). `seed.sql`'s `item-1`/HEALTH row removed; its
      pin re-pointed at the remaining `item-2`/ARMOR row. No SQL test file referenced `HEALTH` or
      `items` at all, so none needed edits. Repo-wide `grep -rniE "health|salute|medkit|first aid"`
      (excluding `.git`/build dirs) now returns only: `backend/supabase/config.toml`'s
      `health_timeout` (Supabase's own local-stack setting, unrelated), the already-applied
      migration that originally introduced `HEALTH` (left untouched, as instructed), and the new
      migration above. `docs/ROADMAP.md`'s "Open decisions" updated with the requested line.
    - Not `[x]` because: **`supabase db reset`/`supabase test db` still could not actually be run**
      (re-checked: `docker` still not on PATH) — the one unmet acceptance item ("the SQL tests pass
      on a clean `supabase db reset`"), same environment limitation step 3 already has. The
      migration was reviewed carefully by eye but is unverified by execution.
    - Notes / follow-ups: re-run `supabase db reset && supabase test db` on a machine with Docker
      before trusting the migration blindly.

- [x] **I - Icon set per sub-type and ClusterBadge** (core/design) - needs: 1, 2 - done: 2026-10-18
    - **13-icon set**: `IloveViIcons.kt` gained `enum class IloveViIcon` (13 values — PISTOL, RIFLE,
      MACHINE_GUN, MELEE, EXPLOSIVE, CAR, MOTORCYCLE, BOAT, HELICOPTER, PLANE, AMMO, ARMOR, BOX),
      each an `ImageVector` built from the given SVG path data (24x24 viewport, 2px stroke, round
      caps/joins, never filled) — a different underlying API than this file's older Canvas-drawn
      icons (kept untouched, next to the new ones), used here because the mapping step needs a
      *value* to return from a `when`, not a composable function. A composable
      `IloveViIcon(icon, modifier, size, tint, contentDescription)` wraps M3's `Icon` for rendering.
    - **Mapping** (new file `EntryIcons.kt`): `EntryCategory.icon()`, `WeaponType.icon()`,
      `VehicleType.icon()`, `ItemType.icon()`, `Entry.icon()`, every one an exhaustive `when` with
      no
      `else`. `core:design` didn't depend on `core:model` before this step — added
      `implementation(project(":core:model"))` to `core/design/build.gradle.kts` (matches this
      project's existing convention of `implementation`, never `api`, even where a public API leaks
      the dependency's types — see e.g. `core:mapview`'s own `MapViewer(config: MapConfig, ...)`).
    - **Wired in**: `MapPin.kt`'s `IloveViMapPin` gained an `icon: IloveViIcon` parameter (defaults
      to the category's generic icon — Pistol/Car/Box — for callers that only know the category;
      `core:mapview`'s `PinLayer.kt` wasn't touched, since its `MapViewerPin` has no sub-type field
      yet to supply a more specific one — see follow-ups). `ListRow`'s and `Card`'s
      (`IloveViCompactCard` — the map summary card) previews now use the new icons. `Chip.kt` gained
      an optional leading `icon` parameter (18dp, fits the fixed 44dp height) for category and
      Filters sub-type chips. Old `IconWeaponCrosshair`/`IconVehicle`/`IconStar` deleted from
      `IloveViIcons.kt` — confirmed zero remaining references anywhere in the repo.
      "Detail image placeholder"/"empty states" and the real Map Filters screen don't exist yet (not
      started — steps 11/13B/catalog-detail) so there was nothing to wire there.
    - **Accessibility**: 12 sub-type name strings added in English + Italian
      (`core/design/.../composeResources/values(-it)/strings.xml` — this module's *first* string
      resources). Icons next to a label (Chips) stay `contentDescription = null`. `IloveViMapPin`'s
      icon also stays `null`: `PinLayer.kt` already attaches one merged semantics description to the
      whole pin, so a second one on the icon would double-announce. The gallery's new "Icons"
      section demonstrates the one concrete "icon alone" case with a real description.
    - **ClusterBadge** (new component + preview + gallery section + `documentation/.../components/
    ClusterBadge/README.md`, flagged there as code-first, no designer `preview.html` source):
      40dp circle / 44dp tap target, category-color fill (`brand-purple` for mixed — the one brand
      accent not already a category color; flagged as an unconfirmed judgment call, since the
      system's own README calls `brand-purple` "decorative elements only"), 2dp `bg-900` border,
      Anton count text (`ink-on-brand`, already verified elsewhere at >4.5:1 contrast), "99+" above
      99, semantics content description from a new localized string
      (`design_cluster_badge_content_description`, EN/IT). Previews cover every requested
      count/category combination (11/42/99/120 × Weapons/Vehicles/Items/mixed) plus a 2x font-scale
      ("large-text") variant. **No light-theme variant**: this design system's README states Night
      is its *only* theme (`IloveViTheme` hardcodes a single `darkColorScheme`) — there is no light
      theme anywhere in this app to preview against, so that specific request doesn't apply here;
      documented in the new README rather than silently skipped.
    - **Tests** (new `core/design/src/commonTest`, this module's first — added
      `commonTest.dependencies`
      to its `build.gradle.kts`): `EntryIconsTest` (5 tests — every enum value maps to the right
      icon) and `IloveViIconTest` (3 tests — all 13 icons have a 24x24 viewport, all build a
      non-empty vector, exactly 13 exist and none is health-related).
      `./gradlew :core:design:jvmTest`
      and `:core:design:iosSimulatorArm64Test` — **8/8 pass** on both. No screenshot-testing tooling
      exists in this repo (no Paparazzi/Roborazzi configured anywhere), so bullet 6's conditional
      "screenshot test... if the module already has screenshot tests" doesn't apply.
    - **Docs**: design system README's "Iconography" section expanded with the full icon table and
      "how to replace one"; "Components" list now says twelve, includes ClusterBadge; new v1.2
      changelog entry. `MapPin/README.md` and `Chip/README.md` updated to describe sub-type icons
      instead of the old crosshair/star/vehicle glyphs. `tokens.json` left untouched — it doesn't
      list icons as a structured section (only incidental "usage" text mentions), so bullet 5's
      conditional "update tokens.json if it lists icons" doesn't apply.
    - **Verified**: `./gradlew :core:design:compileKotlinJvm :core:design:compileAndroidMain
    :core:design:compileKotlinIosSimulatorArm64` and `:core:mapview:jvmTest
    :shared:testAndroidHostTest :androidApp:assembleDebug` (everything depending on `core:design`)
      all pass; unzipped the built debug APK and confirmed the new `values`/`values-it` string
      resources are actually packaged into Android assets (same `build_logic` fix from step 0/5A-bis
      applies here too). Repo-wide grep confirms zero remaining references to the deleted icons.
    - Notes / follow-ups: (1) `core:mapview`'s `MapViewerPin`/`PinLayer.kt` weren't touched — they
      have no per-pin sub-type field, so real pins still fall back to `IloveViMapPin`'s
      category-level default icon; wiring a real sub-type through is for whichever step actually
      gives `MapViewerPin` that data (`feature:map`, step 11). (2) the "cluster threshold of 10
      pins" and "mixed-cluster color = brand-purple" are both still open/unconfirmed — see "Open
      decisions". (3) no real on-device/screen-reader pass of the new content descriptions (same
      "no device/emulator in this environment" limitation as every UI step so far).

- [x] **5A - core:mapview** (core/mapview) - needs: 2, 4A, 1 - done: 2026-10-18
    - Compose Multiplatform pan/pinch/scroll/double-tap-zoom map viewer with a tile layer (Coil3)
      and a pin layer, deliberately dependency-free of database/network/feature modules so it can be
      shared by `tools:map-editor` and `feature:map` later. `./gradlew :core:mapview:jvmTest` —
      **all tests pass** (`DevMapImageTest`, `MapGeometryTest`, `MapViewerPerfTest`,
      `MapViewerStateTest`,
      `SingleImageGeometryTest`; also green on `iosSimulatorArm64Test`).
    - Notes / follow-ups: **2026-10-18, a real correctness bug was found and fixed**: the three
      gesture `pointerInput` blocks were chained in an order (`panZoomFlingGestures` first,
      `tapGestures` second) where — confirmed by reading `compose-ui`'s own `HitPathTracker`/
      `TapGestureDetector` sources — `detectTapGestures` always consumes the pointer-down event
      before
      `panZoomFlingGestures` ever got to read it (chained `pointerInput` modifiers dispatch
      `PointerEventPass.Main` innermost-first; `tapGestures` was effectively dispatched before
      `panZoomFlingGestures`). Net effect: **dragging the map never panned it at all**. Fixed by
      reordering the chain (`tapGestures` → `scrollWheelZoomGestures` → `panZoomFlingGestures`, pan
      last so it reads every event first). Also replaced the old bare `0f..1f` center clamp (which
      allowed up to 50% of the image to scroll off-screen, showing empty space) with
      `centerBoundsForAxis`, a proper edge-aware bound applied uniformly to pan, pinch-zoom,
      double-tap-zoom, scroll-wheel zoom and fling, so the map can never be dragged/flung past its
      own edge. Module's own README is still explicit that real-device verification (actual rendered
      frame times with 1000 pins, gesture-block interplay on a real touchscreen, Coil3 disk cache
      behavior) has **not** been done — this is now a higher-priority gap, since a real gesture bug
      was just caught by static analysis rather than by testing. Budget real device time before
      shipping.
    - **Cluster support added to the viewer (2026-10-18)**: `MapViewerCluster(id, position, count,
    dominantCategory, bounds: NormalizedBounds)` + a sealed `MapViewerItem { Pin, Cluster }`;
      `MapViewer` now takes `items: List<MapViewerItem>`, with a non-breaking `pins:
    List<MapViewerPin>` overload kept for every existing caller/preview. Clusters draw via
      `:core:designsystem`'s `IloveViClusterBadge` in the same `PinLayer` canvas as pins (selected
      pin always topmost); a new `hitTestCluster` mirrors `hitTestPin`'s 44dp-floor hit-testing; a
      new `MapViewerState.animateToBounds(scheme, bounds, paddingPx)` picks the zoom that fits a
      cluster's bounds, clamped to `minZoom`/`maxZoom`. No clustering algorithm added — that's still
      entirely `feature:map`'s job (step 11), unstarted. 47 module tests pass (up from 37; 10 new:
      4 cluster hit-testing/screen-position, 3 `animateToBounds` normal/upper-clamp/lower-clamp
      cases, 3 `NormalizedBounds`/`MapViewerItem` validation), on both `jvmTest` and
      `iosSimulatorArm64Test`; `androidApp:
    assembleDebug` and `shared:testAndroidHostTest` (everything downstream of this module)
      unaffected. New preview `MapViewerWithPinsAndClustersPreview` (11/42/120-pin clusters, mixed
      category included, tapping one calls `animateToBounds`) — not watched on a real device, same
      limitation as the rest of this module.

- [!] **5A-bis - core:mapview single image mode (bundled PNG)** (core/mapview) - needs: 5A - done:
  2026-10-18
    - The original "no Map tab to wire into yet" blocker is resolved — step 8's `TabScreens.kt`
      already wires `MapViewerSampleScreen` into the Map tab. The remaining blocker is strictly: no
      real Android/iOS device or emulator exists in this environment, so nothing here has been
      physically watched panning/zooming/loading on-screen (the
      `android.kotlin.multiplatform.library`
      fix below was verified by inspecting the built APK's contents, which is strong but not the
      same as a device screen).
    - Everything inside `:core:mapview` itself: `MapContent` sealed interface (`SingleImage`/
      `Tiles`), the old `MapViewer(state, config, pins, ...)` signature kept as a thin non-breaking
      overload, single-image mode reusing tile-mode's gesture/transform code via a synthetic
      `TileScheme`,
      `rememberDevMapImage()` with a 3-step fallback chain, downscale-to-4096 with per-platform
      memory-safe decoding, and a compile-time (`-Pilovevi.release=true`) release gate — all present
      and unit-tested.
    - **2026-10-18, found and fixed (in `build_logic`, not in this module)**: Android showed the
      wrong font and never loaded the bundled map image, while iOS worked fine — root cause was
      `build_logic`'s `KmmLibraryPlugin` (used by every `:core:*` library module) configuring its
      Android target without `androidResources { enable = true }`. That silently disabled the Gradle
      task that copies Compose Multiplatform resources into Android's `assets/`, so `map_dev.png`,
      `map_dev_fallback.png` and the bundled fonts never reached the Android APK at all — confirmed
      by unzipping the built debug APK before/after: those files were simply absent before the fix,
      present after. This module's own Kotlin code was correct the whole time; only Android's
      resource packaging, at the build-logic level, was silently broken. See step 0's entry for the
      fix.
    - **2026-10-18, product decision**: `singleImageMinZoom` switched from "contain" fit (whole
      image visible, letterboxed top/bottom or left/right when the image's aspect ratio doesn't
      match the screen's) to "cover" fit (always fills the viewport edge-to-edge, cropping the
      overflow instead — confirmed with Paolo after he reported the bundled map rendering with dark
      bars above/below it on his device). The new pan/zoom/fling bounds (see 5A's note) are what
      make the cropped part of the map reachable by dragging, so nothing is ever permanently hidden.
    - Notes / follow-ups: (1) do a literal on-device/emulator pass once available — pan/zoom/fling,
      the font fix, and the map-image fix should all be re-confirmed by eye, not just by reading APK
      contents and unit tests. (2) the release gate (`-Pilovevi.release=true`) is still not
      automatically passed by any build — step 15 needs to wire it into real release packaging. (3)
      **open question for Paolo, unchanged**: `map_dev.png`/`map_dev_fallback.png` are both
      generated grid-with-watermark placeholders, not real map art — if a real dev map image is
      planned later, confirm its rights before it's committed (see the module README).

- [!] **5B - tools:map-editor** (tools/map-editor) - needs: 5A, 4B - done: 2026-10-19
    - New plain-JVM Compose Desktop module (`org.jetbrains.kotlin.jvm` + `org.jetbrains.compose` +
      `application`, same shape as `:tools:tilegen` minus the Compose bits — no `build_logic`
      convention plugin, since this needs no Android/iOS target). Depends on `core:model`,
      `core:mapview` (incl. single-image/offline mode, step 5A-bis), `core:design`,
      `tools:tilegen` (in-process, for "New map version") and, newly in this repo, **supabase-kt**
      (`auth-kt` + `postgrest-kt`) — a deliberate exception to `:tools:tilegen`'s "raw Ktor, no SDK"
      house style, justified in this module's own README: real session-based auth + multi-table
      authenticated reads/upserts is enough surface that the SDK earns its keep, unlike tilegen's 3
      plain service-role calls. Every supabase-kt call (`createSupabaseClient`, `Auth.signInWith`,
      `Postgrest.from(...).select/upsert/update`, `Columns`, `FilterOperator`) was verified against
      the real 3.1.4 jars via `javap` (no sources jar was available), not from memory, precisely
      because this environment can't run the result against a live project to catch a wrong method
      signature. Also added `kotlin-csv-jvm` (CSV) — no existing CSV library in the catalog.
    - **PLT-01..09 all implemented**: real GoTrue email/password sign-in with an `app_metadata.role`
      check for a specific "not an admin" message (`auth/AdminAuthRepository.kt`); loads the current
      `map_config` and shows it via `MapViewer` (`data/MapConfigRepository.kt`);
      searchable/filterable entry list that arms placement (`EntryListPanel`); map click
      creates/moves the armed entry's pin, drag and arrow-key nudge (Shift = large step) move the
      selected one, Delete soft-deletes (`geometry/EditorGeometry.kt`, `state/EditorState.kt`);
      all-pins list flagging pins on an older
      `map_version` as "to reposition"; CSV/JSON import (validated — unknown entry, out-of-range
      x/y, duplicates — report shown, confirmation required before any write) and export
      (`importer/`); save upserts by the `(category, entry_id)` unique key (not `map_pins.id`, which
      import rows never carry) and soft-deletes removals, gated on an admin session, with an
      unsaved-changes exit prompt; "New map version" runs `:tools:tilegen`'s pipeline in-process and
      registers it via the **service-role** key (same credential tilegen's own CLI uses — not this
      tool's admin session, since `map_config` writes have always been a service-role operation
      here); offline "Open image" mode (no tiles/network) stores pins in a local JSON file and
      refuses to publish them until a real admin sign-in succeeds, with a permanent "Offline: not
      connected to the backend" banner. One small, justified touch to `:core:mapview` itself:
      `singleImageTileScheme` made public (was `internal`) so this module can compute the same
      `TileScheme` `MapViewer` does internally, for its own drag/nudge pixel math in offline mode —
      `core:mapview`'s own tests (47) and downstream builds (`androidApp:assembleDebug`,
      `shared:testAndroidHostTest`) re-verified unaffected.
    - **Tests**: `./gradlew :tools:map-editor:test` — **13/13 pass**
      (`EditorGeometryTest` — click→normalized-point matches `MapViewport.toNormalized` exactly
      across several zooms/window sizes, plus drag/nudge delta math and its `0f..1f` clamp;
      `ImportValidatorTest` — every validation rule individually, **including the 300-row import the
      acceptance check names**; `MapPinDtoTest` — `MapPin` ↔ upsert-DTO mapping, and why
      `deleted_at`
      must be a required (non-defaulted) field so reactivating a soft-deleted pin on re-import
      actually sends it).
    - Not `[x]` because two of the three acceptance checks need a live backend this environment
      doesn't have (no Docker, same limitation steps 3/H/5A-bis already have): **"a pin placed in
      the tool is readable from the backend with the same coordinates after reopening"** and **"
      nothing is written without an admin session"** are both implemented and unit-tested at the
      mapping/logic level (DTO round-trip; `save()`/`publishOfflinePins()` refuse and set
      `lastError` when
      `adminSession` is `null`) but never actually exercised against a real Supabase project or a
      real GUI. "Importing 300 rows works" **is** fully verified (unit test, passing).
    - Notes / follow-ups: (1) run this module's new README "Manual test script" section end to end
      once Docker is available — it's written specifically to cover the two unverified acceptance
      items. (2) real mouse-drag-vs-map-pan gesture interaction (the pin's own `pointerInput` drag
      detector vs `MapViewer`'s pan/zoom one) is unverified on a real screen — same category of risk
      this module's own README flags for `core:mapview`'s three independent gesture blocks. (3) the
      left/pin-list panels and dialogs are intentionally plain Material3 (per this step's "needs no
      visual design" instruction), not restyled with `core:designsystem` beyond `IloveViTheme`/
      `IloveViMapPin`.

- [!] **5B - Pin placement with entry creation and Commit** (tools/map-editor, backend/supabase) -
  needs: 5B - done: 2026-10-21
    - Staged-pin draft/Commit flow on top of the original step 5B: `state/DraftState.kt` (place/
      select/multi-select/drag-rectangle/undo-redo ≥50 steps/duplicate guard), `draft/DraftStore.kt`
      (crash-safe local JSON autosave), `model/EntryFieldSpec.kt` (the one real-column-driven inline
      entry form definition), `ui/DraftEditorScreen.kt`/`PinInspectorPanel.kt`/`CommitDialog.kt`
      (new online-only screen; offline mode keeps the original `EditorScreen.kt` unchanged), and
      `data/CommitRepository.kt` calling a new atomic `public.commit_map_edits(payload jsonb)`
      Postgres function (`backend/supabase/migrations/20261021120003_commit_map_edits_rpc.sql`,
      plain `security invoker`, never the service-role key) that upserts entries and pins together,
      rolling back everything on any failure (one plpgsql function call is one implicit
      transaction). Entry images upload to a new `entry-images` Storage bucket
      (`20261021120002_storage_entry_images.sql`, same shape as `map-tiles`) inside the Commit call
      itself. `20261021120001_map_pins_allow_multiple_per_entry.sql` drops the old
      `map_pins_one_per_entry` unique constraint so an entry can have several pins; `map_pins.id`
      (client-generated UUIDs) is the only upsert/idempotency key from here on.
    - **`core:model` touch** (flagged per this step's own instruction): one KDoc sentence on
      `MapPin` ("there is at most one MapPin per located entry") was corrected — it described an
      invariant this feature deliberately removes. No field/shape change. `core:design`
      (`IloveViMapPin`
      gained an additive `dimmed` param) and `core:mapview` (`MapViewer` gained an opt-in
      `backgroundDragSelectEnabled`/`onBackgroundDrag*` callback set for marquee select, default
      off, zero behavior change for every other caller) were also touched — neither is `core:model`,
      both additive/backward-compatible, both re-verified via their own module's test suite plus
      `androidApp:assembleDebug`/`webApp:build`/`core:model:jvmTest` downstream.
    - **Tests**: `./gradlew :tools:map-editor:test` — **44/44 pass** (13 pre-existing + 31 new:
      `model/EntryFieldSpecTest`, `state/DraftStateTest`, `draft/DraftStoreTest`,
      `data/CommitPayloadBuilderTest`, the last using the new `data/FakeCommitRepository.kt` the
      acceptance explicitly asks for). `./gradlew :core:mapview:jvmTest :core:design:jvmTest
    :core:model:jvmTest` also pass, all re-run after every source change in this step, not assumed.
      `supabase-kt`'s `Postgrest.rpc(...)` and the newly-added `storage-kt`'s
      `BucketApi.upload`/`publicUrl` were verified against their real 3.1.4 sources jars (not from
      memory), per this module's own established convention.
    - **Not `[x]`, three acceptance items genuinely unverified in this environment** (no Docker, no
      local Supabase stack, no attached display — same limitation the original 5B entry above
      already hit): (1) **"all tests and SQL tests pass"** — the new
      `backend/supabase/tests/database/004_commit_map_edits.test.sql` (admin-can-commit,
      anonymous-cannot, bad-coordinate-fails-atomically, retry-creates-no-duplicate) was written
      following the existing three files' exact pgTAP idioms and reviewed carefully, but
      `supabase test db` itself was never run — only the Kotlin side (44/44) is confirmed green. (2)
      **"press Commit, and see entries and pins in the database tables on supabase.com"** and **"a
      failed commit leaves the database unchanged... pressing Commit twice does not duplicate
      anything"** — the RPC's atomicity and idempotency are argued from the SQL itself (a single
      plpgsql call is one transaction; every target is upserted by primary key) and exercised by the
      unwritten-but-unrun SQL test file above, but never actually watched happening against a live
      project. (3) **"I can place 50 pins... with a few clicks"** — `DraftStateTest` exercises 55
      programmatic placements (including the 50-step undo/redo cap precisely), but no one has
      actually clicked the running tool: `./gradlew :tools:map-editor:run` reaches `SignInScreen`
      with no exception, but the window closes on its own within seconds in this sandbox (no real
      display attached), same as already noted for the original 5B entry.
    - Notes / follow-ups: (1) run this module's README "Manual test script" step 6 (place ~50 pins,
      create/link entries with an image, multi-select, undo/redo, Commit, verify in Postgres, retry
      the same RPC call by hand) end to end once Docker/a real display are available. (2) the
      marquee drag-rectangle gesture addition to `core:mapview` has the same
      never-run-on-a-real-screen caveat already flagged for every other gesture block in this tool;
      shift-click multi-select is the fully-equivalent fallback if it turns out not to work as
      designed. (3) no live rectangle is drawn during a marquee drag (selection is still computed
      correctly on release) — a deliberate scope cut, not a bug. (4) `setCategoryForSelected`'s bulk
      recategorize only applies to pins linked to a staged (not yet committed) entry, documented in
      its own KDoc.
    - **Superseded by the entry below** (2026-10-22): the user asked for the UI to be rebuilt from
      scratch against the Functional Analysis's EDT-01..81 and the prototype screenshots, with the
      real RPC/Commit deliberately walked back out in favor of a clean fake-repository boundary
      first. None of this entry's file names (`DraftState.kt`, `DraftEditorScreen.kt`,
      `PinInspectorPanel.kt`, `data/CommitRepository.kt`, `commit_map_edits`) exist in the tree
      anymore — kept here only as a historical record of that earlier iteration, not as current
      module documentation. See the "Map editor UI (fake services)" entry below for what replaced
      it.

- [!] **5B - Map editor UI (fake services)** (tools/map-editor, core/design) - needs: 5B - done:
  2026-10-22
    - Full rebuild of `:tools:map-editor`'s UI against the Functional Analysis (EDT-01..81) and the
      prototype screenshots, on top of what already existed on disk from an earlier session of this
      same task (every file this step's spec names — `state/EditorState.kt`, `model/DraftPin.kt`/
      `DraftEntry.kt`/`EntrySource.kt`/`EntryFieldSpec.kt`, `data/fake/*`,
      `draft/FileDraftStore.kt`,
      `ui/Header.kt`/`ToolRail.kt`/`CategoryRow.kt`/`MapArea.kt`/`RightPanel.kt`/
      `InspectorPanel.kt`/
      `DraftPanel.kt`/`CommitDialog.kt`/`SignInScreen.kt` — was already present and already matched
      the spec's architecture almost exactly). This step's actual work was verifying that code
      against every acceptance item and fixing what verification turned up, not writing it from
      zero:
      (1) **a real bug, found by the one Compose UI test this repo has**: the Inspector's "New
      entry" form (name/sub-type/zone/description/extra fields/image picker/duplicate warning/
      Create/Delete buttons) is taller than the right panel's visible height on a 1280x800 window,
      and `InspectorPanel`'s root `Column` had no scroll modifier, so anything past the fold got a
      degenerate, unclickable layout node — fixed with `Modifier.verticalScroll(...)`. (2) autosave
      was writing `draft.json` synchronously on every intent call (a pin drag would have hit disk on
      every pointer-move frame) — now properly debounced (400ms, cancel-and-relaunch), with a
      `flushAutosave()` called from the window's `onCloseRequest` so the last edit before closing
      isn't lost to the debounce window. (3) Undo/Redo were always clickable even with an empty
      history (only the icon dimmed) — `core:design`'s `IloveViRailButton` gained a real `enabled`
      parameter (additive, every existing call site unaffected) and `ToolRail` now uses it. (4)
      database pins were outline-only but not actually dimmed (EDT-11 asks for both) — added 50%
      alpha. (5) **zero `@Preview`s existed** despite the fakes' own KDoc explicitly saying they
      were
      "for `@Preview`s" — added all 8 the acceptance names (empty draft, draft with an incomplete
      pin, new-entry form, existing-entry picker, each of the 4 Commit dialog states, login,
      non-admin), which required adding an internal `previewPhase` param to `CommitDialog` since its
      phase enum has no other way to be forced into a given state from outside.
    - **Tests**: `./gradlew :tools:map-editor:test` — **51/51 pass** (50 pure `kotlin.test` + the
      acceptance's explicit UI test, `ui/MapEditorAppUiTest.kt`: sign in as admin, place a pin,
      create a new entry for it, Commit, assert the success dialog — actually renders the real
      composable tree headlessly via Skiko and drives it with synthesized input, which is how (1)
      above was found). `core:design:jvmTest` and `androidApp:assembleDebug`/
      `shared:testAndroidHostTest` re-verified unaffected by the `IloveViRailButton` change.
    - **Not `[x]`: the one acceptance item this environment genuinely cannot check is "it looks like
      the screenshots."** This sandbox has no attached display and this conversation has no actual
      screenshot images to compare against (the Functional Analysis PDF's prototype images are
      referenced by the task text but weren't available to inspect pixel-by-pixel here) — "run the
      tool," "place 50 pins," "see warnings," "press Commit and see success/error," and "close and
      reopen to get the draft back" are all either directly exercised by the test suite above or
      architecturally guaranteed (`FileDraftStore` is the real, wired `DraftStore` in
      `di/AppModule.kt`, not a fake, so draft persistence across restarts is the actual production
      path, not a simulation) — but no human has watched this render, the same category of gap every
      GUI-heavy step in this repo has flagged since step 5A.
    - [x] **Load map from the tool (PLT-08)** - 2026-10-23 - done: the empty state's "No map
      loaded - Loading..." forever bug is fixed (plain-words reason + Retry within a 10s timeout,
      `state/EditorState.kt`'s `load()`); one-click "Load map image..." flow (validate → compute
      tile pyramid params via `core:model.TileScheme` → generate via `:tools:tilegen`'s
      `TilePyramidGenerator` → upload to `map-tiles` storage, parallel + retried + resumable →
      verify → register `map_config` only after everything else succeeds → reload) reuses
      `:tools:tilegen`'s pipeline and `core:model`'s tile math rather than duplicating either;
      header "Map" menu adds "Replace map..."/"Map info". 75/75 tests pass (22 new, incl. the
      upload-retry-skips-already-uploaded-files case against a hand-written `RemoteTileStorage`
      fake). Both the no-padding-for-non-square-images and the no-new-migration-needed parts of the
      task resolved to "already true" after reading `core:model`'s `TileScheme`/`MapViewport` and
      the existing `map_config_admin_*`/`map_tiles_admin_*` RLS policies — no code/migration change
      was needed for either, see `tools/map-editor/README.md`'s "Loading a map from the tool"
      section.
    - Notes / follow-ups: (1) not run against a real Supabase project or a real display — same
      sandboxed-environment gap every GUI/network step in this roadmap has flagged; the fake backend
      was launched here and confirmed not to crash (`./gradlew :tools:map-editor:run`), the real
      pipeline only against `MockEngine`/fakes (`SupabaseMapPublisherTest.kt`), never a live bucket.
      (2) `SupabaseMapPublisher` uploads under `v<version>/...`, while `:tools:tilegen`'s own CLI/
      `SupabaseAdminClient` uploads under `<version>/...` (no `v` prefix) — each is internally
      consistent with its own `tile_url_template`, but the two tools don't share one convention;
      flagged in the README's "Naming decisions" rather than silently picked.
    - [x] **Fix crash on an invalid `map_config` row (PLT-08)** - 2026-10-24 - done:
      `MapConfigRepository.loadCurrent` used to feed `map_config`'s current row straight into
      `MapConfig`'s constructor, whose `require(...)` checks throw `IllegalArgumentException` on a
      bad `tile_url_template`/zero-or-negative size/`min_zoom > max_zoom` — uncaught, this killed
      the loading coroutine (the map screen and "Load map image..." both called it). Fixed by making
      `loadCurrent()` return a `MapConfigResult` (`Loaded`/`Missing`/`Invalid(reason, rawRow)`,
      `data/MapConfigResult.kt`) instead of a throwing `MapConfig?` — `data/supabase/
    MapConfigRepository.kt`'s row→result conversion also now rejects a blank `source_image_url`
      (the one bad value `MapConfig` itself doesn't check) and never throws, even on a decode
      failure. `EditorState.load()` and both `MapPublisher.plan()`s (fake and real) switch on the
      three cases instead of null-checking; `MapPublishPlanning.decideVersion` now takes just the
      raw
      `map_version` (`MapConfigResult.mapVersionOrNull`) so "replace this broken row in place /
      create a new version" still works on an `Invalid` row. `ui/MapArea.kt`'s empty state shows the
      reason plus the raw `tile_url_template`/`source_image_url` for `Invalid`, with "Load map
      image..."/"Open local image" still both usable in every state. Added a global
      `CoroutineExceptionHandler` on the shared scope (`di/AppModule.kt`) plus a
      `Thread.setDefaultUncaughtExceptionHandler` fallback (`Main.kt`), both reporting to
      `error/UncaughtErrorReporter.kt`, which `ui/UncaughtErrorDialog.kt` renders (stack trace +
      "Copy details") — so any future uncaught background exception shows up instead of failing
      silently. 84/84 tests pass (9 new): `data/supabase/MapConfigRepositoryTest.kt` exercises the
      real Postgrest decode path via `MockEngine` for a missing row, a valid row, and each bad value
      (bad template, zero width, negative height, `min_zoom > max_zoom`, blank
      `source_image_url`) — none throw; `state/EditorStateMapLoadTest.kt` gained a case confirming
      an
      `Invalid` row reaches `EditorUiState.mapLoadError`/`invalidMapConfigRow` correctly. Not
      verified: a human has not watched the empty state actually render for an invalid row, or
      clicked "Copy details" against a real clipboard — same sandboxed-environment gap as every
      other GUI step in this roadmap; the fake backend was relaunched here and confirmed not to
      crash with the new wiring.
    - Notes / follow-ups (pre-existing, 5B): (1) a human needs to actually run
      `./gradlew :tools:map-editor:run` and compare it against the prototype screenshots
      pixel-by-pixel — nothing substitutes for that. (2) real mouse-drag-vs-map-pan interaction and
      keyboard-shortcut feel are unverified on a real screen, same class of risk this tool's README
      already flags for `core:mapview`'s gesture blocks. (3) ~~the real Commit... is not wired back
      in~~ — **done 2026-10-23**: `di/AppModule.kt`
      now branches on `MAP_EDITOR_BACKEND` (`di/MapEditorBackend.kt`) between the fake bindings and
      the real `data.supabase` ones. `EntryRepository`/`MapConfigRepository` implement the new
      interfaces directly; `PinRepository`/`AuthService`/`CommitService` each got a small adapter
      (`SupabasePinRepositoryAdapter`, `auth/SupabaseAuthService`, `SupabaseCommitServiceAdapter` —
      the auth one does its own sign-in rather than wrapping `AdminAuthRepository`, since EDT-02
      wants a non-admin to stay signed in rather than being signed out locally). 53/53 tests pass (2
      new, pure-mapping); both backends were launched here and reached a running window with no
      exception (fake for real, supabase against a dummy URL/key so only Koin wiring was checked,
      not an actual live commit) — still not verified against a real Supabase project end to end.
      (4) the duplicate-entry guard only checks already-committed database entries, not two draft
      entries created in the same session with the same name — not asked for explicitly and would
      need a parallel "use this draft entry instead" path; flagged rather than silently assumed out
      of scope.

- [x] **6 - core:database** (core/database) - needs: 2 - done: 2026-10-21
    - SQLDelight 2.0.2 KMP module (JVM/Android/iOS drivers behind `DatabaseDriverFactory`
      expect/actual); 13 tables (9 mirroring backend content tables + 4 local-only);
      case/accent-insensitive search via a `searchKey` column; epoch-millis dates;
      `verifyMigrations` wired with a committed schema-v1 baseline; 6 small DAOs (`CatalogDao`/
      `MapDao`/`MusicDao`/`NewsDao`/`SyncDao`/`PreferencesDao`) +
      `databaseModule` Koin module; 24 tests, all green on **both** JVM and iOS simulator.
    - Notes / follow-ups: (1) Android host unit tests remain not wired up for this module — same
      `withHostTest {}` gap already present across every other `core:*` module in this repo, not new
      here; JVM + iOS-simulator is this repo's established test-verification scope. (2) no real
      Android device/emulator or Supabase stack exists in this environment, so `AndroidSqliteDriver`
      was only verified by compiling against the real API, not by running on-device, and no real
      delta-sync engine exists yet to round-trip against the live backend tables from step 3 — see
      this module's README's "What was and wasn't verified here" for detail. (3) the next real
      schema change must add both a `.sqm` migration file and regenerate the committed schema
      snapshot (`src/commonMain/sqldelight/databases/1.db`), or `verifyMigrations` will correctly
      fail the build.

- [!] **7A - core:network** (core/network) - needs: 2 - done: 2026-10-30
    - New KMP library module (`kmm-library-plugin`, same convention plugin as `core:database`):
      remote-only data sources talking to Supabase, no `core:database`/`core:data`/Compose
      dependency. `SupabaseConfig.fromBuildConfig()` reads two `const val`s generated at build time
      by a new `generateNetworkBuildConfig` Gradle task (same shape as `core:mapview`'s
      `generateMapViewBuildConfig`) from the `SUPABASE_URL`/`SUPABASE_ANON_KEY` environment
      variables, falling back to the git-ignored root `local.properties` — never hardcoded, never
      committed (confirmed by grep: the real anon key only ever appears under the git-ignored
      `core/network/build/generated/...` output, not in any tracked file). `SupabaseClientProvider`
      builds the shared `SupabaseClient`: `useHTTPS = true`, only `Postgrest` installed (no `Auth` —
      no sign-in, read-only public content), 15s/10s/15s request/connect/socket timeouts, a 2-retry
      exponential-backoff policy (safe since every call is a plain `select`), and a per-platform
      `networkHttpClientEngine()` `expect`/`actual` (OkHttp on Android/JVM, Darwin on iOS — same
      `expect`/`actual` shape as `core:database`'s `DatabaseDriverFactory`).
    - 8 internal `@Serializable` snake_case DTOs (`dto/`) for `weapons`/`vehicles`/`items`/
      `stations`/`songs`/`map_config`/`map_pins`/`feed_sources`, matching
      `backend/supabase/migrations/20261011*.sql` column-for-column. 8 `*RemoteDataSource`
      interfaces + `internal` impls (`datasource/`), each `fetchChanges(since, pageSize)` through
      one shared `fetchDeltaPage` helper issuing exactly `backend/README.md`'s delta-sync convention
      (`updated_at=gt.<since>` omitted when `since == null`, `order=updated_at.asc,<idColumn>.asc`,
      `limit=<pageSize>`, soft-deleted rows never filtered out); `map_config` is the one table using
      `idColumn = "map_version"` instead of `"id"`.
    - `Change<T>(item, id, deletedAt)` (tombstone representation) + one `rowChange()` mapper helper
      shared by every `*Dto.toChange()`: a tombstoned row never even attempts mapping
      (`item = null`,
      `deletedAt` set); any mapping failure (unrecognized `sub_type`/`topic` enum, an unknown
      `map_pins.category`, or a `map_config` row failing `MapConfig`'s own `require`s) also becomes
      `item = null` with `deletedAt = null` instead of throwing, so one bad row never fails a whole
      page. `map_pins`' `category` + `entry_id` become an `EntryRef`
      (`categoryAndEntryIdToEntryRef`).
    - `NetworkResult` sealed type (`Success`/`Offline`/`Timeout`/`ServerError(code)`/`ParseError`) +
      `safeNetworkCall`, the one place every Ktor/supabase-kt exception is caught and classified
      (`RestException`/`ResponseException` → `ServerError`, `HttpRequestTimeoutException`/
      `ConnectTimeoutException` → `Timeout`, `SerializationException` → `ParseError`, anything
      else →
      `Offline`, `CancellationException` always rethrown) — no exception leaves this module's public
      API. `di/NetworkModule.kt`'s `networkModule` (plain Koin, same style as `databaseModule`)
      exposes the `SupabaseClient` single and all 8 data sources; not wired into `shared`'s
      `AppKoinModules.kt` aggregate, matching `core:database`'s own precedent of staying unwired
      until a consuming step (7B) needs it.
    - Every non-obvious supabase-kt/Ktor API used (`httpEngine`, `httpConfig` — needed
      `@OptIn(SupabaseInternal::class)`, found only by actually compiling, not from memory —
      `order`/`limit`/`filter { gt(...) }`, `decodeList`, `HttpRequestRetry`/`HttpTimeout`,
      `RestException.statusCode`) was verified against the real 3.1.4/3.6.0 jars via `javap`, per
      this repo's established convention for third-party surfaces.
    - **Tests**: `./gradlew :core:network:jvmTest` — **24/24 pass**; `./gradlew
    :core:network:iosSimulatorArm64Test` — pass too (both re-run after every fix, not assumed).
      `./gradlew :core:network:build` (every target, including the real `iosArm64` release
      framework, in isolation) passes clean. A full repo-wide `./gradlew build` does **not** pass,
      but this was confirmed pre-existing and unrelated to this module: re-running just
      `:core:design:linkReleaseFrameworkIosArm64 :shared:linkReleaseFrameworkIosArm64` — neither
      depends on `:core:network`, which nothing in the repo references yet — reproduces the exact
      same `java.lang.OutOfMemoryError: Java heap space` from the Kotlin/Native linker (this
      sandbox's Gradle daemon has no `org.gradle.jvmargs` heap bump, default 4 GiB, too little for
      several release iOS framework links back-to-back in one daemon). The full build's other
      failure, `:androidApp:mergeExtDexRelease`, also passes cleanly run on its own
      (`BUILD SUCCESSFUL`) — a *release*-variant dex merge no earlier roadmap step's own
      verification ever exercised anyway (every prior step only ever ran
      `androidApp:assembleDebug`), and its full-build failure reads as the same daemon already being
      heap-exhausted from the iOS linking that ran immediately before it, not an independent
      problem. Mapper tests (`EntryMapperTest`,
      `MapMapperTest`) use real fixture JSON covering a fully-populated row, every nullable column
      actually null, a tombstoned row, and — the acceptance's explicit case — an unrecognized
      `map_pins.category` ("BUILDING") producing an unmappable `Change` instead of crashing.
      `NetworkResultTest` unit-tests `safeNetworkCall`'s exception classification directly.
      `WeaponRemoteDataSourceTest` drives a real `WeaponRemoteDataSourceImpl` against a
      `MockEngine`, asserting the actual request query string (no `updated_at` filter when
      `since = null`, the
      `gt.` filter when it isn't, `order=...nullslast`, `limit=`) plus end-to-end
      `ServerError`/`ParseError`/`Offline` mapping through a real `Postgrest` call.
    - Not `[x]`: **the acceptance's own local-stack check — "against the local Supabase stack a
      small JVM test fetches the seed rows and one delta page" — could not actually be run**, same
      Docker-less sandboxed-environment limitation already flagged for steps 3/5B/6/H. The test
      exists and is written to do exactly that (`src/jvmTest/.../LocalSupabaseAcceptanceTest.kt`),
      self-skipping (not failing) when
      `SUPABASE_URL`/`SUPABASE_ANON_KEY` aren't set to a local stack's values — every other
      acceptance item (DTOs, mappers, data sources, `NetworkResult`, Koin module, the full test
      suite, "no secret in the repository") is met and verified.
    - Notes / follow-ups: (1) run `cd backend && supabase start && supabase db reset`, then
      `SUPABASE_URL=... SUPABASE_ANON_KEY=... ./gradlew :core:network:jvmTest --tests
      "*LocalSupabaseAcceptanceTest*"` on a machine with Docker before trusting the live round-trip.
      (2) `ResponseException`'s branch in `safeNetworkCall` is a defensive fallback not
      independently exercised by any test, since every error path actually driven through a real
      `Postgrest` call in this module's tests surfaces as `RestException` instead — flagged in the
      module README's
      "Error mapping" section rather than silently left untested. (3) an unrecognized throwable
      (e.g. a bare `java.net.SocketTimeoutException` below Ktor's own timeout plugin on the OkHttp
      engine)
      falls into the `Offline` bucket rather than `Timeout`, since this module avoids referencing
      platform-specific exception types from `commonMain` — documented, not accidental. (4) Android
      host unit tests remain not wired up for this module either, same `withHostTest {}` gap already
      present across every other `core:*` module in this repo. (5) **newly found, repo-wide, not
      specific to this module**: a full `./gradlew build` reliably OOMs this sandbox's Gradle daemon
      (default 4 GiB heap, no `org.gradle.jvmargs` override anywhere in the repo) while linking
      release iOS frameworks for `:core:design`/`:shared`; `:androidApp:mergeExtDexRelease` fails in
      that same full-build run but passes standalone, consistent with it just inheriting an
      already-exhausted daemon rather than having its own problem. Worth a real `org.gradle.jvmargs`
      heap bump before anyone relies on a full `./gradlew build` passing end to end; every
      per-module verification in this roadmap (including this step's) has so far only ever exercised
      individual module builds or `androidApp:assembleDebug`, never the full aggregate.

- [!] **7B - core:data** (core/data) - needs: 6, 7A, 3 - done: 2026-10-30
    - New KMP library module (`kmm-library-plugin`): the offline-first data layer sitting between
      `:core:database` (step 6) and `:core:network` (step 7A). No Compose, no feature module depends
      on `:core:database` directly from here on — everything goes through this module's repositories
      instead.
    - **Repositories** (`repository/`): `CatalogRepository` (weapons/vehicles/items — list/search/
      get-by-id/uniform `entry()` lookup), `MusicRepository` (stations/songs read-only + local-only
      compilation add/remove/reorder/rename/create/delete), `MapRepository` (current `MapConfig`,
      pins joined with entry name), `SettingsRepository` (preferences) — each a thin pass-through to
      the matching `:core:database` DAO (which already returns `Flow`s of `:core:model` types
      directly). `MusicRepository`/`MapRepository`/`SettingsRepository` reuse `:core:database`'s own
      `Compilation`/`CompilationSongEntry`/`MapPinWithEntryName`/`Preferences` view types as-is
      rather than duplicating them — a flagged judgment call, see this module's README.
    - **`ContentSyncer`** (`sync/`): pages `map_config` → `stations` → `weapons`/`vehicles`/
      `items` →
      `songs` → `map_pins` (that order: `songs.station_id` is a real local FK to `stations`;
      `feed_sources` deliberately excluded, that's steps 12A/12B) via one shared `syncTable` helper
      per table. Every page's upserts/tombstones and its `SyncDao` cursor advance together in one
      transaction (`IloveViDatabase.inTransaction`, a new small `:core:database` addition — see
      below), so **an interrupted sync resumes from the last committed page**, and **a failing
      table's error never touches any other table's own independent loop/transaction**. `sync()` is
      `Mutex`-guarded against overlapping calls, never throws, and exposes `syncState:
    StateFlow<SyncState>` (`IDLE`/`RUNNING`, `lastUpdatedAt`, `lastError`). `start(scope)` wires the
      "sync at app start" (one immediate, non-blocking `sync()`) and "sync when connectivity
      returns" (a `ConnectivityMonitor.isOnline` false→true watcher) triggers; "manual refresh" is
      just calling `sync()` directly — nothing to wire for that one.
    - **`ConnectivityMonitor`** (`connectivity/`): `expect class PlatformConnectivityMonitor(context:
    PlatformContext)`, reusing `:core:database`'s `PlatformContext` rather than inventing a second
      "app context." Android: a real `ConnectivityManager.NetworkCallback`. iOS: Network.framework's
      low-level C API (`nw_path_monitor_*`) — compiles clean against the real framework on both
      `iosArm64`/`iosSimulatorArm64`, found on the first attempt (no decompiling needed, unlike some
      other third-party surfaces this repo has had to reverse-engineer). JVM: a 5s
      `NetworkInterface`
      poll, a documented best-effort heuristic since no real desktop build of this app exists.
    - **Bundled seed** (`seed/`): `SeedImporter.importIfNeeded()` applies a small, obviously-fake,
      `seed-`-prefixed dataset exactly once (reusing `SyncDao`'s cursor table as the "already
      imported" marker, no new table needed) inside one transaction, so the app is never empty
      offline before the first real sync. The seed is a plain compiled-in Kotlin string (not a
      platform resource file — this module has no Compose dependency, so no `composeResources`, and
      this repo has no other established uniform KMP resource-bundling mechanism), parsed by a
      small, self-contained set of `@Serializable` DTOs independent of `:core:network`'s own
      (`internal` to that module anyway).
    - **Two small, justified touches to earlier modules**, both flagged in their own READMEs too:
      `:core:database` gained `IloveViDatabase.inTransaction` (bridges `suspend` DAO writes into one
      real SQL transaction — `transaction {}`'s own lambda isn't `suspend`, and every DAO write
      method here never actually suspends, so the `runBlocking` inside is safe; see that file's
      KDoc) and `dao/DaoFactory.kt` (one-line public factories for each DAO's `internal` `*Impl`,
      since this module's own tests build an in-memory database directly, without Koin/
      `PlatformContext`). `:core:network` gained the equivalent
      `datasource/RemoteDataSourceFactory.kt`, used by this module's `LocalSupabaseAcceptanceTest`
      to point a real data source at a local stack instead of the build-time-configured production
      client.
    - **Koin**: `di/DataModule.kt`'s `dataModule` — the four repositories, `ConnectivityMonitor`,
      `SeedImporter`, `ContentSyncer`, all singles. Not wired into `shared`'s `AppKoinModules.kt`,
      matching `:core:database`/`:core:network`'s own precedent of staying unwired until a consuming
      step needs it.
    - **Tests**: `./gradlew :core:data:jvmTest` — **14/14 pass** (re-run after every fix, not
      assumed): `ContentSyncerTest` (first sync; second sync requests/applies only the newest row; a
      tombstone removes a row; **an interrupted sync resumes** — a real 200-row page forcing
      pagination, then a simulated connection drop, then a second `sync()` that asks for exactly the
      missing data, not a redownload; one table failing leaves the other's rows applied and names
      itself in `lastError`; two concurrent `sync()` calls don't double-apply), `SeedImporterTest`
      (first import populates every table; importing twice doesn't duplicate), `MusicRepositoryTest`
      (**compilation reorder persists**, read back via `songsForCompilation`; rename/delete),
      `CatalogRepositoryTest`/`MapRepositoryTest`/`SettingsRepositoryTest` (read-what-was-written
      smoke tests). `./gradlew :core:database:jvmTest :core:network:jvmTest` re-run clean after this
      step's two touches to those modules. `:core:data:compileAndroidMain` and
      `:core:data:compileKotlinIosSimulatorArm64`/`:core:data:compileTestKotlinIosSimulatorArm64`
      (production *and* test Kotlin) all pass.
    - Not `[x]`, two acceptance items genuinely unverified in this sandboxed environment: (1) **the
      step's own acceptance check — "with the local stack, ... shows data with the network off after
      one sync, and a second launch downloads only the changes" — could not actually be run**, no
      Docker here, same limitation already flagged for steps 3/5B/6/7A/H. The test exists and is
      written to do exactly that (`src/jvmTest/.../LocalSupabaseAcceptanceTest.kt`), self-skipping
      when `SUPABASE_URL`/`SUPABASE_ANON_KEY` aren't set to a local stack's values. (2) **this
      module's `iosSimulatorArm64Test` *execution* (not compilation) could not be run**: the test
      binary fails to *link*, with every `sqlite3_*` symbol from the native SQLite driver's cinterop
      missing — reproduced as a genuine Kotlin/Native limitation (confirmed not a cache artifact via
      `-Pkotlin.native.cacheKind=none`; confirmed not a problem with this module's own Kotlin, since
      `:core:database`'s own `iosSimulatorArm64Test`, the identical dependency declared one module
      "shallower," passes in this same environment re-run immediately afterward) rather than
      anything wrong in this module's source — see the README's "What wasn't verified here" for the
      full investigation. Every required test still runs, and is green, on the JVM target.
    - Notes / follow-ups: (1) run `cd backend && supabase start && supabase db reset`, then
      `SUPABASE_URL=... SUPABASE_ANON_KEY=... ./gradlew :core:data:jvmTest --tests
      "*LocalSupabaseAcceptanceTest*"` on a machine with Docker before trusting the live round-trip.
      (2) the `iosSimulatorArm64Test` linking issue is worth a real fix (or a filed upstream issue)
      once there's time to dig further — it may also silently affect any *other* future module that
      both depends on `:core:database`/`:core:network` (or any other project with its own native
      cinterop dependency) *and* needs that same native dependency directly in its own test source
      set. (3) Android host unit tests remain not wired up for this module either, same
      `withHostTest {}` gap already present across every other `core:*` module in this repo. (4)
      `ContentSyncer.start()`/`SeedImporter.importIfNeeded()` are not called from anywhere in the
      real app yet — wiring them into `shared`'s actual startup sequence is for whichever step
      assembles the real app shell against this data layer (feature modules, starting at step 9).

- [!] **8 - app shell and navigation (five tabs)** (shared / androidApp / iosApp / webApp) - needs:
  1 - done: 2026-10-17
    - Run early/adapted, ahead of 6/7A/7B as explicitly instructed (so `needs` is just 1, not 1+7B
      as originally listed — no data/network/database is read in this step). Blocked only on the
      same thing recent steps have been blocked on: no real Android/iOS device or emulator exists in
      this environment, so "the app opens on Android and iOS" and "pins stay on the same image
      points while zooming" were never literally watched happening — see below for what *was*
      verified instead.
    - Template fully removed (`Greeting`/`GreetingUtil`/the `compose_multiplatform` drawable/the
      default `MaterialTheme`+"Click me!" screen are gone). `App()` is `expect` in `commonMain`; its
      real `actual` (IloveViTheme + the 5-tab shell) lives in `shared/src/appMain`, aliased as an
      extra source directory into `androidMain`/`iosMain` rather than a custom intermediate Kotlin
      source set — see `shared/README.md` for exactly why. `webApp`/js/wasmJs get a minimal
      placeholder `App()` instead, since `:core:design`/`:core:mapview` don't target those
      platforms.
    - Five tabs (Map, Catalog, Music, News, Settings, in that order) via `IloveViNavBar` +
      Navigation Compose. One flat `NavHost`; per-tab back stacks via the standard
      `popUpTo(startDestination){saveState=true}` recipe; "system back exits on Map" is Navigation
      Compose's own default at the graph's start destination. Secondary destinations (Map Filters,
      Detail, Compilation) have a back control; Compilation is reachable only from Music's
      compilation card. "Show on map" sets shared selection state and switches to Map the same way
      the bottom bar does. Stub screens are title+placeholder, design-system components only.
      Strings are in `composeResources` (`values`/`values-it`), no hardcoded text. The Map tab shows
      `:core:mapview`'s `MapViewerSampleScreen` whenever
      `MapViewBuildConfig.singleImageDevModeEnabled`
      is true. Koin started from `App()` with one empty `module{}` per Gradle module aggregated.
    - Verified (re-run 2026-10-18): `:shared:testAndroidHostTest`,
      `:shared:compileKotlinIosSimulatorArm64`,
      `:shared:compileKotlinJs`, `:shared:compileKotlinWasmJs`, `:androidApp:assembleDebug` and
      `:webApp:build` all still succeed.
    - Notes / follow-ups: (1) run on a real Android/iOS device or emulator once available. (2) no
      Compose UI test harness (`compose-ui-test`) is set up; the navigation tests are structural
      only. (3) no CI workflow, no `CLAUDE.md` (see step 0). (4) **2026-10-18**: the Android
      resource-packaging bug described under step 0/5A-bis would have affected this step's own
      rendering too (NavBar/tab fonts, the Map tab's bundled image) — same fix, not re-verified on a
      literal device yet.

- [!] **9 - feature:catalog** (feature/catalog) - needs: 7B, 1 - done: 2026-10-31
    - New KMP library module (`kmm-library-plugin` + Compose) — the first `feature:*` module in this
      repo, so no sibling module existed to copy; follows the `core:*` library pattern plus step 8's
      navigation contract (`FeatureRegistration`: plain callbacks, no `NavController`
      knowledge inside the module). Dependencies: `:core:model`, `:core:data`, `:core:design`,
      `:core:mapview` — no `:core:database`/`:core:network` anywhere, confirmed by the module's own
      `build.gradle.kts`.
    - **Catalog (CAT-01..05)**: title, three `IloveViChip`s (exactly one active), a 300ms-debounced
      `IloveViSearchField` ("Search by name or area") that never delays a category switch
      (`query.debounce(300).onStart { emit(query.value) }`), `IloveViListRow` per entry (category-
      colored sub-type icon placeholder, or the entry's real image via Coil3 once it has one; "type
        - zone"; chevron). Empty state and a one-time loading state (never reappears after the first
          successful read, even across category/search changes) are `CatalogUiState` derivations,
          not separate screens. Row tap calls `onOpenDetail(category, entryId)`, matching `EntryRef`
          's shape exactly, same as the step-8 stub it replaced.
    - **Detail (DET-01..04)**: back control, `IloveViCategoryBadge`, name (`displayLarge` =
      tokens.json's "display-xl"), type, a hero image area (Coil3 + placeholder), "Where to find it"
      (a small **non-interactive** `:core:mapview` `MapViewer` — an invisible full-size
      `Modifier.pointerInput` on top consumes every touch, since `MapViewer` itself has no
      "disable gestures" flag — centered on the pin, that pin selected; or "Location not available
      yet" with the "Show on map" button hidden entirely when the entry has no pin), "Show on map"
      (calls `onShowOnMap(entryId)`, which step 8's `AppNavHost` already wires to switch to the Map
      tab with that entry selected), and `AttributesTable` — one composable, exhaustive `when` on
      the concrete `Entry` subtype: Weapon's fixed type/price/ammo, Vehicle/Item's free-form
      `attributes` map rendered as label-humanized (`top_speed` → `Top Speed`) rows.
    - **ViewModels**: `CatalogViewModel`/`DetailViewModel` (`androidx.lifecycle.ViewModel`), each
      exposing one `StateFlow<...UiState>`. Category/search surviving tab switches costs nothing
      extra — `AppNavHost`'s existing `popUpTo(startDestination){saveState=true}` +
      `restoreState=true` tab-switch recipe already keeps a tab's `NavBackStackEntry` (and so its
      `ViewModelStore`) alive, so the same `CatalogViewModel` instance is simply still there.
      `DetailViewModel` takes `category`/`entryId` as Koin `parametersOf` constructor arguments
      (keyed `"$category/$entryId"`), not mutable state; a `category` that doesn't parse as a real
      `EntryCategory` resolves to `NotFound`, same as the entry simply not existing, never a crash.
    - **A real architectural bug found and fixed in `:core:data` (step 7B) while building this**:
      `MapRepository`/`MusicRepository`/`SettingsRepository` used to return `:core:database`'s own
      view types directly — `:core:database` is only an `implementation` dependency of `:core:data`,
      so those types are invisible to any module (like this one) that only depends on `:core:data`.
      Hit immediately trying to implement `MapRepository` in a test fake here. Fixed at the source:
      `:core:data` now has its own `MapPinWithEntryName`/`Compilation`/`CompilationSongEntry`/
      `Preferences` (new `core/data/.../repository/RepositoryTypes.kt`); `:core:data`'s 14/14 tests
      re-verified green after the fix.
    - **Two small, exact-precedent additions to `:core:design`**:
      `EntryCategory.toIloveViCategory()`
      (next to the existing `EntryCategory.icon()`) and `WeaponType`/`VehicleType`/`ItemType`/
      `Entry.labelRes()` (the 12 `design_subtype_*` strings step I added but never consumed — their
      own KDoc already said they were for exactly this use).
    - **Wired into the real app for the first time**: `AppFeatureRegistrations.kt` now calls this
      module's real `CatalogScreen`/`DetailScreen`, replacing (and deleting, along with their now-
      dead string resources) step 8's stub `CatalogStubScreen`/stub `DetailScreen`.
      `AppKoinModules.kt`'s `allAppKoinModules` wires in `databaseModule`/`networkModule`/
      `dataModule`/`catalogFeatureModule` for the first time (each of those modules' own README said
      "not wired in until a consuming step needs it" — this is that step), via a new
      `di/PlatformContextModule.kt` (`expect` in *true* `commonMain`, `actual` per real platform —
      not the `src/appMain` alias, since `expect`/`actual` need genuinely different compilations)
      providing the real Android `Context` `databaseModule` needs, read via `LocalContext.current`
      since there's no `Application` subclass and `App()`'s signature stays untouched. **Also found
      and fixed while wiring this**: step 7B's `SeedImporter`/`ContentSyncer` were never actually
      called from anywhere in the app — the local database would have stayed empty on a fresh
      install regardless of this step's own code being correct. `App()` now calls
      `seedImporter.importIfNeeded()` then `contentSyncer.start(appScope)` in one `LaunchedEffect`.
    - **Tests**: `./gradlew :feature:catalog:jvmTest` — **9/9 pass**: `CatalogViewModelTest`
      (category switch; search is case- **and accent-insensitive**, via a small local fold in the
      fake repository; no-matches is empty but not loading; loading is `true` before any
      collection), `DetailViewModelTest` (an entry **without a pin** has a `null` pin in the loaded
      state; one with a pin has it; unknown id and malformed category both resolve to `NotFound`),
      `CatalogToDetailUiTest` (`compose.desktop.uiTestJUnit4`, same dependency
      `:tools:map-editor`'s own UI test uses, this repo's only other one — the acceptance's explicit
      scenario, "row tap -> Detail -> 'Show on map' event," driving the real stateless
      `CatalogScreenContent`/`DetailScreenContent`; interactive nodes found by `testTag`, not
      visible text, since `stringResource()` was observed still resolving empty after
      `waitForIdle()` in this plain JVM `createComposeRule()` environment). Re-verified after every
      fix:
      `./gradlew :core:data:jvmTest :core:design:jvmTest :core:network:jvmTest
      :core:database:jvmTest` all still pass. `androidApp:assembleDebug`,
      `shared:compileKotlinIosSimulatorArm64`, `shared:compileKotlinJs`,
      `shared:compileKotlinWasmJs`, `webApp:build`, `shared:testAndroidHostTest` all pass with the
      full, real DI graph wired end to end.
    - Not `[x]`: same sandboxed-environment limitation as steps 3/5B/6/7A/7B/8 — **no real
      Android/iOS device or emulator exists here, so nothing was literally watched rendering**.
      `androidApp:assembleDebug` succeeding with the complete real DI graph (a real
      `AndroidSqliteDriver` behind `PlatformContext`, the seed importer/syncer wired, every view
      model resolving through Koin) is the strongest check available in this environment, and every
      acceptance claim is either a verifiable fact (no network/database dependency; strings are all
      `composeResources`; the design-system components are genuinely used, not reinvented) or
      exercised by the test suite above against fakes — but "Catalog and Detail work offline with
      the seed data for all three categories" was never watched happening on a real screen end to
      end, which is what the acceptance literally asks for.
    - Notes / follow-ups: (1) run the app on a real Android/iOS device or emulator once available —
      this is the single biggest remaining gap across this entire roadmap, not specific to this
      step. (2) the mini-map's "two zoom steps in from `minZoom`" and the attributes table's visual
      layout are unconfirmed judgment calls (nothing in the Functional Analysis specifies either) —
      flagged in this module's own README, not silently assumed. (3) Android host unit tests remain
      not wired up for this module either, same `withHostTest {}` gap already present across every
      other `core:*`/`feature:*` module in this repo. (4) the `stringResource()`-not-resolving-in-
      JVM-tests issue found while writing the UI test is worth investigating properly at some
      point — right now it's only worked around (via `testTag`) for this one test, and could bite
      any future Compose UI test in this repo the same way.

- [ ] **10 - feature:music** (feature/music) - needs: 7B, 1 - done:
    - Not started. No `feature:music` module exists.

- [!] **11 - feature:map** (feature/map) - needs: 5A, 5A-bis, 7B, 1 - done: 2026-10-31
    - New KMP library module (`kmm-library-plugin` + Compose), following `:feature:catalog`'s
      pattern and step 8's `FeatureRegistration` contract (plain callbacks, no `NavController`
      knowledge inside the module). Dependencies: `:core:model`, `:core:data`, `:core:design`,
      `:core:mapview` — confirmed by the module's own `build.gradle.kts`.
    - **Product rule, double-checked end to end**: no location marker, no "center on player"
      control, no location permission anywhere. `AndroidManifest.xml`'s merged manifest has only
      `INTERNET`/`ACCESS_NETWORK_STATE`; `iosApp/iosApp/Info.plist` has no `NSLocation*` key; a
      repo- wide grep for `ACCESS_FINE_LOCATION`/`LocationManager`/`FusedLocation`/
      `CLLocationManager`/
      `centerOnPlayer`/`centerOnUser` across this module, `:core:mapview`, `androidApp` and `shared`
      returns nothing.
    - **Clustering (MAP-10/MAP-11)**: `cluster/PinClusterer.kt`, pure Kotlin (no Compose import),
      grid-bucketed (`CLUSTER_DISTANCE_PX = 48f` cell size) with 3x3-neighbor union-find merging — O
      (n), not O (n²). `CLUSTER_MIN_SIZE = 10` (strictly more pins than this becomes a badge) and
      `CLUSTER_DISTANCE_PX` are both named constants with KDoc in one file. Filters are applied
      upstream (`MapViewModel`) before `MapScreen` ever calls `cluster()`; the selected pin is
      excluded from every group and re-added by the caller as its own always-on-top
      `MapViewerItem.Pin` (found and fixed during this step's own UI test: the first version never
      added it back, so a selected pin would have vanished from the map entirely); no clustering at
      `zoom >= maxZoom`; cluster ids are `"cluster:<cellX>:<cellY>:<size>"`, built from the
      screen-space grid cell of the group's centroid, never random; dominant-category badge color is
      whichever category strictly has the most pins in the group, `null` on a tie. Recomputed via
      `remember` keyed on the camera's center/zoom/viewport size plus the ViewModel's
      pins/selection/ showLabels — not on every recomposition.
    - **Measured clustering performance** (JVM, `PinClustererTest`, this sandboxed environment — see
      below for what a real device couldn't confirm): **500 pins in ~3.5-4ms** (budget <16ms),
      **2000 pins in ~24ms** (budget <50ms) — both asserted, not just logged, and consistent across
      repeated runs.
    - **Map screen** (MAP-01..09): wordmark + search + filters header, three category chips
      (`IloveViChip`, all on at first launch, each toggling `MapFilterState`), `MapViewer` in tile
      mode, `IloveViMapControls` (zoom in/out only, no third button — the product rule's own
      2-button cluster) top-right, `IloveViCompactCard`-based summary card + "View details" for the
      selected pin, empty-hint overlay (MAP-09) when the active filters leave nothing to show,
      Could-priority search (name/zone substring match, scoped to currently-visible pins). Tapping
      the background clears the selection. "Show on map" (MAP-06) resolves `selectedEntryId` (step
      8/9's existing navigation-contract parameter, unchanged) to a pin and animates the camera
      there via `MapViewerState.animateTo`, re-running only when that id or the pin list changes.
    - **Filters screen** (MAP-05/MAP-09): one `IloveViChipGroup` per category, each with its own
      `WeaponType`/`VehicleType`/`ItemType` sub-type chips (reusing `:core:design`'s existing
      `EntryIcons.kt` icon/label mapping — nothing new added there); "Reset" turns every category
      and sub-type back on; "Show on map" is `onBack`, since every chip tap already applies live
      through the shared `MapFilterState` (a Koin **`single`**, not a `viewModel` — Map and Filters
      are two different routes/back-stack entries, so the `saveState`/`restoreState` tab-switch
      trick that keeps `CatalogViewModel` alive across a *tab* switch doesn't apply between two
      different routes; a plain singleton has no such lifecycle tie). `MapRepository.pins`/
      `MapDao.pins` treat an empty `subTypes` set as "no filter" — the opposite of "every sub-type
      off"; `MapViewModel`
      special-cases that to an explicit empty result instead of forwarding it.
    - **Offline tiles (MST-04)**: `:core:mapview`'s `TileLayer` already Coil3-disk-caches whatever
      it loads; new `map/TilePrefetch.kt` additionally `enqueue`s every tile at `mapConfig.minZoom`
      through Coil3's `SingletonImageLoader` the moment the config loads, so the lowest zoom level
      is warmed even before the user pans/zooms there.
    - **Two small additions to `:core:design`**: `IconSearch`/`IconFilter` (`IloveViIcons.kt`, same
      24x24 stroke-only style as every other icon there) — flagged in their own KDoc as having no
      design-canvas asset to match against, since no exported "Map"/"Map filters" screen mockup
      exists anywhere in this repo's design-system docs (only component-level ones: Chip, MapPin,
      ClusterBadge, SearchField, etc. — confirmed by listing `documentation/ilovevi-design-system
    2/components/`).
    - **Wired into the real app**: `AppFeatureRegistrations.kt` now calls this module's real
      `MapScreen`/`MapFiltersScreen`, replacing step 8/9's `MapTabScreen` (which gated on
      `MapViewBuildConfig.singleImageDevModeEnabled` to show the dev sample) and stub
      `MapFiltersScreen`. `AppKoinModules.kt` wires in `mapFeatureModule`. `shared/build.gradle.kts`
      gained `:feature:map`.
    - **Tests**: `./gradlew :feature:map:jvmTest` — **30/30 pass**: `PinClustererTest` (13 — see
      clustering bullets above for exactly what each one proves), `MapViewModelTest` (11 — map-
      config-missing, all-on-by-default, category toggle hiding a category's pins, every-sub-type-
      off leaving pins empty, filter reset, selection/clearing, `selectByEntryId` incl. a miss,
      `showLabels` following the settings preference, a repository error surfacing as
      `MapUiState.Error`, pre-collection `Loading`), `MapFiltersViewModelTest` (5 — all-on at start,
      one sub-type off, every-sub-type-off, reset, shared-state visible across two separately-
      constructed view models), `MapPinToDetailUiTest` (1, `compose.desktop.uiTestJUnit4` — the
      acceptance's explicit scenario, pin tap -> summary card -> "View details" fires with the pin's
      `(category, entryId)`; the pin tap goes through its real `onClick` semantics action rather
      than a raw synthetic mouse click, since the latter has to survive `MapViewer`'s own custom
      gesture detector, which `:core:mapview`'s README already flags as unverified in this
      device-less sandbox). Re-verified after every change: `feature:catalog:jvmTest`,
      `core:data:jvmTest`, `core:mapview:jvmTest`, `core:design:jvmTest` all still pass;
      `androidApp:assembleDebug`, `shared:compileKotlinIosSimulatorArm64`,
      `feature:map:compileKotlinIosSimulatorArm64` all succeed with the full real DI graph.
    - Not `[x]`: same sandboxed-environment limitation as steps 3/5A/5A-bis/5B/6/7A/7B/9 — **no real
      Android/iOS device or emulator exists here.** Three acceptance items specifically depend on
      watching real rendering, which this environment cannot do: (1) "pins appear in the same place
      at every zoom and screen size" is guaranteed by construction (clustering and drawing both
      derive from the exact same `MapViewport.toScreen` conversion) but never visually confirmed on
      a real screen; (2) "500 fake pins show a few round badges, zooming in splits them" is proven
      algorithmically (`PinClustererTest`) and performance-budgeted, but never watched happening in
      a real rendered frame; (3) "works offline after one sync" has real code behind it now (Coil3's
      disk cache plus the new `minZoom` prefetch) but whether it actually survives an app restart
      with the network truly off was not, and could not be, checked here. The one item fully and
      directly verified is "no location permission anywhere" (see the product-rule bullet above).
    - Notes / follow-ups: (1) run on a real Android/iOS device or emulator once available — the
      single biggest remaining gap across this roadmap, not specific to this step. (2) the tile-
      prefetch's real effect (does it actually warm a persistent on-device cache) is unverified; it
      has no dedicated test of its own since an `ImageLoader` isn't something this module's fakes
      substitute. (3) search has no dedicated test beyond what `MapViewModelTest` exercises
      indirectly — not called out as its own acceptance item, so not blocking, but worth adding if
      search becomes a higher priority later. (4) `IconSearch`/`IconFilter` are hand-drawn, not
      sourced from a design-canvas export — flagged inline, worth revisiting once/if one exists.

- [!] **5A-quater - Tile pipeline and overzoom (app and editor)** (core/mapview, tools/tilegen,
  feature/map, tools/map-editor) - needs: 5A, 5A-bis, 5B, 11 - done: 2026-11-03
    - **Diagnosis (step 0)**: the tile-reload-flicker bug's prior implementation (plain Coil3
      `AsyncImage` per tile, no custom cache/coordinator) was already removed from the tree by the
      time this step's work resumed in this environment, so the historical "before" behavior could
      not be reproduced live against a real device/display or a real Supabase-Storage-backed map
      here (same limitation every other roadmap step already has — no Android/iOS device or
      emulator, no real network credentials wired into this sandbox). The diagnosis below is
      reconstructed by reading what the old approach necessarily implied, not by watching the bug
      happen and then stop. Four confirmed causes (full detail in `core:mapview`'s README, "Tile
      pipeline > Diagnosis"): (1) no hysteresis on the continuous-to-integer tile zoom, so holding a
      pinch near an integer boundary flipped the whole visible tile set on every tiny tremor; (2)
      `state.zoom`/`state.center` read in composition rather than the draw phase, recomposing (and
      re-requesting) the entire tile layer on every pinch/pan frame; (3) cancel-and-restart of
      still-needed in-flight requests as a direct consequence of (2); (4) no fallback-to-ancestor
      rendering at all, so a decoding/missing/failed tile showed nothing instead of a scaled-up
      lower-resolution tile — the actual black/empty squares.
    - **Part 1 (core:mapview tile pipeline)**: new `core/mapview/.../tile/` package
      (`TileCacheConfig`
      — every tunable, named, with KDoc; `TileKey`; `TileMemoryCache` — LRU-by-bytes,
      `MutableStateFlow`-backed so `peek` is lock-free from the draw phase, zoom 0..min
      (nativeMax,3)
      pinned up to 85 tiles; `TileDiskCache`/`TileDiskFiles` — versioned-URL-keyed, no revalidation,
      size-capped LRU, per-platform actuals (android/ios/jvm); `TileFetcher`/`HttpTileFetcher` —
      disk then network, decodes via the module's existing `decodeImageBitmapDownscaled`;
      `TileLoadCoordinator` — 3-priority FIFO queues (viewport > prefetch-ring > next-zoom), bounded
      parallelism (8), in-flight dedup, retry-with-backoff, 120ms debounced stale-cancellation;
      `TileZoomSelector` — hysteresis band (0.15 zoom levels); `findAncestorFallback` — walks up to
      the nearest resident ancestor, returns its crop rect; `TileCacheMetrics`;
      `TileLowMemorySignal`/`TileReducedMotionSignal` — per-platform actuals; `TileEngine` — bundles
      all of the above, `rememberTileEngine` builds/disposes one per
      `(mapVersion, tileUrlTemplate)`).
      `TileLayer.kt` rewritten to draw every visible tile from one `Canvas` lambda — fallback tile
      (if the real one isn't ready) drawn first, real tile cross-faded on top over 150ms (skipped
      under reduced motion or for an already-resident tile) — reading `state.zoom`/`state.center`
      *inside* the draw lambda only, never in `TileLayer`'s own composable body. `TileDebugOverlay`
      (off by default, absent from release builds via `MapViewBuildConfig.debugOverlaySupported`)
      shows zoom/visible/cached/loading/failed counts and hit rates.
    - **A real concurrency bug found and fixed during this step, not inherited from before**: the
      first version of `TileLoadCoordinator` written for this step mutated its plain `HashMap`/
      `ArrayDeque` queues directly from each fetch job's own `finally` block — safe only if every
      completion resumes on the same thread, which isn't guaranteed once 8 fetches are genuinely
      running in parallel against a real HTTP engine. `MapViewerCameraStabilityUiTest` caught a real
      `ConcurrentModificationException` from this. Fixed by routing every mutation through one
      `Channel`-fed consumer coroutine (`request()`/a fetch job's completion both just
      `Channel.trySend`, lock-free from any thread) — the same confined-ownership pattern
      `MapGestures.kt`'s own `pendingSnaps` channel already uses for the camera. Re-verified clean
      across 3 repeated full `:core:mapview:jvmTest` runs after the fix.
    - **Part 2 (overzoom + deeper detail)**: `TileCacheConfig.OVERZOOM_LEVELS = 3`, re-exported
      publicly as `TILE_OVERZOOM_LEVELS` (`core/mapview/.../TileOverzoom.kt`) for callers outside
      this module's internals. `MapViewer`'s camera zoom ceiling is `nativeMaxZoom +
    OVERZOOM_LEVELS`; `TileLayer` requests/renders only at `min(cameraZoom, nativeMaxZoom)` and
      scales the whole canvas up past that with `FilterQuality.Medium`. Pan limits stay correct at
      an overzoomed scale for free — `TileScheme.scaleAt(zoom: Float)` (unlike its `Int` overload)
      has no upper clamp, so `centerBoundsForAxis` just keeps extrapolating the same formula; no
      `core:model` change was needed (confirmed by reading `TileScheme`/`MapViewport` before writing
      any overzoom code — the one place this step came close to touching `core:model` and didn't
      have to). `:tools:tilegen`: `MapPublishPlanning.MAX_DIMENSION_PX` raised 16384 -> 32768
      (strip- based generation already scaled to it); `TileFormat` (WEBP default, quality 0.82, via
      `com.luciad.imageio.webp.WebPWriter`, PNG fallback) — decode support verified across every
      client target (Android `BitmapFactory` since API 14/18, Compose Multiplatform's Skia decoding
      on JVM/iOS) rather than assumed, so no target needed the PNG fallback; tile_url_template now
      carries the real extension. `SupabaseStorageLimits` (Free: 1GB total/50MB per file; Pro:
      100GB — checked against supabase.com/pricing+docs on 2026-11-01, comment cites the date to
      revisit). `MapPublishPlanning.estimateStorageBytes` (real sample tiles, encoded, averaged,
      scaled to the full tile count) is now actually wired into both `FakeMapPublisher` and
      `SupabaseMapPublisher`'s `plan()` (previously computed but never called —
      `estimatedStorageBytes`
      would have stayed `0` forever). `LoadMapDialog`'s confirm step now shows the "1 image pixel =
      1 screen pixel at zoom N; beyond it the map is enlarged" sentence, the storage estimate, and
      `storageWarning`'s text when relevant. `MapInfoDialog` now shows the overzoom levels and total
      detail ratio on top of the native zoom range. `SupabaseRemoteTileStorage` already set
      `Cache-Control: max-age=31536000, immutable` and already skips re-uploading a file a previous
      attempt already succeeded on (file-level "resumable" retry — not byte-level/chunked resumable
      upload, which nothing in the task's own wording specifically asked for beyond that).
      `map_config.min_zoom`/`max_zoom` remain the native levels only; overzoom is purely a client-
      side addition on top, so the existing 4096px/zoom-4 map keeps working unchanged.
    - **Part 3 (both clients, public-API-only)**: `feature:map`'s own `TilePrefetch.kt` (a second,
      redundant tile-preloading mechanism) deleted — `core:mapview`'s
      `TileEngine.preloadPinnedLevels`
      is now the one implementation, reused rather than duplicated (also satisfies "the app already
      downloads the lowest levels at first launch for offline use: reuse that"). `MapArea.kt`
      (`:tools:map-editor`) gained two hidden shortcuts reaching `core:mapview`'s public API only:
      `Ctrl+Shift+D` (`debugOverlayEnabled` param, forwarded through the `pins`-based `MapViewer`
      overload) and `Ctrl+Shift+B` (`rememberTileBenchmarkTrigger`, a `@Composable` wrapper that
      hides `coil3.PlatformContext`/`CoroutineScope` plumbing from the caller entirely). Mobile
      memory safety: `TileMemoryCache.clearUnpinned()` fires on Android `onTrimMemory`/`onLowMemory`
      and iOS's memory-warning notification; **not independently exercised on a real low-end Android
      emulator profile in this environment** (none exists here) — the mechanism itself (drop
      everything but the pinned overview) is unit-tested (`TileMemoryCacheTest
    .clearUnpinned_dropsEverythingExceptThePinnedKeys`) but "never crashes with OOM while zooming
      quickly on a real device" specifically could not be checked here.
    - **Tests**: `TileZoomSelectorTest` (8, commonTest — hysteresis both directions, clamping,
      reset), `TileMemoryCacheTest` (8, jvmTest — LRU-by-bytes eviction, pinned-never-evicted,
      `clearUnpinned`, no-double-count), `TileFallbackTest` (6, jvmTest — z-1/z-2/z-3 ancestor
      lookup and exact crop rects, cross-map-version isolation, no-ancestor -> null),
      `TileLoadCoordinatorTest`
      (10, jvmTest — 100 concurrent requests for one tile = 1 fetch; already-cached tile never
      re-fetched; VIEWPORT drains before PREFETCH_RING before NEXT_ZOOM regardless of enqueue order;
      retry-with-backoff to success and to exhaustion; a failed VIEWPORT tile gets another chance, a
      failed RING/NEXT_ZOOM one doesn't; stale in-flight tile swept after the stability delay, a
      continuously-re-requested one never is; `arrivedAtMillis` timing), two new overzoom/pan-limit
      tests in `MapGeometryTest` (scaleAt continuing smoothly 8x past the native ceiling;
      `centerBoundsForAxis` staying a valid non-crossed range at that scale), `TileBenchmarkTest`
      (4, jvmTest — zero duplicate fetches, >95% second-pass hit rate, positive reqs/sec and p95>
      =p50, the zoom-sweep step sequence itself), `TileFormatTest` + `TilePyramidGeneratorTest`'s
      WebP round-trip test (already existed from earlier work this step resumed), 5 new
      `MapPublishPlanningTest` cases for the exact 8192/16384/32768px -> zoom 5/6/7 ->
      1365/5461/21845-tile table task item 7b names (4096 -> zoom 4 -> 341 was already covered) plus
      the raised 32768px dimension limit.
      `./gradlew :core:mapview:jvmTest :tools:tilegen:test :tools:map-editor:test :feature:map:jvmTest`
      all pass (`feature:map:jvmTest`'s `PinClustererTest` flaked once under heavy parallel-build
      CPU load with a timing-budget assertion unrelated to this step's own changes — confirmed by
      re-running it alone, which passed; pre-existing, not introduced here). `:core:mapview`
      compiles on `compileAndroidMain`/`compileKotlinIosSimulatorArm64`; `androidApp:assembleDebug`
      and `shared:compileKotlinIosSimulatorArm64` (the real DI graph, both consumers) succeed.
    - **Benchmark numbers**: no "before" measurement is possible in this environment — see the
      diagnosis note above (the old implementation was already gone). After the fix, this
      environment's pure-Kotlin simulation (`TileBenchmarkRunner`'s own honest scope — see
      `core:mapview`'s README; no real device/display here to measure an actual rendered frame from)
      against an 8192x6144 scheme, zoom 0-5, a 1080x2000 viewport, and a fake fetcher with a 15ms
      artificial delay per tile: **2088 total requests, 0 duplicates, 100% second-pass cache hit
      rate (target >95%), ~1072 requests/sec, frame time p50 0.107ms / p95 0.332ms** (well under a
      16ms/60fps budget for the pure tile-selection-and-request math).
    - Not `[x]` because: the acceptance items that depend on watching a real rendered map — "no
      black/empty squares while zooming/panning a real map," "zoom beyond the old limit... with
      smooth enlargement" actually looking smooth, "all tests pass on every target" (Android/iOS
      *compile* but have no real device/emulator/test-runner here to actually *run* on) — could not
      be directly observed in this sandboxed, display-less environment, the same recurring
      limitation steps 3/5A/5A-bis/5B/6/7A/7B/9/11 already carry. Everything that *can* be verified
      without a real display or device — the algorithmic fixes (hysteresis, fallback, dedup,
      debounced cancellation, the concurrency bug), the overzoom math, the tilegen/editor dialog
      changes, and the benchmark's own measurement logic — is implemented and unit/benchmark-tested
      green.
    - Notes / follow-ups: (1) run this on a real Android/iOS device or emulator (and against a real
      Supabase-Storage-backed map) before trusting the "no flicker" claim visually — the single
      biggest remaining gap, same as every other roadmap step's own note. (2) the low-memory-signal
      response needs an actual low-end Android emulator run to confirm it prevents an OOM crash
      while zooming quickly, not just that the unpin-everything mechanism itself works in isolation.
      (3)
      `PinClustererTest`'s timing-budget flake under heavy build load is pre-existing and unrelated
      to this step, but worth tightening (a wider margin or a warm-up iteration) if it keeps
      recurring in CI. (4) byte-level/chunked resumable upload (TUS-style) was not added —
      "resumable" here means the pre-existing "retry skips already-uploaded files" behavior, which
      is what the task's own wording plausibly meant given no TUS-specific detail was named; flag if
      that's wrong.
    - **2026-11-03 follow-up, found by actually running the app**: three real bugs no unit test
      could have caught (none of them require a real device per se, but all three only show up when
      a
      `DrawScope` lambda's actual draw timing/content is observed, which this sandboxed environment
      cannot do on its own — confirming the README's own "not yet confirmed" caveat was warranted).
      (1) the map only ever redrew on a drag/pinch, never spontaneously when a tile finished
      decoding in the background (`TileMemoryCache.peek` was a plain, non-Compose-observed read) —
      perceived as "pins move before the map does," since pins have no such dependency. Fixed via
      `TileMemoryCache.generation`, a real `mutableIntStateOf` counter `TileLayer`'s draw lambda now
      reads to register the missing dependency. (2) tiles never actually scaled continuously while
      pinching *within* the native zoom range — only past the native max (overzoom) — because the
      old scale formula was `2^(continuousZoom - min(continuousZoom, nativeMaxZoom))`, exactly `1.0`
      the entire native range; the map was frozen at a fixed pixel size until the discrete render
      level flipped, which is why the tile grid's seams were visible while zooming. Fixed by
      `tileRenderScaleFactor` (`2^(continuousZoom - renderLevel)`, one formula for both cases),
      extracted as a pure function specifically so it's unit-testable (`TileRenderScaleFactorTest`,
      5 cases) — the inline version living only in a `DrawScope` lambda never was. (3) a defensive
      1px tile-overlap fix for residual rounding seams, now that (2)
      actually scales the canvas. `./gradlew :core:mapview:jvmTest` — 96/96 pass; Android/iOS
      compile; `:tools:map-editor:test`/`:tools:tilegen:test` still pass. **Still not visually
      confirmed on a real device** — same recurring limitation.

- [x] **12A - backend fetch-news job** (backend/supabase or a separate worker) - needs: 3 - done:
  2026-11-03
    - Backend-merged news feed (Functional Analysis section 9.3, option B): a scheduled Edge
      Function reads/merges every RSS/Atom feed; the app reads one clean `news_items` list. This
      supersedes `feed_sources`' original migration comment ("NewsItem is a device-local cache").
    - **Migration** (`20261103120001_news_items_and_feed_status.sql`): `news_items` (mirrors
      `:core:model`'s `NewsItem` exactly: id/feed_id/title/link/published_at/excerpt/image_url/
      topic/fetched_at, plus the usual updated_at/deleted_at), `link unique` (the upsert key), `id`
      is `sha256(feed_id + link)` truncated (stable across runs, collision-proof across different
      feeds reusing the same guid). `feed_status` (feed_id pk, last_success_at/last_error/
      last_error_at — one row per feed, overwritten each run, no soft-delete since it isn't synced
      content). **RLS deliberately deviates from every other content table here**: public read on
      `news_items` (admin-only read on `feed_status`), but *no* insert/update/delete policy on
      either — not even an admin can write; only `service_role` (which bypasses RLS entirely, a real
      Supabase/Postgres property, not a policy) ever does, since only `functions/fetch-news`
      is meant to write these tables. `backend/README.md`'s "Row Level Security" section now covers
      this exception explicitly.
    - **Edge Function** (`functions/fetch-news/`, Deno/TypeScript, 8 files): `fetch.ts` (HTTPS
      download, 10s timeout, 5MB cap, streamed so a dishonest/missing Content-Length can't bypass
      the limit) — `parse.ts` (RSS 2.0 + Atom via `npm:fast-xml-parser`, auto-detected from the root
      element; `XMLValidator.validate()` run first, since the parser alone is too lenient to
      reliably throw on real malformed XML — confirmed while writing this step's own tests) —
      `html.ts` (HTML-stripped, ~200-char, word-boundary-truncated excerpt) — `merge.ts` (tags items
      with feed/topic, de-dupes by link then by normalized title, derives id) — `trim.ts`
      (keeps the newest 200 per topic) — `orchestrate.ts` (runs every feed's fetch+parse
      *independently*, catching each one's own failure — the actual "one feed must not block the
      others" mechanism, built specifically to be unit-testable with an injected fake fetcher) —
      `db.ts` (all Supabase I/O, service-role client, `SUPABASE_URL`/`SUPABASE_SERVICE_ROLE_KEY`
      auto-injected by the platform, never read from a file). **"Never stores full article text"**
      lives as a comment in both `index.ts` (top of file) and `functions/fetch-news/README.md`
      ("What this job stores"), per the task's own instruction that it live in both places.
    - **Scheduling** (`20261103120002_fetch_news_schedule.sql`): `pg_cron` + `pg_net`, every 30
      minutes, calling the function over plain HTTP from inside the Postgres container
      (`host.docker.internal:54321` — the Supabase CLI's own documented local-stack address).
      `config.toml`'s `[functions.fetch-news]` sets `verify_jwt = false` specifically so this needs
      no bearer token/secret stored anywhere just to self-invoke; the function's own writes still go
      through the auto-injected service-role key, never this HTTP call's (lack of) auth. **Ships
      with the local-stack URL only** — the migration and `backend/README.md`'s "Scheduled jobs"
      section both say, loudly, that the URL must be updated via `cron.alter_job` to the real
      project address before/when this is ever applied remotely — not done here, per the task's own
      "never touch remote without asking."
    - **Tests**: `functions/fetch-news/*.test.ts` (50 Deno tests, no database needed — fixtures in
      `fixtures/`: valid RSS, valid Atom, a genuinely malformed feed, one with a long/nested/
      entity-laden HTML description) covering parsing (incl. image-source priority: enclosure >
      media:content > media:thumbnail; Atom `rel=alternate` link preference), excerpt truncation,
      de-dup (by link and by normalized title), the 200-per-topic trim, fetch timeout/size-limit
      handling, and — the task's own explicit ask — per-feed error isolation. `005_news_items_rls
    .test.sql` (pgTAP, 6 assertions) covers the new RLS shape: anon reads `news_items` but not
      `feed_status`; anon *and* admin writes to either are refused; `service_role` writes to both
      succeed.
    - **Full real verification, not just unit tests** — Docker was not available in this sandbox by
      default, but `brew install colima docker` + `colima start` got a real local Docker working, so
      this step's acceptance was actually run end-to-end rather than only argued for: `supabase
    start` applied both new migrations cleanly; `supabase test db` — **23/23 pgTAP assertions
      pass**; `deno test` — **50/50 pass**. Then, live: served `fetch-news` locally, pointed three
      `feed_sources` rows at this module's own fixture files (via a throwaway local HTTP server)
      plus the pre-existing seeded `example.com` ones (which 404, a free extra real-world error
      case) and one deliberately-broken URL — one real invocation produced `{fixture-rss: 3 items, fixture-atom:
    2 items, feed-1/feed-2/fixture-broken: failed independently}`, `mergedItemCount: 5`, and
      `news_items`/`feed_status` in Postgres matched exactly (verified via direct `psql` queries,
      correct topic tagging, correct excerpts, correct image presence). Re-running the identical
      request produced the same 5 rows (upsert dedup confirmed idempotent across runs, not just
      within one). Bulk-inserted 210 extra NEWS rows directly, re-invoked: **trimmed exactly 13**,
      landing NEWS at exactly 200 visible rows, MISSIONS's own 2 rows untouched (per-topic
      independence confirmed). `supabase db reset` afterward restored a clean state; `supabase test
    db` re-confirmed 23/23 passing on that clean state. The local Supabase stack (and the Colima
      Docker VM) were left running after this session, not torn down — stop them yourself
      (`supabase stop` / `colima stop`) if you don't need them.
    - Every acceptance item is met: "fills news_items merged, de-duplicated and trimmed" and "a
      broken feed leaves the others working" were both directly observed against fixture feeds on
      the local stack, not just reasoned about.
    - **2026-11-03 follow-up**: the real Google News feed was initially left out of `seed.sql`
      entirely (the task's own "do not hardcode real feed URLs" instruction) — the user then asked
      for it explicitly after not seeing any news locally, was shown it working live (102 real items
      fetched, 100 after dedup), and, after being asked to confirm the trade-off directly
      (`AskUserQuestion` — not assumed), chose to make it permanent. `seed.sql` now has
      `google-news-gta6` alongside the two `example.com` placeholders; re-verified against a fully
      clean `supabase db reset` (not just the already-running session): the real feed is present by
      default and one `fetch-news` run against the fresh seed produced 101 merged real items, with
      `supabase test db` still 23/23 afterward. **Still not verified, still not invented**: whether
      Google's terms of use actually permit this kind of scheduled, programmatic consumption, and
      whether a keyword search for "GTA 6"/"Grand Theft Auto VI" reliably stays on-topic (results
      already include at least one borderline/tangential item — an influencer-opinion piece, not
      official news) — both stay open decisions (`docs/ROADMAP.md`'s own "Open decisions" section's
      "Real news feeds" entry, unchanged in substance by this step, just no longer blocking a
      *local* demo). (2) the scheduled cron job itself (`pg_cron`/`pg_net` actually firing
      `fetch-news` on its own, unattended, every 30 minutes) was not separately observed running on
      a timer in this session — only the function's own HTTP invocation was; the SQL registering the
      schedule applied without error (confirmed via
      `supabase db reset`'s own migration log) but pg_cron's internal timer firing correctly was not
      additionally watched tick. (3) a cross-feed id-collision edge case is possible but unhandled:
      if the same story's link moves from one feed to another between runs, the upsert (`ON CONFLICT
    (link)`) changes that row's primary key (`id` is derived from `feed_id + link`) rather than
      creating a new row — harmless in practice (nothing currently references `news_items.id` via
      foreign key) but worth knowing about if a future client caches ids for its own delta-sync
      logic. (4) `feed_sources`' pre-existing seed rows (`feed-1`/`feed-2`, both `example.com`) were
      left untouched — they 404 against a real `fetch-news` run (confirmed above), which is exactly
      the harmless, expected behavior of a placeholder URL, not a bug.

- [x] **12B - news data layer** (core/data or feature/news) - needs: 12A, 7B - done: 2026-11-03
    - `news_items`/`feed_sources` now sync like every other content table: `core:model`'s
      `NewsItem` gained `updatedAt: Instant` (matching `FeedSource`'s existing field);
      `core:database`'s
      `NewsItems.sq`/`NewsDao` dropped the old device-local-cache design (`deleteOlderThanLatest`,
      per-topic `selectByTopic`) for a tombstone-based `selectAll`/`upsert`/`tombstone` shape
      identical to `FeedSources.sq`'s — the News screen's All/News/Missions filter is applied
      client-side on one merged, newest-first list rather than three separate DB queries, since the
      backend's own `fetch-news` job (step 12A) already trims each topic to 200 items.
      `core:network` got `NewsItemDto`/`NewsItemRemoteDataSource`/its `toChange()` mapper, built
      identically to the existing `FeedSource` ones, wired into `RemoteDataSourceFactory.kt`/
      `NetworkModule.kt`. `core:data` got `NewsRepository` (wraps `NewsDao`) and `ContentSyncerImpl`
      gained `syncFeedSources()`/`syncNewsItems()` (feed_sources was previously and deliberately
      left unsynced — see this step's own prior KDoc — now it's the two final tables in the sync
      order, after `map_pins`, since `news_items` references `feed_sources` by id). Verified:
      `core:model`/
      `core:database`/`core:network`/`core:data` all compile and their full `jvmTest` suites pass
      (including new `NewsDaoTest`/`NewsViewModelTest`-adjacent fakes for the sync harness).

- [x] **12C - feature:news** (feature/news) - needs: 12B, 1 - done: 2026-11-03
    - New `feature:news` module: `NewsViewModel` combines `NewsRepository.newsItems()` +
      `.feedSources()` (to resolve each item's `feedId` to a display name) +
      `ContentSyncer.syncState`
      (loading/offline/"updated Xm ago" text) into `core:design`'s pre-existing, preview-only
      `IloveViNewsUiState`/`IloveViNewsScreen` (built earlier as design-system scaffolding, never
      wired to real data until now). `NewsScreen` is the thin composable wrapper — the only place in
      this module that touches a platform API (`LocalUriHandler.openUri` to open an article in the
      system browser, per `IloveViNewsRow`'s own KDoc contract). Wired into `shared`:
      `AppKoinModules.kt`'s module list, `AppFeatureRegistrations.kt` (`NewsScreen()` replaces the
      old
      `NewsStubScreen()` registration for `AppDestination.Tab.News`), and the now-dead
      `NewsStubScreen` composable removed from `TabScreens.kt`. This is what fixes the
      "Presto disponibile nella sezione news" placeholder the user reported seeing — the News tab
      now shows real, synced `news_items` content. Verified: `feature:news:jvmTest` passes (new
      `NewsViewModelTest`/`NewsFormattingTest`), `shared:compileAndroidMain` succeeds with the real
      screen wired in, and the full `jvmTest` sweep across every touched module (`core:model`/
      `core:database`/`core:network`/`core:data`/`feature:catalog`/`feature:map`/
      `feature:news`) passes — the only failure seen was `feature:map`'s pre-existing
      `PinClustererTest` timing assertions, confirmed flaky under parallel test load (passes alone),
      unrelated to this step.

- [x] **12D - revert news to client-side fetching** (backend/core:network/core:data) - needs: 12C -
  done: 2026-11-03
    - **Reverts 12A/12B's "option B" (backend-merged `news_items` table)** back to the functional
      analysis's own original design (section 8/9.3's "option A": the app downloads each feed
      directly; `NewsItem` is a device-local cache — exactly what `feed_sources`' original migration
      comment, pre-12A, already said). The trigger: the user explicitly rejected the backend-table
      design ("I don't want to store any single news in the database, rather I want to fetch it from
      the RSS feed") after finding `news_items`/`feed_status`/`functions/fetch-news` had never been
      pushed to the linked remote project (`fgkoxqovgammjunlmhqn`) — deliberately, per 12A's own
      "never touch remote without asking" note — and so didn't exist there at all. 9.3 itself only
      ever proposed option B "to confirm"; it was never actually confirmed before 12A built it.
    - **Removed entirely**: `backend/supabase/functions/fetch-news/` (the whole Deno/TypeScript
      job), migrations `20261103120001_news_items_and_feed_status.sql` /
      `20261103120002_fetch_news_schedule.sql`, pgTAP test `005_news_items_rls.test.sql`,
      `config.toml`'s `[functions.fetch-news]` block — none of it was ever applied to a remote
      project, so deleting it is lossless. `backend/README.md`/`core/database/README.md` updated to
      match (news is no longer part of the backend at all). `feed_sources` itself is untouched — the
      feed *list*/config is still backend-synced exactly as before; only the fetched stories moved.
    - **New client-side pipeline**, all pure `commonMain` Kotlin (no third-party XML library — a
      hand-rolled permissive RSS/Atom-oriented XML tree parser, `core:data`'s
      `news/XmlDocument.kt`, was written instead, specifically to avoid vetting a KMP XML
      dependency's JVM/Android/iOS target support): `core:network` gained `NewsFeedFetcher`
      (replacing `NewsItemRemoteDataSource`/`NewsItemDto`/its mapper) — a plain Ktor `HttpClient`
      HTTPS GET with a 10s timeout and a 5MB cap (declared-`Content-Length` pre-check plus a
      streaming `readBuffer` cap, mirroring 12A's own `fetch.ts`), since a feed URL is an arbitrary
      third-party endpoint, never a Supabase table. `core:data` gained `news/FeedParser.kt`
      (RSS 2.0 + Atom → `ParsedFeedItem`; image priority enclosure > media:content > media:
      thumbnail; RFC 822 *and* ISO 8601 date parsing — `kotlin.time.Instant.parse` only covers the
      latter, so RFC 822 got its own small hand-rolled parser, including a Howard-Hinnant-algorithm
      civil-date→ epoch conversion, since there's no `java.time`/`NSDate` from `commonMain`),
      `news/HtmlExcerpt.kt`
      (~200-char word-boundary-truncated excerpt), and `news/FeedMerge.kt` (de-dup by link then
      normalized title; id is `"<feedId>|<link>"`, not a SHA-256 hash — this is now a purely local,
      single-writer cache, so a cryptographic hash buys nothing 12A's own reasoning needed it for).
      `ContentSyncerImpl.syncNewsItems()` (a `syncTable` call against a nonexistent table) became
      `syncNews()`: reads already-synced `feed_sources` locally, fetches+parses every enabled feed
      independently (one feed's network failure or malformed XML never blocks another — same
      property 12A's `orchestrate.ts` had), merges/de-dupes the result, upserts, and tombstones each
      topic back down to 200 rows (`NewsDao.trimNewsItems`, a new `NewsItems.sq` query) — all in one
      transaction. `NewsDao`'s existing `upsertNewsItem`/`tombstoneNewsItem` needed no changes at
      all: the table's storage shape (tombstone-based soft delete, `updatedAt`/`deletedAt`) was
      already right for this; only what populates it changed.
    - **A real bug found and fixed along the way**: before this revert,
      `NewsItemRemoteDataSourceImpl`
      was discovered to be querying Supabase's `feed_sources` table while deserializing the response
      as `NewsItemDto` — a copy-paste leftover from `FeedSourceRemoteDataSourceImpl` that 12B's own
      tests never caught (they sync through a fake data source, never real HTTP/JSON). This is now
      moot (the whole class is deleted), but is recorded here since it was a genuine, independently
      confirmed defect in 12B, not just a design disagreement.
    - A real Google News feed response surfaced a second, substantive parsing gap during this same
      investigation: Google News' RSS puts raw, un-escaped HTML straight into `<description>`
      (`<a href="...">Headline</a>&nbsp;<font>Source</font>`, no CDATA wrapper) rather than the
      CDATA-wrapped convention every fixture tested — on the backend parser this produced an empty
      excerpt silently (fixed there too, in-place, before the revert, via `fast-xml-parser`'s
      `stopNodes` option + `html.ts` entity-decoding it previously relied on the parser for). The
      new client-side `XmlDocument.allText()` handles both conventions uniformly with no
      special-casing at all — collecting all descendant text under a subtree already covers raw
      child elements and CDATA alike — which is strictly simpler than the workaround the backend
      version needed.
    - Verified: `core:data:jvmTest` (`FeedParserTest` — 18 cases including this exact Google News
      raw-HTML-description shape and a malformed-XML case; `FeedMergeTest`; full `ContentSyncerTest`
      suite unchanged and passing) and `core:network:jvmTest` (`NewsFeedFetcherTest` — success,
      non-2xx, declared-oversize, and streamed-oversize-with-no-honest-Content-Length cases) all
      pass. `core:database:jvmTest` (`NewsDaoTest`, incl. a new per-topic trim case) passes.
    - **Not done in this step**: no live network call against the real `google-news-gta6` URL was
      exercised end-to-end through the full `ContentSyncerImpl.syncNews()` path inside this sandbox
      (no outbound HTTPS to arbitrary third-party hosts from here) — `FeedParserTest`'s Google-News-
      shaped fixture and the standalone `NewsFeedFetcherTest`s (against `MockEngine`) are what's
      actually verified; wiring the two together against the live URL is unverified beyond that.

- [ ] **13A - feature:spotify** (feature/spotify) - needs: 7B, 1 - done:
    - Not started. No `feature:spotify` module exists.

- [ ] **13B - feature:settings** (feature/settings) - needs: 1 - done:
    - Not started. No `feature:settings` module exists.

- [ ] **14 - quality pass** (states, accessibility, localization, performance, privacy)
  (cross-cutting) - needs: 8, 9, 10, 11, 12C, 13A, 13B - done:
    - Not started (depends on the feature modules above existing first).

- [ ] **15 - store readiness** (androidApp / iosApp) - needs: 14 - done:
    - Not started.

- [ ] **16 - content entry after the game release** (backend / content) - needs: 3, 12A - done:
    - Not started. Blocked until GTA VI ships (19 Nov 2026) — real content can't be entered before
      then; see "Content after release" below.

---

## Open decisions

- **Map image rights**: no official or fan-made GTA VI map image may ever be embedded, downloaded,
  or re-hosted by this project (`tools/tilegen/README.md`, `core/mapview/README.md` are both
  explicit about this). What map image will actually ship in the tile pyramid, and who owns/licenses
  it, is still undecided.
- **Real news feeds**: `feed_sources` exists and is fetched client-side now (step 12D reverted
  12A/12B's backend-table design) — one real feed is seeded (`google-news-gta6`, a Google News RSS
  keyword search, added 2026-11-03 at the user's explicit request). Still undecided: whether
  Google's terms of use actually permit this kind of scheduled/programmatic consumption, whether
  this specific feed's results stay reliably on-topic (a keyword search can and does surface
  tangential stories), and which *other* outlets/feeds are trustworthy/approved sources, plus how
  editorial verification ("leak tracker")
  works. 12B/12C (done 2026-11-03) are built and don't depend on this being resolved — the News
  screen shows whatever `feed_sources` is configured with, today just the one Google News feed; this
  decision only gates adding more feeds.
- **Spotify access and limits**: `feature:spotify` (13A) needs a decision on which Spotify API
  access tier is available (rate limits, whether a backend proxy is needed for secrets), and what
  happens for users without a Spotify account/Premium.
- **Final app name**: repo and docs currently use "ILoveVI" / `ilovevi.com` throughout — confirm
  this is the final shipping name before store listings (step 15) are prepared.
- **Real GTA VI item categories**: `core/model`'s `WeaponType` (5 values) and `VehicleType` (5
  values) are a reasonable placeholder guess, not sourced from real game data — unavailable before
  release. Item types are placeholders (AMMO, ARMOR — HEALTH was removed in step H, since GTA VI has
  no health-pickup item): confirm the real item classification of GTA VI when known. Expect this
  enum set to need rework in step 16, and possibly again once real categories are known, which also
  means step I's per-sub-type icon set may need redrawing after launch.
- **Cluster threshold**: no decision yet on how many overlapping map pins should merge into one
  cluster bubble before `feature:map` (step 11) implements clustering against step I's
  `ClusterBadge`
  — a threshold of 10 has been floated but isn't confirmed.
- **Mixed-cluster color**: step I's `ClusterBadge` uses `brand-purple` for a mixed-category cluster
  (the only brand accent not already claimed by Weapons/Vehicles/Items) — not a confirmed design
  decision, since the design system's own README calls `brand-purple` "decorative elements only".

## Content after release (19 Nov 2026)

Real weapons/vehicles/items/map/radio content cannot be collected before the game ships. Per
`backend/README.md`, the plan is a spreadsheet import into the schema from step 3, done separately
from schema work — this is step 16 above. Until then, `backend/supabase/seed.sql` only contains a
small set of obviously-fake placeholder rows (`[Weapon 1]`, `[Station 1]`, …) for tests/previews.

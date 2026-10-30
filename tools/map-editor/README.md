# :tools:map-editor

The internal admin pin-placement tool, rebuilt against the **Map Editor Functional Analysis**
(`documentation/ilovevi-design-system 2/IloveVI-MapEditor-Functional-Analysis.pdf`, EDT-01..81):
a desktop (JVM) Compose app for placing pins on the game map, creating their catalog entries
(weapons, vehicles, items) inline, and saving everything to the database with one Commit button.
**Not part of the mobile app** — never shipped to players, used by maintainers only, the same way
`:tools:tilegen` is.

## Mock vs. real data: `MAP_EDITOR_BACKEND`

`di/AppModule.kt` binds either the **fake** implementations (`data/fake/`, the default — no
environment variables needed) or the **real** Supabase-backed ones (`data/supabase/`, from a
previous session: migrations, the `commit_map_edits` RPC, `EntryRepository`/`MapPinRepository`/
`SupabasePostgrestCommitRepository`/`AdminAuthRepository`), picked once at startup by
`di/MapEditorBackend.fromEnvironment()`:

```sh
# Fake data (default) — no env vars needed.
./gradlew :tools:map-editor:run

# Real Supabase project.
MAP_EDITOR_BACKEND=supabase SUPABASE_URL=... SUPABASE_ANON_KEY=... ./gradlew :tools:map-editor:run
```

`SupabaseConfig`/`MapEditorBackend` both read real process environment variables, never a file in
this repo — but the root **`local.properties`** (already used for `SUPABASE_URL`/
`SUPABASE_ANON_KEY`, gitignored, never committed) is a convenient place to keep them for local dev:
`tools/map-editor/build.gradle.kts` reads that file at build time and forwards
`SUPABASE_URL`/`SUPABASE_ANON_KEY`/`MAP_EDITOR_BACKEND` into every `JavaExec` task's environment
(covers both `./gradlew :tools:map-editor:run` and the IDE's own gutter "run" action), so

```properties
# local.properties
SUPABASE_URL = https://xxxx.supabase.co
SUPABASE_ANON_KEY = xxxx
MAP_EDITOR_BACKEND = supabase
```

is equivalent to exporting all three — no need to type them on every command line. Explicit env vars
on the command line still work too and aren't overridden by this.

`EntryRepository`/`MapConfigRepository` (`data/supabase/`) already matched the interfaces exactly,
so they implement them directly. `PinRepository`/`AuthService`/`CommitService` needed a small
adapter — see `data/supabase/SupabasePinRepositoryAdapter.kt`,
`data/supabase/auth/SupabaseAuthService.kt` and `data/supabase/SupabaseCommitServiceAdapter.kt`,
each with its own KDoc on exactly what it translates and why it couldn't just wrap the old class
unchanged (the short version: shape differences — `MapPin` vs. the thinner read-only `DbPin`, an
`imagesUploaded` count the RPC never reports back, and the non-admin sign-in behavior EDT-02 wants
that the old `AdminAuthRepository`/`AdminSignInResult` deliberately doesn't give).

## Dependencies

`:core:model` (`MapPin`/`NormalizedPoint`/`MapConfig`/category & sub-type enums), `:core:mapview`
(`MapViewer`, tiles or single-image/offline mode), `:core:design` (every visual element — chips,
buttons, the map pin, the new `IloveViTextField`/`IloveViSegmentedSwitch`/`IloveViRailButton`/
`IloveViEmptyStateCard` added for this step — nothing is redrawn locally) and **supabase-kt**
(`auth-kt` + `postgrest-kt` + `storage-kt`), used by the `data.supabase` implementations whenever
`MAP_EDITOR_BACKEND=supabase` selects them (see "Mock vs. real data" above).

Like `:tools:tilegen`, this is a **plain JVM module**, not a KMP one.

## Running it

```sh
./gradlew :tools:map-editor:run
```

No environment variables needed for the default, fake backend. Sign in with `admin@example.com`
(any password) for an admin session, or any other well-formed email/password for the non-admin
screenshot's state. For the real Supabase backend, see "Mock vs. real data" above — sign-in there is
a real email/password against your project's Auth, and admin status comes from that user's
`app_metadata.role` claim, not a hardcoded email.

## Loading a map from the tool (PLT-08)

From the empty state ("No map loaded"), press **"Load map image..."** and pick a PNG/JPEG map image
(at most 16384px per side, `data/MapPublishPlanning.kt`'s `MAX_DIMENSION_PX`) — the tool does
everything else, with no commands or SQL to run by hand:

1. **Validate** — format, readability, file size, dimension limit; the dialog shows the image size
   and a preview, or a plain-words reason it can't proceed.
2. **Compute** — tile size 256, `min_zoom`/`max_zoom`/tile count from the image's own width and
   height (reusing `core:model.map.TileScheme`'s math, never a second implementation of it): a
   4096x4096 image reaches zoom 4. Non-square images keep their own width/height — `TileScheme` and
   `core:mapview`'s `MapViewport` already handle independent width/height end to end, so nothing is
   padded to a square canvas.
3. **Generate** — the tile pyramid, in a temporary folder, via `:tools:tilegen`'s own
   `TilePyramidGenerator` (shared, not duplicated).
4. **Upload** — tiles and the source image to the `map-tiles` Storage bucket under
   `v<version>/...`, through the signed-in admin's own session (`RemoteTileStorage`/
   `SupabaseRemoteTileStorage` — never the service-role key), 4 files at a time, retried with
   backoff, skipping anything a previous attempt already uploaded — so **Retry resumes** instead of
   re-uploading everything.
5. **Verify** — tile `0/0/0` plus a sample of random tiles must actually respond `200` at their
   public URL before anything else happens.
6. **Register** — only now is `map_config` written. If any earlier step fails, `map_config` is left
   exactly as it was and the dialog shows what already succeeded, with a **Retry** that resumes. If
   no pin is on the current map's version yet (the broken-v1 case), that row is updated in place;
   otherwise a new version row is inserted and the dialog says how many existing pins are now on an
   older version and need repositioning (every pin remembers which map version it was placed on).
7. **Reload** — the editor shows the newly published map immediately.

The header's **Map** menu always has **"Replace map..."** (same flow) and **"Map info"** (version,
size, zoom levels, and the exact `tile_url_template` — for reference only, never something to type
by hand). Only a signed-in admin can confirm a publish; anyone else sees why the button is disabled,
same gate `isCommittable` already uses elsewhere in this tool.

**How to check the result**: open **Map info** afterward and confirm version/size/zoom levels match
the image just loaded; the map should render at every zoom immediately, no manual reload needed. If
"Loading..." doesn't resolve within 10s, the empty state shows the actual reason (no map configured,
a tile returning a non-200 status, or the timeout itself) together with the exact address that was
requested, instead of spinning forever.

## Architecture

- **`ui/`** (Compose) — `MapEditorApp.kt` (sign-in → non-admin notice → editor), `Header.kt`/
  `ToolRail.kt`/`CategoryRow.kt`/`MapArea.kt`/`RightPanel.kt` (`InspectorPanel.kt`+`DraftPanel.kt`)/
  `StatusBar.kt` matching the Functional Analysis's window schematic (§4) exactly, plus
  `CommitDialog.kt` and `SignInScreen.kt`. No business logic lives here — every composable reads one
  `EditorUiState` snapshot and calls plain functions on `EditorState`.
- **`state/EditorState.kt`** — one immutable `EditorUiState` (`StateFlow<EditorUiState>`) plus
  intent functions (`placeAt`, `moveSelectedTo`, `deleteSelected`, `undo`/`redo`, `linkExisting`,
  `stageEntry`, `commit`, ...). **Zero `androidx.compose.runtime` imports** — a plain `kotlin.test`
  function constructs one, calls an intent, and asserts on `state.value`, no Compose runtime or test
  rule involved at all (`state/EditorStateTest.kt`, 17 tests). The UI bridges it into Compose with
  the standard `collectAsState()`.
- **`data/`** — interfaces only: `EntryRepository`, `PinRepository`, `MapConfigRepository`,
  `AuthService`, `CommitService`, `DraftStore`, plus the Compose-free `CommitPayload`/
  `CommitEntry`/`CommitPin`/`CommitResult`/`CommitPayloadBuilder` (`data/Commit.kt`). Every
  interface is bound to a **fake** by default; `data/fake/` holds all of them (`FakeEntryRepository`
  — 8 fixed entries, `FakePinRepository` — 15 fixed database pins, `FakeMapConfigRepository` — one
  "Map v1 4096x4096", `FakeAuthService`, `FakeCommitService` — succeeds after ~1.5s or throws when
  `forceFailure = true`, `InMemoryDraftStore`). `draft/FileDraftStore.kt` is the one *real*
  implementation kept outside `data.supabase` — plain local-file JSON persistence has nothing to do
  with Supabase, so it's wired by default even though every network-facing service is fake.
- **`model/`** — `DraftPin`/`DraftEntry`/`EntrySource` (the draft's own domain types — see "Naming"
  below for why `EntrySource`, not `EntryRef`), `EditMode`, `DbPin`, `DraftSnapshot`,
  `EntryFieldSpec`/`EntryFields` (the one real-column-driven definition of each category's *extra*
  fields — see "Entry fields" below), `Session`/`SignInResult` (this step's sign-in model — see
  "Naming"), `EntrySummary` (kept from the previous session, the read shape for "entries of a
  category"), `EntryCategories.kt` (`EntryCategory.subTypes()`).
- **`data.supabase/`** — the real, Supabase-backed implementations from the previous session, now
  wired behind `MAP_EDITOR_BACKEND=supabase` (see "Mock vs. real data" above):
  `EntryRepository`/`MapConfigRepository` implement this step's interfaces directly (their own
  `loadAll()`/`loadCurrent()` already matched exactly); `MapPinRepository`,
  `CommitRepository`/`SupabasePostgrestCommitRepository` and `auth/AdminAuthRepository` are
  unchanged but sit behind a small adapter each —
  `SupabasePinRepositoryAdapter`/`SupabaseCommitServiceAdapter`/`auth/SupabaseAuthService` — since
  their shapes genuinely differ from this step's interfaces (see "Where the real services plug in"
  below). Their own `CommitPayload`/`CommitEntry`/`CommitPin`/`CommitResult` types were kept (not
  deleted) but their old `CommitPayloadBuilder` was dropped — it built from the previous session's
  `StagedPin`/`StagedEntry`, which no longer exist, replaced by this step's `DraftPin`/`DraftEntry`
  and a new builder in `data/Commit.kt`.
- **`importer/`, `offline/`, `model/PinRow.kt`** — kept, **unwired**: the Functional Analysis's
  scope (§1) never mentions bulk CSV/JSON import or export, and the new window has no such button.
  Still valid code, just not reachable from this UI.

## Naming decisions (flagged, not silent)

- **`EntrySource`, not `EntryRef`.** The Functional Analysis's `EntryRef = Db(id) | Draft(id)`
  means "where does this id live" — a different concept from `core:model.EntryRef` (`ToWeapon`/
  `ToVehicle`/`ToItem`, "which entity type"). Reusing the same name for both in one codebase would
  conflate two unrelated ideas, so this one is `model/EntrySource.kt`.
- **`Session`/`SignInResult`, not `AdminSession`/`AdminSignInResult`.** EDT-02 explicitly wants a
  non-admin user to stay signed in (`isAdmin = false`, Commit just disabled) — the older
  `AdminSignInResult` (still used, untouched, by the real `data.supabase.auth.AdminAuthRepository`)
  signs a non-admin out locally and treats it as a sign-in *failure*, a genuinely different
  behavior, not just a naming difference. `model/Session.kt` is the new, separate model this step's
  `AuthService` uses.
- **`v<version>/...` object paths, not `<version>/...`.** `SupabaseMapPublisher` uploads under
  `v1/`, `v2/`, ... (per the Functional Analysis's own literal `tile_url_template` example) —
  `:tools:tilegen`'s own CLI/`SupabaseAdminClient` uploads under `1/`, `2/`, ... (no `v` prefix).
  Both are internally consistent (each builds its own `tile_url_template` to match its own upload
  paths, covered by
  `SupabaseMapPublisherTest.tileUrlTemplate_resolvesToTheSamePathsObjectsAreUploadedUnder`), but a
  map published by one tool's object paths is not addressed the same way by the other's template —
  flagging this rather than quietly picking one and calling it "the" convention.

## Entry fields: what's real vs. the prototype's placeholders

The Functional Analysis's own open question #1 asks this directly and proposes reading it from the
real migrations. Per `backend/supabase/migrations/20261011120002_weapons_vehicles_items.sql`:
**only weapons have category-specific columns** (`price`, `ammo_type`). Vehicles and items have no
equivalent — not "price and seats for vehicles" as the interactive prototype apparently shows, just
the generic `attributes jsonb` column (which this form doesn't expose at all — the Functional
Analysis's field list for the new-entry form never asks for a free-form attributes editor, only
"the extra fields of the category"). `model/EntryFieldSpec.kt`'s `EntryFields.extraFieldsFor`
reflects this exactly: `[price, ammoType]` for weapons, `[]` for vehicles/items. `name`/`subType`/
`zone`/`description` are dedicated `DraftEntry` properties (not part of this spec), per the
Functional Analysis's own literal field list (EDT-54).

## Pin rendering: why two different approaches

§8 asks for smooth pan/zoom/placing with 500 draft pins and 2000 database pins, "draw pins in one
canvas layer." `ui/MapArea.kt` treats the two pin sets differently, on purpose:

- **Draft pins** (≤500, interactive — click, drag, multi-select) go through `core:mapview`'s
  `MapViewer`'s existing `items`/`pinContent` mechanism, which already gets click/drag/pan
  coexistence right (proven in the previous session). Re-engineering that into an external
  canvas-plus-custom-hit-test path would be a real `core:mapview` gesture-internals change for a pin
  count Compose Desktop should handle fine as plain nodes.
- **Database pins** (≤2000, read-only, EDT-11: "not clickable") draw in a single `Canvas` layer with
  **no** `pointerInput` at all — a layer with no pointer input never competes with
  `MapViewer`'s own gestures, so this needed no special coordination, and it's exactly where a naive
  one-node-per-pin approach would cost the most. Positions are computed every frame from
  `MapViewport.toScreen` (public, `core:model.map`) and `MapViewerState.center`/`zoom` (public
  getters) — `core:mapview`'s own `hitTestPin` isn't needed here since nothing is clickable.

**What "report measured frame times" couldn't be**: this sandbox has no attached display (the tool
does launch and run without exceptions — confirmed via `./gradlew :tools:map-editor:run`, over 35s
with no crash — but nothing was ever actually clicked or watched rendering). `state/
EditorStatePerformanceTest.kt` measures the pure data-layer cost instead (placing 500 pins with full
undo history, filtering/positioning 2000 database pins) — a structural stand-in, not a frame budget.
A real frame-time measurement needs a real display and is a follow-up.

## Where the real services plug in

`di/AppModule.kt` branches on `MapEditorBackend.fromEnvironment()` (`di/MapEditorBackend.kt`) and
binds one full set of interfaces or the other — see the module there for the exact `single<X> { }`
lines. `EntryRepository`/`MapConfigRepository` are a direct 1:1 swap (their real classes implement
the interfaces directly, no adapter). The other three needed a small adapter, each because the real
class's shape genuinely differs from what this step's interface wants, not just a naming difference:

- **`PinRepository`** — `SupabasePinRepositoryAdapter` maps `MapPinRepository.loadPins()`'s
  `core:model.MapPin` (carries an `EntryRef`, `mapVersion`, `updatedAt` — everything a *draft*
  pin's eventual database row needs) down to this step's much thinner read-only [`DbPin`]
  (just `id`/`category`/`entryId`/`position`/`zone`, enough to draw the EDT-11 overlay).
- **`AuthService`** — `auth/SupabaseAuthService` does its own sign-in against `Auth` rather than
  wrapping `AdminAuthRepository`: EDT-02 wants a non-admin user to *stay* signed in
  (`Session(email, isAdmin = false)`, only Commit disabled), while `AdminAuthRepository` signs a
  non-admin out locally and reports it as a sign-in failure (`AdminSignInResult.NotAnAdmin`) — see
  "Naming decisions" below for why that's a real behavior difference, not just a type rename.
- **`CommitService`** — `SupabaseCommitServiceAdapter` translates between this step's
  `data.CommitPayload` (entries carry `price`/`ammoType` directly) and
  `data.supabase.CommitPayload` (a generic `attributes` map) that
  `SupabasePostgrestCommitRepository`/the `commit_map_edits` RPC expect, and fills in
  `CommitResult.imagesUploaded` from `pendingImages.size` since the RPC's own return value never
  reports that count (every pending image is uploaded *before* the RPC call, so a successful return
  means they all made it).

## Tests

`./gradlew :tools:map-editor:test` — 75 tests: 74 pure `kotlin.test` (no live network; `MockEngine`
stands in for the two HTTP calls that need one) plus one Compose UI test exercising the real
composables end to end:

- `data/supabase/SupabasePinRepositoryAdapterTest.kt` (1),
  `data/supabase/SupabaseCommitServiceAdapterTest.kt`
  (1) — the two adapters' pure mapping logic (`MapPin` → `DbPin`, `data.CommitPayload` →
  `data.supabase.CommitPayload`), called directly without a real `SupabaseClient`.
- `data/MapPublishPlanningTest.kt` (12, PLT-08) — `validate`/`validateDimensions` (unsupported
  extension, oversized file, oversized dimensions, the valid case), `decideVersion`'s three outcomes
  (no config yet, no pins on the current version → update in place, pins on the current version →
  bump + warn how many must move), and `buildPlan`'s zoom/tile-count math for a 4096x4096, a 256x256
  and a non-square 1000x700 image (confirming no square-padding happens).
- `data/fake/FakeMapPublisherTest.kt` (3, PLT-08) — a forced failure at `UPLOAD` or `VERIFY` never
  touches `map_config`; only a fully successful `publish()` updates it.
- `data/supabase/SupabaseMapPublisherTest.kt` (3, PLT-08) — the `tile_url_template` built for a
  version resolves to exactly the object paths tiles are uploaded under; `source.<ext>`'s path
  defaults the extension to `png`; and a retried `publish()` (same plan, same publisher instance)
  never re-uploads a path that already succeeded, only the one that previously failed — using a
  hand-written `FakeRemoteTileStorage` (the 3-method `RemoteTileStorage` seam, not supabase-kt's
  full `BucketApi`) plus a `MockEngine` that always 404s the verify step, so the test never needs a
  real `SupabaseClient` network call.
- `state/EditorStateMapLoadTest.kt` (4, PLT-08) — `EditorState.load()`'s fix for the "No map
  loaded - Loading..." forever bug: no `map_config` yet, a tile-check failure (with the exact
  requested URL preserved), an exception while loading, and a tile check that never resolves — all
  four leave `isLoading = false` with a plain-words `mapLoadError` instead of hanging, the last one
  exercising the real 10s timeout via `runTest`'s virtual time (no test actually waits 10 seconds).

- `state/EditorStateTest.kt` (17) — place (every click adds a new incomplete pin), placing with the
  active category/sub-type or an active entry, commit-gating (`isCommittable`: changes + every pin
  complete + admin), move, select-within-a-rectangle, delete (+ pruning an unused draft entry,
  EDT-41), `setCategoryForPin` only applying to an entry-less pin (EDT-57), linking an existing
  entry and pruning the draft entry it replaced (EDT-58), the duplicate guard, two pins sharing one
  staged entry, undo/redo across 55 operations (the 50-step cap exercised precisely), two-click
  discard, and Commit's three outcomes (success clears the draft, failure keeps it intact, refusing
  when not committable without ever calling the service).
- `draft/FileDraftStoreTest.kt` (5) — save/load round-trip (including a pending image's path and
  `attributes`), a damaged file loading empty while keeping a `.corrupt-<timestamp>` copy of the
  original bytes, and `clear()`.
- `data/CommitPayloadBuilderTest.kt` (7) — the RPC payload: map version stamped on every pin, a pin
  resolving to its linked existing or staged entry, an incomplete/dangling pin dropped rather than
  sent malformed, every weapon column mapped (including price/ammo type), vehicles/items correctly
  leaving those `null`, and `FakeCommitService` recording what it was called with.
- `model/EntryFieldSpecTest.kt` (6) — `extraFieldsFor`/`validateExtra` match the real migration
  columns exactly (see "Entry fields" above), and `EntryCategory.subTypes()` matches the real
  `WeaponType`/`VehicleType`/`ItemType` enums.
- `state/EditorStatePerformanceTest.kt` (2) — see "Pin rendering" above.
- `geometry/EditorGeometryTest.kt` (4), `data/supabase/MapPinDtoTest.kt` (3, re-verifying the
  untouched real DTO mapping still works), `importer/ImportValidatorTest.kt` (6, the kept-but-
  unwired bulk import code) — all pre-existing, still green.
- `ui/MapEditorAppUiTest.kt` (1) — the acceptance's explicit UI test, driving the real
  `MapEditorApp`/`EditorState` through Compose's desktop test harness (`createComposeRule`, no
  mocks): sign in as admin, place a pin, switch to "New entry," type a name, create the entry, open
  Commit, confirm, and assert the success dialog. This is the one place this module found a real bug
  rather than a documentation gap: the Inspector's "New entry" form (name, sub-type, zone,
  description, extra fields, image picker, duplicate warning, Create/Delete buttons) is taller than
  the panel's visible height on a 1280x800 window, and `InspectorPanel`'s root
  `Column` had no scroll — so anything past the fold had a degenerate, unclickable layout node.
  Fixed by adding `Modifier.verticalScroll(rememberScrollState())`; the test scrolls to the button
  first (`performScrollTo()`) the same way a real drag-to-scroll would.

`@Preview`s (all using the fakes directly, `data/fake/*`): the empty draft, a draft with an
incomplete pin, the new-entry form, the existing-entry picker, each of the 4 Commit dialog states,
the sign-in dialog, the non-admin notice — plus the new `core:design` components' own previews
(`MapPin.kt`, `Button.kt`, `TextField.kt`, `SegmentedSwitch.kt`, `RailButton.kt`,
`EmptyStateCard.kt`).

## Other behavior fixes from this pass

- **Autosave is actually debounced now.** `EditorState.persist()` used to write `draft.json`
  synchronously on every single intent call — harmless for a click, but a pin drag calls
  `moveSelectedTo` on every pointer-move frame, which would have hit disk dozens of times a second.
  `persist()` now cancels/relaunches a 400ms-delayed write (`autosaveJob`); `flushAutosave()`
  writes immediately and is called from `Main.kt`'s `onCloseRequest` so the last edit before closing
  the window is never dropped by the debounce window.
- **Undo/Redo are now actually disabled when empty**, not just dimmed. `IloveViRailButton`
  (`core:design`) gained an `enabled` parameter (it had none — Undo/Redo were always clickable, just
  drawn with a fainter icon); `ToolRail` now passes `enabled = canUndo`/`canRedo`.
- **Database pins are dimmed, not just outlined** (EDT-11 asks for both) — `MapArea`'s read-only pin
  `Canvas` layer now draws them at 50% alpha on top of the existing stroke-only style.

## What wasn't verified here

Same sandboxed, display-less environment every step since 3 has noted — no physical display, no
human ever watched this render. What *is* new this pass: the Compose UI test
(`ui/MapEditorAppUiTest.kt`) actually renders the real composable tree headlessly via Skiko and
drives it with synthesized mouse/keyboard input, which is how the Inspector-scrolling bug above was
actually found — that's a meaningfully stronger signal than "it compiles," even without a human
look. `./gradlew :tools:map-editor:run` launches with **no exception** and keeps running with no
crash. Still unconfirmed by a human: pixel-accurate layout against the screenshots, real
mouse-drag-vs-map-pan interaction, keyboard-shortcut feel, and the `@Preview`s actually rendering
correctly in an IDE.

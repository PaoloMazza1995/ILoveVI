# :tools:map-editor

The internal admin pin-placement tool (step 5B, Functional Analysis v0.3 PLT-01..08, section 9.5):
a desktop (JVM) Compose app for placing `MapPin`s on the map. **Not part of the mobile app** — never
shipped to players, used by maintainers only, the same way `:tools:tilegen` is.

## Dependencies

`:core:model` (`MapPin`/`EntryRef`/`NormalizedPoint`/`MapConfig`), `:core:mapview` (`MapViewer`,
single-image mode for offline "Open image"), `:core:design` (`IloveViTheme`/`IloveViMapPin` — tokens
used where convenient, no new visual design per this step's spec), `:tools:tilegen` (the
"New map version" pipeline, called in-process) and **supabase-kt** (`auth-kt` + `postgrest-kt`).

**Why supabase-kt here, unlike `:tools:tilegen`'s raw Ktor calls**: this tool needs real
email/password sign-in with session/JWT lifetime management, plus authenticated multi-table
PostgREST reads/upserts — the SDK earns its keep for that surface. `:tools:tilegen`'s 3 plain REST
calls (always with the service-role key, no session) didn't need it; see that module's own README.
Both styles coexist in this repo on purpose.

Like `:tools:tilegen`, this is a **plain JVM module**, not a KMP one — `build_logic`'s convention
plugins configure Android/iOS targets this module has no use for, so it applies
`org.jetbrains.kotlin.jvm` + `org.jetbrains.compose` (Compose *Desktop*, not Multiplatform's
Android/iOS targets) + `application` directly, the same shape `:tools:tilegen` uses minus the
Compose bits.

## Running it

```sh
export SUPABASE_URL=http://127.0.0.1:54321
export SUPABASE_ANON_KEY=...        # the anon/publishable key `supabase start` prints
./gradlew :tools:map-editor:run
```

"New map version" additionally needs `SUPABASE_SERVICE_ROLE_KEY` (same variable
`:tools:tilegen`'s CLI reads) — registering a `map_config` row has always been a service-role
operation in this repo, not something this tool's admin session is used for.

No credentials are ever read from a file in this repo, same convention as `:tools:tilegen`.

## What each requirement maps to

- **PLT-01 (sign-in)**: `SignInScreen.kt` + `auth/AdminAuthRepository.kt`. Real GoTrue
  email/password sign-in; afterwards, the signed-in user's `app_metadata.role` claim (the same one
  `backend/supabase/migrations/20261011120001_extensions_and_helpers.sql`'s `is_admin()` checks) is
  read client-side for a specific "you're signed in but not an admin" message — a courtesy message
  only, not the real enforcement: every write still goes through RLS regardless. The session
  (`AdminSession`) lives in memory for the run only; the password itself is never retained past the
  sign-in call.
- **PLT-02 (load map)**: `data/MapConfigRepository.kt` loads the highest `map_version`; shown via
  `MapViewer` (`MapContent.Tiles`).
- **PLT-03 (entry list)**: `EntryListPanel` in `ui/EditorScreen.kt`, filtered by `EntryFilter`
  (`ALL`/`NO_PIN`/category) and a search field. Selecting a row arms it (`EditorState.armEntry`).
- **PLT-04 (place/move/drag/nudge/delete)**: a map click with an armed entry calls
  `EditorState.placeOrMoveArmedPinAt` (creates or moves that entry's pin — the normalized-point
  conversion is `core:mapview`'s own `MapViewer.onMapTap`, exercised directly in
  `geometry/EditorGeometryTest.kt`). Dragging an existing pin's marker and arrow-key nudging
  (Shift = 10px steps, see `geometry/EditorGeometry.kt`'s `movePinByScreenDelta`) both land in
  `EditorState.moveSelectedPin`. Delete soft-deletes (`deleted_at`, never a real `DELETE`) via
  `MapPinRepository.softDelete`. x/y are shown to 4 decimals in the pin list.
- **PLT-05 (pin list)**: `PinListPanel`, with pins on an older `map_version` than the one currently
  loaded flagged "(to reposition)" — see `EditorState.pinsToReposition`.
- **PLT-06 (import/export)**: `importer/PinCsvIo.kt` / `PinJsonIo.kt` (columns: `category,
  entry_id, x, y, zone`), validated by `importer/ImportValidator.kt` (unknown entry, out-of-range
  x/y, duplicates) before `ui/Dialogs.kt`'s `ImportReportDialog` asks for confirmation — nothing is
  written until that confirmation.
- **PLT-07 (save/unsaved changes)**: `EditorState.save()` upserts every dirty pin and soft-deletes
  every removed one, in one Postgrest `upsert` call keyed by the `(category, entry_id)` unique
  constraint, not `map_pins.id` (import rows don't carry one). `EditorState.hasUnsavedChanges`
  drives the "Save*" toolbar label and the exit-time `UnsavedChangesDialog` (`Main.kt`'s
  `onCloseRequest`).
- **PLT-08 (new map version)**: `ui/NewMapVersionDialog.kt` calls `:tools:tilegen`'s
  `TileGenPipeline.run(...)` in-process (no shelling out) with `upload = true`; on success,
  `EditorState.applyNewMapConfig` switches the viewer to the new version and re-flags every pin not
  on it as "to reposition" — existing pins are **never** moved or revalidated automatically, the
  same rule `:tools:tilegen`'s own README states.
- **Section 9.5 / PLT-09 (offline mode)**: `ui/MapEditorApp.kt`'s "Work offline from a local image"
  path calls `core:mapview`'s `loadSingleImageMapContentFromFile` (step 5A-bis) — no tiles, no
  network. Pins go to `offline/OfflinePinStore.kt`, one local JSON file next to the chosen image,
  same shape as the JSON export plus `map_version`. The "Offline: not connected to the backend"
  banner (`ui/EditorScreen.kt`'s `Toolbar`) stays up the whole time;
  `EditorState.publishOfflinePins`
  (reusing the exact same validate → confirm → upsert path as a CSV/JSON import) refuses to run
  until `signIn` has actually succeeded.

## Tests

`./gradlew :tools:map-editor:test` — 13 tests, all pure `kotlin.test` (no mocked network, no Compose
UI test harness, same as `:tools:tilegen`):

- `geometry/EditorGeometryTest.kt` — click position → normalized point at several zooms and window
  sizes (matches `core:model`'s own `MapViewport.toNormalized` exactly, since that's what this
  tool's click handling is); pin-drag/nudge delta math, including the 0f..1f clamp and that a fixed
  pixel delta moves the pin less at a higher zoom.
- `importer/ImportValidatorTest.kt` — unknown category, unknown entry, out-of-range x/y, duplicate
  rows, and a 300-row import (the acceptance check) all validate correctly.
- `data/MapPinDtoTest.kt` — `MapPin` → upsert-DTO mapping (every column present, `deleted_at`
  always explicitly `null` so reactivating a soft-deleted pin on re-import actually works — see that
  file's KDoc for why a *required*, non-defaulted field matters here) and the reverse (backend row →
  `MapPin`), plus `pinRowId`'s determinism.

## Manual test script (needs the local Supabase stack + the placeholder map)

Nothing above exercises a real backend — `supabase`/Docker aren't available in the environment this
step was built in (same limitation steps 3/H have). Run this by hand once Docker is available, to
cover the acceptance checks a unit test can't:

```sh
# 1. Start the local stack and get the anon key it prints.
cd backend && supabase start
export SUPABASE_URL=http://127.0.0.1:54321
export SUPABASE_ANON_KEY=<printed anon key>
export SUPABASE_SERVICE_ROLE_KEY=<printed service_role key>

# 2. Make an admin user (see backend/README.md's "Creating the first admin user"), then generate
#    and register a placeholder map so there's a map_config row to load:
./gradlew :tools:tilegen:run --args="generate-placeholder --out /tmp/ilovevi-map.png"
./gradlew :tools:tilegen:run --args="--source /tmp/ilovevi-map.png --out /tmp/ilovevi-tiles --upload"

# 3. Launch the editor, sign in as that admin.
./gradlew :tools:map-editor:run
```

Then, in the running tool:

1. **Place and reopen**: pick any entry, click the map to place its pin, note the x/y shown (4
   decimals), click **Save**. Quit and relaunch the tool, sign in again: the same entry's pin must
   reopen at the **same** x/y — confirms the round trip through `upsert`/`select` and the DTO
   mapping didn't lose precision.
   ```sh
   # Cross-check directly against Postgres while the tool is still running, or after:
   curl "$SUPABASE_URL/rest/v1/map_pins?select=category,entry_id,x,y&entry_id=eq.<id>" \
     -H "apikey: $SUPABASE_ANON_KEY" -H "Authorization: Bearer $SUPABASE_ANON_KEY"
   ```
2. **Drag, nudge, delete**: drag the pin a short distance; select it and nudge with arrow keys (hold
   Shift for the larger step); press Delete and confirm it disappears from the pin list, then
   confirm in Postgres that the row still exists with `deleted_at` set (not actually removed):
   ```sh
   curl "$SUPABASE_URL/rest/v1/map_pins?select=id,deleted_at&entry_id=eq.<id>" \
     -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY"
   ```
3. **Import 300 rows**: export the current pins to CSV, duplicate/edit it (a spreadsheet or a
   one-line script) up to 300 rows against real entry ids from `seed.sql` or your own data,
   re-import, confirm the report shows 300 valid rows and no issues, confirm, and check the row
   count in Postgres matches.
4. **Nothing written pre-sign-in**: quit the tool, unset `SUPABASE_ANON_KEY`'s session by
   relaunching without signing in (choose "Work offline" instead), attempt **Save** — it must refuse
   (no network call made, `lastError` set) rather than silently doing nothing; confirm the row count
   in Postgres is unchanged from before.
5. **New map version**: use the menu action with a different placeholder image, confirm a new
   `map_config` row appears (`select * from map_config order by map_version desc`) and that every
   previously-placed pin now shows "(to reposition)" in the pin list.

## What wasn't verified here

Same sandboxed, display-less, Docker-less environment every step since 3 has noted: no GUI was ever
actually rendered or clicked, and no call in `data/`/`auth/` has touched a real Supabase project.
The supabase-kt API surface used (`createSupabaseClient`, `Auth.signInWith(Email)`,
`Postgrest.from(...).select/upsert/update`, `Columns.list`, `FilterOperator.IS`) was verified by
decompiling the actual `auth-kt-jvm`/`postgrest-kt-jvm` 3.1.4 jars (`javap`) rather than from
memory, specifically because this module couldn't be run against a live project to catch a wrong
method signature — but it has still never executed against a real GoTrue/PostgREST server. Budget
real time for the manual script above (and a real device/monitor, if run headless) before trusting
this tool with real content.

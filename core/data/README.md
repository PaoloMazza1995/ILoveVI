# :core:data

The offline-first data layer: repositories screens read from (never `:core:database` directly), and
the sync engine that keeps the local database (step 6) up to date from the backend (step 7A).
`commonMain` only — no Compose. **The app is offline-first**: every repository read comes straight
from the local SQLite database; the network only ever feeds that database, through `ContentSyncer`.

## Dependencies

`:core:model`, `:core:database` (the DAOs this module wraps/writes through), `:core:network` (the
remote data sources `ContentSyncer` drives), `kotlinx-coroutines-core`, `kotlinx-serialization-json`
(the bundled seed), and plain Koin (`dataModule`, same style as `databaseModule`/`networkModule`).

## Repositories

One interface + `internal` implementation per feature area (`repository/`), each a thin pass-
through to the matching `:core:database` DAO — the DAOs already return `Flow`s of `:core:model`
types directly, so there's no further mapping to do. The point of this layer isn't transformation,
it's the architectural boundary: a screen depends on `CatalogRepository`/`MusicRepository`/
`MapRepository`/`SettingsRepository`, never on `:core:database`, so the database stays this module's
own implementation detail and could be swapped without any feature module noticing.

- **`CatalogRepository`**: weapons/vehicles/items — list, get by id, search, plus the uniform
  `entry(category, entryId)` lookup.
- **`MusicRepository`**: stations/songs (read-only, synced) and compilations (add/remove/reorder/
  rename/create/delete — local-only, never synced, no backend table for them at all).
- **`MapRepository`**: current `MapConfig`, pins (optionally filtered by category/sub-type, joined
  with their entry's display name).
- **`SettingsRepository`**: user preferences.

`MusicRepository`/`MapRepository`/`SettingsRepository` each have their own `Compilation`/
`CompilationSongEntry`/`MapPinWithEntryName`/`Preferences` (`repository/RepositoryTypes.kt`), with
the `*RepositoryImpl`s mapping from `:core:database`'s rows into them. **Updated during step 9**:
this module originally reused `:core:database`'s own identically-shaped types here directly,
reasoning they were "already a clean abstraction, not raw persistence detail" — that turned out to
be wrong in practice. `:core:database` is only an `implementation` (not `api`) dependency of this
module, so none of its types are visible to a consumer that only depends on `:core:data` — a feature
module implementing one of these interfaces (in a test fake, say) would need a direct
`:core:database` dependency just to spell the return type, exactly the leak this module exists to
prevent. Found and fixed while wiring `:feature:catalog` (step 9), which hit it immediately on
`MapPinWithEntryName`; see that module's own README for the full story.

## `ContentSyncer`

Syncs `map_config` → `stations` → `weapons`/`vehicles`/`items` → `songs` → `map_pins`, in that order
(`songs.station_id` is a real local FK to `stations`, so stations must land first; the rest follow
the same order for consistency, though nothing else depends on it). `feed_sources` is deliberately
not synced here — that belongs to the news feature (steps 12A/12B).

Each table is paged independently via `sync/TableSync.kt`'s shared `syncTable` helper, following
`:core:network`'s own delta-sync convention: fetch a page (`since = null` on a first sync), apply
every row — upsert or tombstone — and advance that table's cursor (`:core:database`'s `SyncDao`)
**together, in one transaction** (`IloveViDatabase.inTransaction`, a small addition to
`:core:database` — see below), then repeat until a short page signals nothing is left. Because the
content and the cursor commit atomically, **an interruption between pages resumes from the last
committed page** on the next `sync()` call, never redownloading it and never skipping past it.

**A table's failure never touches any other table**: `sync()` runs all seven `syncTable` calls
unconditionally (`listOfNotNull(syncMapConfig(), syncStations(), ...)`), each wrapped in its own
try/catch and its own loop — one table returning an error string just means that string ends up in
`SyncState.lastError`, never an exception that could abort a sibling table's own, independent
database transaction.

`sync()` is reentrant-safe (a `Mutex.tryLock()` makes a concurrent call while one is already running
a no-op) and never throws. `start(scope)` wires the two automatic triggers the roadmap step asks
for — one immediate, non-blocking `sync()` (app start) and one more every time
`ConnectivityMonitor.isOnline` transitions false→true (connectivity returns) — the third trigger,
manual refresh, is just calling `sync()` directly from wherever that UI action lives. Nothing in
this module calls `start()` itself: same as `:core:database`/`:core:network` staying unwired from
`shared`'s `AppKoinModules.kt`, that's for whichever step actually assembles the real app shell.

### `IloveViDatabase.inTransaction` (a small `:core:database` addition)

SqlDelight's own `Transacter.transaction { }` takes a plain, non-`suspend` lambda (the driver
underneath is synchronous), but every DAO write method in `:core:database` is `suspend fun` by
convention even though none of them actually suspends. `inTransaction` (new file,
`core/database/.../DatabaseTransactions.kt`) bridges the two with a `runBlocking` that's safe
specifically because nothing inside it ever really suspends — see that file's own KDoc for the full
reasoning. This is the one place `ContentSyncer` (and `SeedImporter`, below) can apply several DAO
writes as a single real SQL transaction.

### `createCatalogDao`/`createMapDao`/`createMusicDao`/`createSyncDao`/
`createPreferencesDao` (another small `:core:database` addition)

`CatalogDaoImpl` etc. are `internal` to `:core:database` — right for production code, which only
ever needs the public DAO *interfaces* via `databaseModule`/Koin. This module's own tests build an
in-memory `IloveViDatabase` directly (no Koin, no `PlatformContext`) and need real DAO instances on
top of it; these five one-line public factories (new file, `core/database/.../dao/DaoFactory.kt`)
are the only way to get one without touching `*Impl` directly. The equivalent addition to
`:core:network` (`createWeaponRemoteDataSource` etc., `core/network/.../RemoteDataSourceFactory.kt`)
exists for the same reason, used by this module's own `LocalSupabaseAcceptanceTest`.

## `ConnectivityMonitor`

`expect class PlatformConnectivityMonitor(context: PlatformContext) : ConnectivityMonitor` — the
same `expect`/`actual` factory shape, and the same `:core:database` `PlatformContext` (reused rather
than inventing a second "app context" abstraction), that `DatabaseDriverFactory` already uses.
Android: a real `ConnectivityManager.NetworkCallback`. iOS: Network.framework's path monitor via its
low-level C API (`nw_path_monitor_*` — the layer Kotlin/Native actually binds to, not the Swift/ObjC
`NWPathMonitor` wrapper); compiles clean against the real framework on
`iosSimulatorArm64`/`iosArm64`. JVM: no desktop-portable "connectivity changed" OS callback exists,
so this one polls `NetworkInterface` for any up, non-loopback interface every 5s — a best-effort
heuristic (interface-up isn't the same as internet-reachable), reasonable here since no real desktop
build of this app exists yet.

## Bundled seed (`seed/`)

`SeedImporter.importIfNeeded()` applies a small, obviously-fake dataset (`SeedJson.kt`, bracketed
placeholder names, `seed-`-prefixed ids distinct from any real backend id) exactly once, inside one
transaction, so the app is never empty offline before the first real sync has had a chance to run.
"Already imported" reuses `SyncDao`'s per-table cursor table (`setLastSyncedAt("seed", now)`)
rather than adding a new table/column to `:core:database` for a single bit.

The seed is a plain compiled-in Kotlin string, not a platform resource file: this module has no
Compose dependency (so no `composeResources`), and this repo has no other established mechanism for
bundling a plain-text asset uniformly across Android/iOS/JVM in a non-Compose KMP library. A small,
self-contained set of `@Serializable` DTOs (`SeedDto.kt`) parses it — not reused from
`:core:network`'s own DTOs, which are `internal` to that module and whose shape this seed has no
need to track anyway, since it's entirely under this module's own control.

## Koin

`di/DataModule.kt`'s `dataModule`: the four repositories, `ConnectivityMonitor`, `SeedImporter` and
`ContentSyncer`, all as singles. Expects `databaseModule` and `networkModule` to already be included
wherever this one is (every binding here resolves at least one of their singles), plus a
`PlatformContext` single — the same requirement `databaseModule` already documents.

## Tests

`commonTest` (JVM + iOS-simulator *compilation* — see "What wasn't verified" below for why
iOS-simulator test *execution* specifically is the one exception to this repo's usual JVM +
iOS-simulator scope), against a real in-memory `IloveViDatabase` (`TestDatabaseFactory.kt`, same
two-line shape as `:core:database`'s own, unreachable, `internal` one) and fake remote data sources
(`fake/FakeRemoteDataSources.kt` — one scriptable `fetchChanges` queue shared via inheritance by
seven thin `Fake*RemoteDataSource` classes, one per table, each recording every `since` it was asked
with so a test can assert exactly what the second sync requested):

- **`ContentSyncerTest`**: first sync applies every table's queued changes; a second sync requests
  (and the fake asserts it received) `since` equal to the first sync's newest row, and applies only
  the new one; a tombstoned row disappears from the DAO; **an interrupted sync resumes** — a full
  200-row page (forcing real pagination, not a shortcut) followed by a simulated connection drop for
  page 2, then a second `sync()` call that resumes from exactly the committed cursor, never
  redownloading page 1; one table queued to fail (`NetworkResult.Offline`) while another succeeds —
  the successful one's rows land regardless, and `lastError` names the failing table; two concurrent
  `sync()` calls don't double-apply.
- **`SeedImporterTest`**: first import populates every table; a second `importIfNeeded()` call is a
  no-op (no duplicate rows).
- **`MusicRepositoryTest`**: `reorderCompilation` persists the new order (read back via
  `songsForCompilation`); rename/delete round-trip.
- **`CatalogRepositoryTest`**/ **`MapRepositoryTest`**/ **`SettingsRepositoryTest`**: a small smoke
  test each, confirming the repository reads exactly what was written to the database through the
  matching DAO.
- **`LocalSupabaseAcceptanceTest`** (`jvmTest`): the roadmap step's own acceptance check — one real
  sync against a local Supabase stack, then reads straight from the local database with no network
  client involved ("shows data with the network off"), then a second sync against the same,
  unchanged backend asserts no error and nothing left to apply. Self-skips (logs why, doesn't fail)
  unless `SUPABASE_URL`/`SUPABASE_ANON_KEY` are set to a local stack's printed values.

## What wasn't verified here

This is a sandboxed, headless environment: no Docker, so `LocalSupabaseAcceptanceTest` could not
actually be run against a live local stack — see that file's KDoc for the exact command to run it
for real once Docker is available.

Separately, and specific to this module: **this repo's usual "JVM + iOS-simulator" test scope had to
be narrowed to "JVM + iOS-simulator *compilation only*"** for this module alone. `commonTest`'s
Kotlin compiles cleanly for `iosSimulatorArm64` (confirmed — `compileTestKotlinIosSimulatorArm64`
succeeds), but *linking* the resulting test binary fails with missing `sqlite3_*` symbols from the
native SQLite driver's cinterop. This reproduces identically with no `:core:data` code involved at
all — e.g. trying to link `:core:data`'s test binary after only adding `sqldelight.native.driver`
as this module's own direct `iosTest` dependency (needed to build an in-memory test database the
same way `:core:database`'s own, unreachable, `internal` `testDatabase()` does) — while
`:core:database`'s own `iosSimulatorArm64Test` (the exact same dependency, declared one module
"shallower") passes in this same environment, confirmed by re-running it immediately after. This
looks like a genuine Kotlin/Native limitation in how a cinterop library's linker flags propagate
when a *second* Gradle module independently depends on it for its own test binary, not a bug in this
module's Kotlin source, and not a caching artifact (`-Pkotlin.native.cacheKind=none` made no
difference). Every required test, including the full "resumes after an interruption"/"one table
failing" scenarios, is still fully exercised and green on the JVM target.

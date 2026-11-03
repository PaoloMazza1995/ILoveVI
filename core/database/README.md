# :core:database

The local SQLDelight database: the offline-first source of truth every screen reads from. It mirrors
`:core:model`'s classes and the backend tables from step 3 (`weapons`, `vehicles`, `items`,
`stations`, `songs`, `map_config`, `map_pins`, `feed_sources`), plus a handful of device-local-only
tables (`news_items`, `compilation`, `compilation_song`, `preferences`, `sync_state`). `commonMain`
only — no Compose, no network; `:core:model` and coroutines are the only cross-module dependencies.

## Dependencies

`:core:model` (the `Weapon`/`Vehicle`/`Item`/`Station`/`Song`/`MapConfig`/`MapPin`/`EntryRef`/
`FeedSource`/`NewsItem` classes this module's rows map to/from), SQLDelight 2.0.2 (`runtime`,
`coroutines-extensions`, plus one platform driver per target), `kotlinx-coroutines-core` (`Flow`),
`kotlinx-serialization-json` (encoding the `attributes` free-form map column as JSON text) and plain
Koin (`databaseModule`, same style as `:tools:map-editor`'s `appModule` — not the dormant
koin-annotations/KSP convention plugin). Deliberately **not** a dependency: anything network-facing
(Ktor, supabase-kt) or Compose — a sync engine built on top of this module owns moving bytes between
the backend and these tables; this module only owns the tables themselves.

## Schema conventions

- **Dates are epoch milliseconds** (`INTEGER` columns), not ISO strings — simpler/faster for SQLite
  (integer sort order, no per-row string parsing), and converted to/from `kotlin.time.Instant` only
  at the mapper boundary (`Instant.fromEpochMilliseconds`/`.toEpochMilliseconds()`), the same
  `Instant` type `:core:model` uses everywhere (not kotlinx-datetime's).
- **Content tables** (the ones mirroring a backend table, plus `news_items` — see below) keep
  `updatedAt`/`deletedAt` and are soft-deleted (`tombstone` sets `deletedAt`, never a SQL
  `DELETE`); every read query filters `deletedAt IS NULL`. `news_items` has no backend table behind
  it at all (device-local cache only, per the functional analysis's data model — see
  `backend/supabase/migrations/20261011120005_feed_sources.sql`'s comment): `:core:data`'s
  `ContentSyncerImpl.syncNews()` fetches and parses every enabled `feed_sources` URL directly,
  on-device, then upserts the result here and trims each topic back to its newest 200 rows
  (`NewsDao.trimNewsItems`, tombstone-based, same convention as every other table) — the storage
  shape is identical to a synced content table, only what populates it differs.
- **Search (case/accent-insensitive)**: every searchable content table has a `searchKey` column
  (name/zone or title/artist, lowercased and Latin-accent-folded at write time by
  `foldForSearch()`), matched with a plain `LIKE '%...%'` against an equally-folded query string.
  This was chosen over `COLLATE NOCASE` + an ICU/unaccent SQLite extension because neither Android's
  bundled SQLite, `sqlite-driver` (JVM/JDBC) nor `native-driver` (iOS) ship such an extension, and
  loading one would be a non-trivial, platform-specific native dependency. **What this does and
  doesn't cover**: `foldForSearch()` is a small hand-written table (à/á/â/ã/ä/å→a, è/é/ê/ë→e, etc.)
  covering common Latin-1 accents — it is not full Unicode NFD normalization, so it will miss
  accented characters outside that table (Cyrillic, CJK, etc. are unaffected either way since they
  have no "unaccented" equivalent to fold to).
- **Primary keys mirror the backend's text ids** (`WeaponId.value` etc.) directly as `TEXT PRIMARY
  KEY` — no separate autoincrement surrogate key — so a delta-sync upsert's conflict target
  (`ON CONFLICT(id) DO UPDATE SET ...`) is exactly the id the backend already assigned.
- **`map_pins`** stores `category`/`entryId` (not a polymorphic FK — same reason the backend's
  trigger-based validation does this rather than a real FK: `entryId` can point at `weaponEntity`,
  `vehicleEntity` or `itemEntity` depending on `category`) plus `mapVersion`, mirroring the backend
  1:1. The category→table dispatch happens in `MapDao`/the mappers, not in SQL.

## Public API

Each feature area is one small DAO interface in `com.pamaz.vi.core.database.dao`, backed by an
internal `*Impl` class and exposed through `databaseModule` (`com.pamaz.vi.core.database.di`):

- **`CatalogDao`**: `weapons()`/`vehicles()`/`items()` (list, excludes tombstoned), `weapon(id)`/
  `vehicle(id)`/`item(id)` (get by id), `searchWeapons(query)`/`searchVehicles(query)`/
  `searchItems(query)` (case/accent-insensitive name-or-zone match), `entry(category, entryId)`
  (the "Entry-level lookup" — dispatches to the right table by `category` and returns the uniform
  `Entry` view, without the caller needing to know which of the three tables it's actually in), plus
  `upsertWeapon`/`upsertVehicle`/`upsertItem` and `tombstoneWeapon`/`tombstoneVehicle`/
  `tombstoneItem` (writes for these three tables live here too, rather than on a separate sync-only
  DAO, since they operate on exactly the tables `CatalogDao` already wraps).
- **`MapDao`**: `currentMapConfig()` (latest non-tombstoned `map_config` row, by `mapVersion`),
  `pins(categories, subTypes)` (pins for the requested categories, each joined with its entry's
  `name`/`subType` — returned as `MapPinWithEntryName(pin: MapPin, entryName: String)` so a caller
  doesn't need a second catalog lookup just to label a pin on screen; an empty/null `subTypes` means
  "no sub-type filter" for every requested category), `pinByEntryId(category, entryId)`, plus
  `upsertMapConfig`/`upsertPin`/`tombstonePin`/`tombstoneMapConfig`.
- **`MusicDao`**: `stations()`, `songs()`/`songsByStation(stationId)`/`song(id)` (all joined with
  their station), `searchSongs(stationId?, query?)` (either filter nullable/independent), plus
  `upsertStation`/`upsertSong`/`tombstoneStation`/`tombstoneSong`; and compilation CRUD —
  `compilations()`/`compilation(id)`/`songsForCompilation(compilationId)` (ordered by `position`,
  returned as `CompilationSongEntry(song, position)`), `createCompilation`/`renameCompilation`/
  `setCompilationSpotifyPlaylistId`/`deleteCompilation`, `addSongToCompilation` (appends at
  `maxPosition + 1`, computed inside a transaction so concurrent adds can't race to the same
  position), `removeSongFromCompilation`, `reorderCompilation(compilationId, orderedSongIds)`
  (rewrites every row's `position` to match the given order, in one transaction). Only a single
  compilation is exercised by any UI in v1, per the roadmap step's scope, but the schema/DAO support
  any number of them.
- **`NewsDao`**: `feedSources()`/`enabledFeedSources(topic)`, `newsItems(limit = 300)` (every live
  item, newest `publishedAt` first, merged across topics — the News screen's All/News/Missions
  filter is client-side), plus `upsertFeedSource`/`tombstoneFeedSource`,
  `upsertNewsItem`/`tombstoneNewsItem`, and `trimNewsItems(topic, keep, deletedAt, updatedAt)`
  (tombstones everything past the newest `keep` rows for that topic — same soft-delete convention as
  every other table, see the "schema conventions" note above).
- **`SyncDao`**: `lastSyncedAt(tableName)`/`setLastSyncedAt(tableName, at)` — delta-sync bookkeeping
  only. Upserting/tombstoning the content tables themselves is each feature DAO's job above, since
  they already own those tables' queries; a sync engine built on top calls both as needed.
- **`PreferencesDao`**: `preferences()` (`null` until the first `set` call — there is no row until
  then, deliberately not defaulted into existence on database creation) / `set(preferences)`. Not
  mentioned in the roadmap step's query list, but the table was, so a minimal DAO was added for
  consistency with every other table having one.
- **`databaseModule`**: a plain Koin `module { }` exposing `DatabaseDriverFactory`,
  `IloveViDatabase`
  and every DAO above as `single`s. It expects a `PlatformContext` single to already be bound by
  whichever app module includes it (see "Platform drivers" below) — this module cannot construct one
  itself, since only the platform app knows what a real Android `Context` is.
- **`dao/DaoFactory.kt`** (added for step 7B/`:core:data`): one-line public factories
  (`createCatalogDao(database)`, `createMapDao(database)`, ...) for each DAO's `internal` `*Impl`
  class, for a caller that already holds a real `IloveViDatabase` but isn't going through
  `databaseModule`/Koin — `:core:data`'s own tests, which build an in-memory `IloveViDatabase`
  directly. `databaseModule` could use these too but doesn't need to.
- **`DatabaseTransactions.kt`** (added for step 7B): `suspend fun IloveViDatabase.inTransaction(body:
  suspend () -> Unit)`, a thin bridge over `Transacter.transaction { }` (which takes a plain, non-
  `suspend` lambda) for a caller — `:core:data`'s `ContentSyncer`/`SeedImporter` — that needs
  several of this module's own `suspend` DAO write methods to commit as one real SQL transaction.
  See that file's own KDoc for why the `runBlocking` inside it is safe here specifically (nothing
  any DAO write method does ever actually suspends).

## Platform drivers

`DatabaseDriverFactory` is `expect class DatabaseDriverFactory(context: PlatformContext) { fun
createDriver(): SqlDriver }`, the same expect/actual-behind-a-small-factory pattern `:core:mapview`
already uses for `decodeImageBitmapDownscaled`. `PlatformContext` exists only so Android's actual
can carry a real `android.content.Context` through to `AndroidSqliteDriver` while iOS/JVM ignore it
entirely:

- **Android**: `actual class PlatformContext(val context: Context)` (a thin wrapper, not a
  `typealias` — `Context` is `abstract`, and an `expect class` is implicitly `final`, so a direct
  typealias fails with an `EXPECT_ACTUAL_INCOMPATIBLE_MODALITY` error). `createDriver()` builds
  `AndroidSqliteDriver(IloveViDatabase.Schema, context.context, "ilovevi.db")`, which creates/
  migrates the schema automatically via its own callback.
- **iOS**: `actual class PlatformContext` is an empty marker (nothing to carry). `createDriver()`
  builds `NativeSqliteDriver(IloveViDatabase.Schema, "ilovevi.db")`, same auto-create/migrate
  behavior as Android's driver.
- **JVM (desktop)**: `actual class PlatformContext` is likewise an empty marker. `createDriver()`
  opens `jdbc:sqlite:~/.ilovevi/ilovevi.db` via `JdbcSqliteDriver` — unlike the other two drivers,
  this one does **not** take a schema and does not auto-create it, so `createDriver()` checks
  whether the file already existed before opening it and calls
  `IloveViDatabase.Schema.create(driver)`
  itself exactly once, the first time.

`verifyMigrations.set(true)` is on in the Gradle config (`core/database/build.gradle.kts`), so a
future schema change that isn't paired with a correct `.sqm` migration file fails the build rather
than silently corrupting an existing on-device database. Schema is version 1; no migrations exist
yet.

**Upsert syntax note:** `ON CONFLICT(id) DO UPDATE SET ...` (SQLite's standard `UPSERT`, added in
3.24) needed `dialect(libs.sqldelight.dialect)` added to the `sqldelight { databases { ... } }`
block — SQLDelight's default dialect baseline predates that syntax and otherwise fails codegen with
a plain SQL parse error.

**`verifyMigrations` needs a baseline snapshot even with zero migrations:**
`verifyMigrations.set(true)`
alone made `:core:database:build` fail (`verifyCommonMainIloveViDatabaseMigration`, wired into
`check`) with "Verifying a migration requires a database file to be present" — decompiling the
SQLDelight Gradle plugin's `VerifyMigrationTask` (same empirical approach used elsewhere in this
repo) showed it unconditionally requires at least one `.db` snapshot file under the migrations
source folder once `verifyMigrations` is on, even with zero `.sqm` migration files. Fixed by adding
`schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))` (which also makes the
`generateCommonMainIloveViDatabaseSchema` task exist — it isn't registered at all otherwise) and
running it once to produce and commit `src/commonMain/sqldelight/databases/1.db`, the schema-v1
baseline. The next real schema change must add both a `.sqm` migration file **and** regenerate this
snapshot, or `verifyMigrations` will correctly fail the build.

## Tests

`commonTest` (run on JVM and iOS simulator — see "What was and wasn't verified" below) uses a fresh,
real in-memory `IloveViDatabase` per test (`testDatabase()`, `expect`/`actual`: JVM via
`JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)` + an explicit `Schema.create`; iOS via
`inMemoryDriver(IloveViDatabase.Schema)` from `native-driver`, confirmed to exist by inspecting the
downloaded klib's linkdata, the same decompile-and-verify approach used elsewhere in this repo for
undocumented third-party APIs) — never a shared/mocked database. Covered: every DAO query above,
upsert idempotence (`CatalogDaoTest.upsertIsIdempotent`), tombstones excluding rows from every read
path, the `map_pins`+entry join returning the right `entryName` *and* the right `EntryRef` subtype
per category (`MapDaoTest.pinJoinReturnsEntryNameAndRightEntryRef`), sub-type filtering, multi-
category combination, compilation add/remove/reorder, and news trimming to the latest 200
(`NewsDaoTest.trimToLatestKeepsOnlyNewest`, seeding 205 rows and asserting both the count and which
205th-and-older row got dropped).

## What was and wasn't verified here

This is a sandboxed, headless environment: there is no Android device/emulator and no Docker/
Supabase stack, so this step could not be verified against a real Android `Context`-backed
`AndroidSqliteDriver`, nor against real backend data flowing through a sync engine (no sync engine
exists yet — that's a later step).

What *was* verified:

- The module builds on every configured target (JVM, Android, iOS) — including the Android-specific
  `PlatformContext`/driver code, which compiles against the real `android.content.Context` API even
  though it was never run on a device here.
- The full test suite (24 tests) passes on **both** the JVM target and the **iOS simulator target**
  (`iosSimulatorArm64Test`), exercising the real generated SQLDelight code and a real (in-memory)
  SQLite engine on each, not a fake/mock database.
- SQLDelight's generated code for every `.sq` file was read and cross-checked by hand against this
  module's mappers/DAOs (column order, nullability, the `IN :collection` parameter-binding quirk
  that required splitting `MapPins.sq`'s filtered/unfiltered pin queries, the `dialect` requirement
  for `ON CONFLICT`), rather than assumed from memory.

**Still to confirm once a real device/emulator and Supabase stack are available**: `Preferences`/
`CatalogDao` etc. against the actual Android `SQLiteOpenHelper` on a real device (vs. this module's
in-memory JVM/iOS-simulator coverage); that a real delta-sync engine built on top of `SyncDao` +
every feature DAO's `upsert*`/`tombstone*` methods round-trips correctly against the live backend
tables from step 3; Android host unit tests remain not wired up for this module (same
`withHostTest {}` gap already noted across every other `core:*` module in this repo — JVM +
iOS-simulator is the established test-verification scope here).

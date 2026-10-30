# :core:network

Read-only remote data sources for the backend from step 3: `weapons`, `vehicles`, `items`,
`stations`, `songs`, `map_config`, `map_pins`, `feed_sources`. `commonMain` only (plus one small
HTTP-engine `actual` per platform) — no Compose, no local database. News (`NewsItem`) DTOs are not
here: `feed_sources` is the admin-curated feed list (covered below), but `NewsItem` itself is a
device-local cache per the functional analysis data model, fetched by a backend job, not synced from
this module — that's step 12B.

## Dependencies

`:core:model` (the `Weapon`/`Vehicle`/`Item`/`Station`/`Song`/`MapConfig`/`MapPin`/`EntryRef`/
`FeedSource` classes this module's rows map to), supabase-kt's `postgrest-kt` (query building +
response decoding), Ktor (`client-core`, `content-negotiation`,
`serialization-kotlinx-json`, plus one platform engine), `kotlinx-serialization-json` (DTOs),
`kotlinx-coroutines-core` and plain Koin (`networkModule`, same style as `:core:database`'s
`databaseModule`/`:tools:map-editor`'s `appModule` — not the dormant koin-annotations/KSP convention
plugin). Deliberately **not** a dependency: `:core:database`, `:core:data` or Compose — this module
only moves bytes off the wire into `:core:model` shapes; reconciling that with the local database is
a sync engine's job (`:core:data`, step 7B), not this one's.

## Client configuration

The app ships with **no sign-in** (read-only public content), so this module only ever needs the
project URL and the **publishable/anon key** — never the service-role key, never a session.
`config/SupabaseConfig.kt`'s `fromBuildConfig()` reads two `const val`s
(`config/NetworkBuildConfig.kt`, **generated**, not committed) that this module's
`generateNetworkBuildConfig` Gradle task (`build.gradle.kts`) writes at build time from the
`SUPABASE_URL`/`SUPABASE_ANON_KEY` environment variables, falling back to the git-ignored root
`local.properties` — the exact same two keys `:tools:map-editor`/`:tools:tilegen` already read for
their own Supabase clients.

This is a different mechanism from those two JVM-only tools on purpose: they're CLI processes that
forward real env vars into their own `JavaExec`/`run` task at run time
(`tools/map-editor/build.gradle.kts`'s `tasks.withType<JavaExec>()` block). This module is a KMP
library consumed by the Android/iOS app — there's no shell environment on a phone to read from — so
the values are baked into a generated `const val` at *build* time instead, the same shape
`:core:mapview`'s `generateMapViewBuildConfig`/`MapViewBuildConfig` already established for its own
release-gate flag. Nothing is ever hardcoded in a checked-in source file, and the generated file
itself lives under `build/generated/...`, never committed. A build run with neither the env var nor
`local.properties` set still compiles (blank constants), but
`SupabaseConfig.fromBuildConfig()`'s `require(...)` throws immediately at startup instead of
silently pointing at nothing.

## `SupabaseClientProvider`

Builds the one `SupabaseClient` every remote data source shares: `useHTTPS = true`, only
`Postgrest` installed (no `Auth` — this module never signs in), a small request/connect/socket
timeout (15s/10s/15s) and a 2-retry exponential-backoff policy on server errors
(`HttpRequestRetry.retryOnServerErrors`) — safe to retry blindly because every call this module
makes is a plain `select` (idempotent by construction, never a write). The HTTP engine is
`networkHttpClientEngine()`, an `expect`/`actual` factory (OkHttp on Android/JVM, Darwin on iOS) —
the same `expect`/`actual` shape `:core:database`'s `DatabaseDriverFactory` already uses for its
per-platform SQLite driver. Tests pass a `MockEngine` instead via the same `engine` parameter.

## DTOs and mapping

One `internal @Serializable` DTO per table (`dto/`), field names matching the backend's snake_case
columns exactly via `@SerialName`, timestamps kept as raw `String` (parsed to `kotlin.time.Instant`
only in the mapper) — the same convention `:tools:map-editor`'s row DTOs already use, rather than
relying on `kotlinx.serialization` having a built-in `Instant` serializer.

Each DTO has a `toChange()` mapper (`mapper/`) producing a `Change<T>`:

```kotlin
data class Change<T>(val item: T?, val id: String, val deletedAt: Instant?)
```

`item` is `null` in exactly two cases, disambiguated by `deletedAt`:

- **Tombstone** (`deletedAt` non-null): the row was soft-deleted server-side — the data layer should
  delete `id` locally. The rest of the row is never even mapped.
- **Unmappable** (`deletedAt` null): the row is still live, but couldn't be turned into a valid
  `T` — an unrecognized `sub_type`/`topic` enum constant, a `map_pins.category` that isn't one of
  `WEAPON`/`VEHICLE`/`ITEM`, or a `map_config` row failing `MapConfig`'s own `require`s (bad zoom
  range, non-positive size, a `tile_url_template` missing a placeholder). The data layer should skip
  `id`, not delete it — the row is still real, this module just can't represent it yet.

Both branches share one small helper, `mapper/RowChange.kt`'s `rowChange(id, deletedAtRaw, map)`,
which also means any unexpected exception inside `map` (a malformed `updated_at`, for instance)
degrades to "unmappable" instead of failing the whole page — one bad row must never take down an
otherwise-good page of 199 good ones.

`map_pins`' `category` + `entry_id` become an `EntryRef` (`mapper/MapMappers.kt`'s
`categoryAndEntryIdToEntryRef`) — the one place this module builds that sealed type from raw
strings. `map_config`'s `Change.id` is `mapVersion.toString()`, since that table's primary key is an
integer, not a text `id`.

## Remote data sources

One interface + `internal` implementation per table (`datasource/`), all with the same shape:

```kotlin
interface WeaponRemoteDataSource {
    suspend fun fetchChanges(since: Instant?, pageSize: Int): NetworkResult<List<Change<Weapon>>>
}
```

Every implementation calls the same shared helper, `datasource/DeltaPage.kt`'s `fetchDeltaPage`,
which issues exactly the query `backend/README.md`'s "Delta sync convention" describes:

```
GET /rest/v1/<table>?select=*&updated_at=gt.<since>&order=updated_at.asc.nullslast,<idColumn>.asc.nullslast&limit=<pageSize>
```

`since = null` omits the `updated_at` filter entirely (first sync: every row, including already-
tombstoned ones, is wanted — nothing here ever filters on `deleted_at`). `idColumn` defaults to
`"id"` and is `"map_config"`'s one exception (`"map_version"`). This call returns exactly one page;
whether the caller needs another is `items.size == pageSize` (a short page means nothing is left),
re-calling with `since` set to the last row's `updated_at` to continue — the same keyset-ish
re-paging the backend README itself calls sufficient for this convention. Driving that loop, and
reconciling the result with the local database, is `:core:data`'s job (step 7B), not this module's.

## `NetworkResult` and error mapping

```kotlin
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data object Offline : NetworkResult<Nothing>
    data object Timeout : NetworkResult<Nothing>
    data class ServerError(val code: Int) : NetworkResult<Nothing>
    data object ParseError : NetworkResult<Nothing>
}
```

No exception from a Ktor/supabase-kt call ever reaches a caller of this module: `safeNetworkCall`
(`NetworkResult.kt`) is the one place that catches and classifies them, in this order (more specific
first, since some subclass a broader Kotlin/Java type):

1. `CancellationException` — always rethrown, never classified (swallowing it would break structured
   concurrency).
2. `RestException` (supabase-kt's own, thrown by `Postgrest` for any non-2xx response it can
   parse) → `ServerError(statusCode)`.
3. `ResponseException` (a raw Ktor HTTP error, in case one ever surfaces un-wrapped) →
   `ServerError(response.status.value)`.
4. `HttpRequestTimeoutException` / `ConnectTimeoutException` → `Timeout`.
5. `SerializationException` (kotlinx.serialization — the body wasn't the JSON shape a DTO
   expected) → `ParseError`.
6. Anything else → `Offline`.

That last bucket is deliberate, not a cop-out: this module avoids referencing platform-specific
exception types (e.g. a bare `java.net.SocketTimeoutException`, thrown below Ktor's own timeout
plugin by the OkHttp/Android engine) from `commonMain`, so an unrecognized throwable —
overwhelmingly a raw connectivity failure (DNS, connection refused, no network) in practice — falls
back to the same bucket a confirmed offline error would. Branch 3 (`ResponseException`) is a
defensive fallback that isn't independently exercised by this module's tests, since every error path
actually driven through a real `Postgrest` call surfaces as `RestException` instead.

## Koin

`di/NetworkModule.kt`'s `networkModule`: one `SupabaseClient` single (built from
`SupabaseConfig.fromBuildConfig()`), plus one single per `*RemoteDataSource` interface above.
Nothing else — no repository, no sync engine, no database wiring; that's `:core:data`.

**`datasource/RemoteDataSourceFactory.kt`** (added for step 7B): one-line public factories
(`createWeaponRemoteDataSource(client)`, ...) for each data source's `internal` `*Impl` class, for a
caller that already holds a `SupabaseClient` but isn't going through `networkModule`/Koin —
`:core:data`'s own `LocalSupabaseAcceptanceTest`, which points at a local Supabase stack rather than
the build-time-configured production client `networkModule` wires up. `networkModule` could use
these too but doesn't need to.

## Tests

`commonTest` (JVM + iOS simulator, this repo's established scope):

- **Mapper tests** (`mapper/EntryMapperTest.kt`, `mapper/MapMappers`'s `MapMapperTest.kt`): fixture
  JSON decoded through each DTO and mapped, covering every nullable column actually null, a
  tombstoned row, and — the acceptance's explicit case — an unrecognized `map_pins.category`
  ("BUILDING") producing an unmappable `Change` instead of crashing. Also an unrecognized
  `sub_type` and an invalid `map_config` zoom range for the same reason.
- **`NetworkResultTest.kt`**: `safeNetworkCall` classification unit-tested directly against each
  exception type (fast and deterministic — no real timing-dependent timeout needed), plus confirming
  `CancellationException` is rethrown, not classified.
- **`datasource/WeaponRemoteDataSourceTest.kt`**: a real `WeaponRemoteDataSourceImpl` against a
  `MockEngine`, asserting the actual request query string — no `updated_at` filter when
  `since = null`, `updated_at=gt.<since>` when it isn't,
  `order=updated_at.asc.nullslast,id.asc.nullslast`, `limit=<n>`
  — plus end-to-end `ServerError`/`ParseError`/`Offline` mapping through a real `Postgrest` call
  (not just the unit-level exception classification above).

`jvmTest`'s `LocalSupabaseAcceptanceTest.kt` is the roadmap step's own acceptance check — against a
*running* local Supabase stack, fetch the seed rows and one delta page. It self-skips (passes
trivially, logging why) unless `SUPABASE_URL`/`SUPABASE_ANON_KEY` are set to a local stack's printed
values, since this sandboxed environment has no Docker to run `supabase start` with — see that
file's KDoc for the exact command to run it for real.

## What was and wasn't verified here

This is a sandboxed, headless environment: no Docker, so `LocalSupabaseAcceptanceTest` could not
actually be run against a live local stack — only the surrounding unit/mock-engine suite was, and is
green. supabase-kt's `Postgrest`/`SupabaseClientBuilder` APIs used here (`httpEngine`,
`httpConfig`, `order`/`limit`/`filter { gt(...) }`, `decodeList`) were all verified against the real
3.1.4 jars via `javap` (no sources jar was available), not from memory, following this repo's
established convention for third-party API surfaces — `ConnectTimeoutException`/
`HttpRequestTimeoutException`'s presence in Ktor's commonMain (not JVM-only) is standard, widely
documented Ktor knowledge and was not independently decompiled.

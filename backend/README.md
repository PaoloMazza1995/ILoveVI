# IloveVI backend (Supabase)

Content backend for the IloveVI app: weapons, vehicles, items, map data, and radio stations/songs.
Entirely read-only from the app's perspective. See Functional Analysis v0.3 sections 8, 9.2 and 9.4
for the product-level requirements this schema implements.

The news feed is **not** part of this backend: per Functional Analysis section 8/9.3, `NewsItem` is
a device-local cache — the app fetches and parses every enabled `feed_sources` row's RSS/Atom URL
directly, on-device (`:core:data`'s `ContentSyncerImpl.syncNews()`). This schema only owns the feed
*list* (`feed_sources` below), never the fetched stories themselves.

Everything lives under `backend/supabase/`, using the standard Supabase CLI project layout:

```
backend/supabase/
  config.toml          # local stack configuration (ports, auth, storage, …)
  seed.sql              # small, obviously-fake dataset for tests/previews
  migrations/            # versioned schema, applied in filename order
  tests/database/         # pgTAP test suite
```

## Prerequisites

- [Docker](https://www.docker.com/) (Desktop or Engine) — the local stack runs as containers.
- [Supabase CLI](https://supabase.com/docs/guides/local-development/cli/getting-started) —
  `brew install supabase`.

## Running locally

```sh
cd backend
supabase start      # first run pulls images and prints local URLs/keys
supabase db reset    # (re)applies every migration, then seed.sql, from a clean database
supabase test db      # runs the pgTAP suite in supabase/tests/database
```

`supabase start` prints a local API URL, anon key and service-role key — use the anon key in the
app's local config. `supabase stop` tears the stack down; add `--no-backup` only if you don't need
the local data kept around.

`supabase db reset` is also what CI / a fresh checkout should run to get to a known-good state: it
always rebuilds from migrations + seed, never from a snapshot.

## Schema

One table per `:core:model` entity that the backend owns. Field names map 1:1 to the Kotlin model
(snake_case in SQL); enum-valued columns (`sub_type`, `map_pins.category`, `feed_sources.topic`)
use the same constant spelling as the Kotlin enum (e.g. `MACHINE_GUN`, `WEAPON`, `MISSIONS`), so the
data layer does not need an enum translation table.

| Table                          | Mirrors                     | Notes                                                                                                                                                                                                                                                                                                                                                                                                                          |
|--------------------------------|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `weapons`, `vehicles`, `items` | `Weapon`, `Vehicle`, `Item` | `sub_type` CHECK against each type's enum; `attributes` is `jsonb`; only `weapons` has `price`/`ammo_type`.                                                                                                                                                                                                                                                                                                                    |
| `stations`                     | `Station`                   |                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `songs`                        | `Song`                      | `station_id` FK to `stations`.                                                                                                                                                                                                                                                                                                                                                                                                 |
| `map_config`                   | `MapConfig`                 | `map_version` is the primary key (not a surrogate `id`), matching `MapConfig.mapVersion`.                                                                                                                                                                                                                                                                                                                                      |
| `map_pins`                     | `MapPin` / `EntryRef`       | `(category, entry_id)` stands in for `EntryRef`; a trigger (`validate_map_pin_entry`) checks `entry_id` exists in whichever of `weapons`/`vehicles`/`items` matches `category`, since a plain foreign key can't target one of three tables conditionally. An entry can have any number of pins (`id` is the real primary/upsert key, not `(category, entry_id)` — see `20261021120001_map_pins_allow_multiple_per_entry.sql`). |
| `feed_sources`                 | `FeedSource`                | The feed list (news stories themselves are never stored here — see this file's own intro), admin-writable like every table above.                                                                                                                                                                                                                                                                                              |

Every table above has `updated_at timestamptz not null default now()` (bumped by a `BEFORE UPDATE`
trigger, `set_updated_at()`), a `(updated_at, id)` index (or `(updated_at, map_version)` for
`map_config`) supporting the delta-sync query below, and `deleted_at timestamptz null` for soft
deletes.

## Row Level Security

RLS is enabled on every table. `anon` and `authenticated` can always `SELECT` every table
(soft-deleted rows included — the app needs to see a deletion to apply it locally). Only an admin
can `INSERT`/`UPDATE`/`DELETE` any table.

Admin is defined by a custom JWT claim: `app_metadata.role = "admin"`, checked by the
`public.is_admin()` SQL helper
(`backend/supabase/migrations/20261011120001_extensions_and_helpers.sql`).
`app_metadata` can only be set with the service-role key, never by the user themselves, so this is
safe to trust from a policy.

### Creating the first admin user

There is no UI for this — it's a one-time `service_role`-authenticated call, done once per
environment:

```sh
curl -X PUT 'http://127.0.0.1:54321/auth/v1/admin/users/<user-id>' \
  -H "apikey: $SUPABASE_SERVICE_ROLE_KEY" \
  -H "Authorization: Bearer $SUPABASE_SERVICE_ROLE_KEY" \
  -H "Content-Type: application/json" \
  -d '{"app_metadata": {"role": "admin"}}'
```

(Create the user first — e.g. via `supabase/studio` locally, or the Auth API — then run the call
above with their user id.) The user's existing session won't see the new claim until they sign in
again, since the claim is baked into the JWT at token-issue time.

## Storage

Bucket `map-tiles` (created in `20261011120006_storage_map_tiles.sql`): public read, admin-only
write — holds the original map illustration's tiles (Functional Analysis section 9.4; no official
game map asset is ever stored here). Bucket `entry-images` (created in
`20261021120002_storage_entry_images.sql`, same public-read/admin-write shape): catalog entry images
uploaded by `:tools:map-editor`'s staged-pin Commit flow (step 5B extension).

## Delta sync convention

The app asks for rows changed since its last sync, soft-deleted rows included, ordered so paging is
stable:

```
GET /rest/v1/weapons?select=*&updated_at=gt.<lastSync>&order=updated_at.asc,id.asc&limit=200
```

Repeat with the increasing `updated_at`/`limit`/`offset` (or keyset on the last row's
`(updated_at, id)`) until a page returns fewer rows than the limit. A row with `deleted_at` set
means "remove this row locally," not "ignore this row." Plain PostgREST filters are enough for this;
no RPC is needed unless a future requirement needs cross-table joins the client can't express.

## Functions (RPCs)

`public.commit_map_edits(payload jsonb) returns jsonb`
(`20261021120003_commit_map_edits_rpc.sql`) is the first — and so far only — Postgres function
called via RPC (`postgrest.rpc(...)`) rather than through plain table endpoints. It exists because
`:tools:map-editor`'s staged-pin Commit flow needs to upsert any number of
`weapons`/`vehicles`/`items` and `map_pins` rows **atomically** (all or nothing), which plain
PostgREST calls (one HTTP request per table) can't guarantee. It's deliberately plain `security
invoker` (the default) — it runs with the caller's own role, so every write inside it still goes
through the normal table-level admin RLS policies, exactly as if the caller had called them
directly; it never uses the service-role key. Follow this same shape (a single plpgsql function
body, which is one implicit transaction) for any future requirement needing several tables written
together — not a new client-side multi-step "hope nothing fails in between" sequence.

## Seed data

`seed.sql` inserts a small set of obviously-fake rows (`[Weapon 1]`, `[Station 1]`, …) for tests and
UI previews, plus two `example.com` placeholder feeds and **one real one**
(`google-news-gta6`, a Google News RSS search) — see that row's own comment in `seed.sql` for the
trade-off this one real feed accepts. Real catalog content (the `[Weapon 1]`-style placeholders)
can only be collected after the game's release (19 November 2026 at the time of writing) — see
Functional Analysis v0.3 section 12.

## Tests

`supabase/tests/database/*.test.sql` are pgTAP tests, run with `supabase test db` against the local
stack. Each file wraps itself in `BEGIN; … ROLLBACK;` so the seed/test data never leaks between
files or into a real run. Covered:

- anonymous `SELECT` works, anonymous `INSERT` is refused, admin `INSERT` works
  (`001_select_and_write_rls.test.sql`).
- `updated_at` changes on `UPDATE`; a soft-deleted row still appears in a delta-sync-style `SELECT`
  (`002_updated_at_and_soft_delete.test.sql`).
- `map_pins.x = 1.5` is refused; a pin pointing at a missing entry is refused
  (`003_map_pin_constraints.test.sql`).
- `commit_map_edits`: admin can commit a new entry and its pin together, retrying the same payload
  creates no duplicates, anonymous is refused, and a bad coordinate fails (and saves nothing from)
  the whole call (`004_commit_map_edits.test.sql`).

On-device RSS/Atom fetching and parsing (`ContentSyncerImpl.syncNews()`) has its own test suite in
`:core:data` (`FeedParserTest`, `FeedMergeTest`) and `:core:network` (`NewsFeedFetcherTest`) — see
those modules, not this backend, for that coverage.

## Applying to a remote project

**Never run this without asking first** — it changes a real, possibly-shared database.

```sh
supabase link --project-ref <project-ref>
supabase db push   # applies any migration not yet on the remote
```

`supabase db push` only pushes schema (migrations), never `seed.sql` — seed is for local/test use
only. Content on a real project is entered through the backend directly (Functional Analysis section
9.2: spreadsheet import, planned separately from this schema work).

## Secrets

The service-role key (and any remote DB password/access token) **never enters this repo**. Locally,
`supabase start` prints fresh keys to your terminal each time — nothing is committed. For a remote
project, keep the service-role key and `SUPABASE_ACCESS_TOKEN` (used by `supabase link`/`db push`)
in your shell environment or a git-ignored `.env`/`local.properties`-style file, never in a
migration, seed file, or test. `backend/supabase/.gitignore` (generated by `supabase init`) already
excludes the CLI's local `.branches`/`.temp` state and `.env*.local` files.

# IloveVI backend (Supabase)

Read-only content backend for the IloveVI app: weapons, vehicles, items, map data, radio
stations/songs and the news-feed source list. See Functional Analysis v0.3 sections 8, 9.2 and 9.4
for the product-level requirements this schema implements.

Everything lives under `backend/supabase/`, using the standard Supabase CLI project layout:

```
backend/supabase/
  config.toml          # local stack configuration (ports, auth, storage, …)
  seed.sql              # small, obviously-fake dataset for tests/previews
  migrations/            # versioned schema, applied in filename order
  tests/database/        # pgTAP test suite
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

| Table                          | Mirrors                     | Notes                                                                                                                                                                                                                                                                                                             |
|--------------------------------|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `weapons`, `vehicles`, `items` | `Weapon`, `Vehicle`, `Item` | `sub_type` CHECK against each type's enum; `attributes` is `jsonb`; only `weapons` has `price`/`ammo_type`.                                                                                                                                                                                                       |
| `stations`                     | `Station`                   |                                                                                                                                                                                                                                                                                                                   |
| `songs`                        | `Song`                      | `station_id` FK to `stations`.                                                                                                                                                                                                                                                                                    |
| `map_config`                   | `MapConfig`                 | `map_version` is the primary key (not a surrogate `id`), matching `MapConfig.mapVersion`.                                                                                                                                                                                                                         |
| `map_pins`                     | `MapPin` / `EntryRef`       | `(category, entry_id)` stands in for `EntryRef`; a trigger (`validate_map_pin_entry`) checks `entry_id` exists in whichever of `weapons`/`vehicles`/`items` matches `category`, since a plain foreign key can't target one of three tables conditionally. One pin per `(category, entry_id)` (unique constraint). |
| `feed_sources`                 | `FeedSource`                | The feed list only — `NewsItem` is a **device-local cache** per the functional analysis data model (section 8) and intentionally has no backend table.                                                                                                                                                            |

Every table above has:

- `updated_at timestamptz not null default now()`, bumped by a `BEFORE UPDATE` trigger
  (`set_updated_at()`) — `INSERT` keeps whatever value is supplied, or the column default.
- `deleted_at timestamptz null` for soft deletes.
- A `(updated_at, id)` index (or `(updated_at, map_version)` for `map_config`) supporting the
  delta-sync query below.

## Row Level Security

RLS is enabled on every table. `anon` and `authenticated` can always `SELECT` (including
soft-deleted rows — the app needs to see a deletion to apply it locally). Only an admin can
`INSERT`/`UPDATE`/`DELETE`.

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
game map asset is ever stored here).

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

## Seed data

`seed.sql` inserts a small set of obviously-fake rows (`[Weapon 1]`, `[Station 1]`, …) for tests and
UI previews. Real content can only be collected after the game's release (19 November 2026 at the
time of writing) — see Functional Analysis v0.3 section 12.

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

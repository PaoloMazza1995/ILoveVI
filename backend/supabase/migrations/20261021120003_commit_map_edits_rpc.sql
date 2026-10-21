-- commit_map_edits: the atomic server side of :tools:map-editor's staged-pin draft/Commit flow
-- (step 5B extension). One call upserts any number of new/updated weapons, vehicles, items and
-- map_pins together. Deliberately NOT `security definer`: it runs with the caller's own role, so
-- every insert/update below still goes through each table's existing admin RLS policies with the
-- caller's own JWT — this tool never uses the service-role key for these writes, matching
-- :tools:map-editor's own SupabaseConfig.kt convention. The explicit is_admin() check up front is
-- just a single clear error instead of N separate per-row RLS failures; RLS is the real guard.
--
-- A plpgsql function body is one implicit transaction: any `raise` below (non-admin, stale
-- map_version, a bad coordinate, a missing entry_id via the existing validate_map_pin_entry
-- trigger, ...) rolls back every statement already run earlier in the same call, including entry
-- inserts — so "all or nothing" needs no explicit BEGIN/ROLLBACK.
--
-- Every target table is upserted by its own primary key `id` (client-generated UUIDs from the
-- draft), so retrying the exact same payload after a network error just re-applies the identical
-- upsert — no separate idempotency ledger needed.
--
-- payload shape:
-- {
--   "map_version": 3,
--   "entries": {
--     "weapons":  [{"id","name","description","sub_type","zone","image_url","price","ammo_type","attributes"}],
--     "vehicles": [{"id","name","description","sub_type","zone","image_url","attributes"}],
--     "items":    [{"id","name","description","sub_type","zone","image_url","attributes"}]
--   },
--   "pins": [{"id","category","entry_id","x","y","zone","map_version"}]
-- }
-- Any of "entries"/"weapons"/"vehicles"/"items"/"pins" may be omitted (treated as empty).
create or replace function public.commit_map_edits(payload jsonb)
returns jsonb
language plpgsql
set search_path = ''
as $$
declare
  current_map_version integer;
  payload_map_version integer;
  weapons_ids text[];
  vehicles_ids text[];
  items_ids text[];
  pins_ids text[];
  weapons_existing integer;
  vehicles_existing integer;
  items_existing integer;
  pins_existing integer;
begin
  if not public.is_admin() then
    raise exception 'commit_map_edits requires an admin session';
  end if;

  payload_map_version := (payload ->> 'map_version')::integer;
  select map_version into current_map_version
  from public.map_config
  where deleted_at is null
  order by map_version desc
  limit 1;

  if current_map_version is null or payload_map_version is distinct from current_map_version then
    raise exception 'commit_map_edits: payload map_version % does not match the current map_version % (reload the map and retry)',
      payload_map_version, current_map_version;
  end if;

  select coalesce(array_agg(x.id), '{}') into weapons_ids
    from jsonb_to_recordset(coalesce(payload -> 'entries' -> 'weapons', '[]'::jsonb)) as x(id text);
  select count(*) into weapons_existing from public.weapons where id = any(weapons_ids);

  select coalesce(array_agg(x.id), '{}') into vehicles_ids
    from jsonb_to_recordset(coalesce(payload -> 'entries' -> 'vehicles', '[]'::jsonb)) as x(id text);
  select count(*) into vehicles_existing from public.vehicles where id = any(vehicles_ids);

  select coalesce(array_agg(x.id), '{}') into items_ids
    from jsonb_to_recordset(coalesce(payload -> 'entries' -> 'items', '[]'::jsonb)) as x(id text);
  select count(*) into items_existing from public.items where id = any(items_ids);

  select coalesce(array_agg(x.id), '{}') into pins_ids
    from jsonb_to_recordset(coalesce(payload -> 'pins', '[]'::jsonb)) as x(id text);
  select count(*) into pins_existing from public.map_pins where id = any(pins_ids);

  insert into public.weapons (id, name, description, sub_type, zone, image_url, price, ammo_type, attributes)
  select id, name, description, sub_type, zone, image_url, price, ammo_type, coalesce(attributes, '{}'::jsonb)
  from jsonb_to_recordset(coalesce(payload -> 'entries' -> 'weapons', '[]'::jsonb)) as x(
    id text, name text, description text, sub_type text, zone text, image_url text,
    price integer, ammo_type text, attributes jsonb
  )
  on conflict (id) do update set
    name = excluded.name,
    description = excluded.description,
    sub_type = excluded.sub_type,
    zone = excluded.zone,
    image_url = excluded.image_url,
    price = excluded.price,
    ammo_type = excluded.ammo_type,
    attributes = excluded.attributes,
    deleted_at = null;

  insert into public.vehicles (id, name, description, sub_type, zone, image_url, attributes)
  select id, name, description, sub_type, zone, image_url, coalesce(attributes, '{}'::jsonb)
  from jsonb_to_recordset(coalesce(payload -> 'entries' -> 'vehicles', '[]'::jsonb)) as x(
    id text, name text, description text, sub_type text, zone text, image_url text, attributes jsonb
  )
  on conflict (id) do update set
    name = excluded.name,
    description = excluded.description,
    sub_type = excluded.sub_type,
    zone = excluded.zone,
    image_url = excluded.image_url,
    attributes = excluded.attributes,
    deleted_at = null;

  insert into public.items (id, name, description, sub_type, zone, image_url, attributes)
  select id, name, description, sub_type, zone, image_url, coalesce(attributes, '{}'::jsonb)
  from jsonb_to_recordset(coalesce(payload -> 'entries' -> 'items', '[]'::jsonb)) as x(
    id text, name text, description text, sub_type text, zone text, image_url text, attributes jsonb
  )
  on conflict (id) do update set
    name = excluded.name,
    description = excluded.description,
    sub_type = excluded.sub_type,
    zone = excluded.zone,
    image_url = excluded.image_url,
    attributes = excluded.attributes,
    deleted_at = null;

  insert into public.map_pins (id, category, entry_id, x, y, zone, map_version)
  select id, category, entry_id, x, y, zone, map_version
  from jsonb_to_recordset(coalesce(payload -> 'pins', '[]'::jsonb)) as x(
    id text, category text, entry_id text, x real, y real, zone text, map_version integer
  )
  on conflict (id) do update set
    category = excluded.category,
    entry_id = excluded.entry_id,
    x = excluded.x,
    y = excluded.y,
    zone = excluded.zone,
    map_version = excluded.map_version,
    deleted_at = null;

  return jsonb_build_object(
    'entries_created',
      (coalesce(array_length(weapons_ids, 1), 0) - weapons_existing)
      + (coalesce(array_length(vehicles_ids, 1), 0) - vehicles_existing)
      + (coalesce(array_length(items_ids, 1), 0) - items_existing),
    'entries_updated', weapons_existing + vehicles_existing + items_existing,
    'pins_created', coalesce(array_length(pins_ids, 1), 0) - pins_existing,
    'pins_moved', pins_existing
  );
end;
$$;

comment on function public.commit_map_edits(jsonb) is
  'Atomic upsert of staged weapons/vehicles/items + map_pins from :tools:map-editor''s Commit button. See backend/supabase/tests/database/004_commit_map_edits.test.sql.';

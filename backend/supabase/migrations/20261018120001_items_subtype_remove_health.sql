-- Removes HEALTH from items.sub_type: GTA VI has no health-pickup item type. Item sub-types are
-- explicit placeholders (see :core:model's ItemType KDoc) pending the real GTA VI item
-- classification — see docs/ROADMAP.md's "Open decisions" entry.
--
-- Never edit an already-applied migration (20261011120002_weapons_vehicles_items.sql, which first
-- defined this CHECK constraint): this is a new migration that narrows it instead.
--
-- Existing data: any row already using 'HEALTH' would violate the narrower constraint below, so it
-- must be dealt with first. This project has no real user-entered content yet (see
-- docs/ROADMAP.md's "Content after release" section — only backend/supabase/seed.sql's
-- obviously-fake placeholder rows exist pre-release), so there is nothing of real value to lose:
-- any 'HEALTH' row found is logged via RAISE NOTICE for visibility, then hard-deleted, along with
-- any map_pins row that points at it. A soft delete (deleted_at) is not an option here — the CHECK
-- constraint applies to every row regardless of deleted_at, so a soft-deleted 'HEALTH' row would
-- still fail it. map_pins has no real foreign key to items (see 20261011120004_map.sql's
-- validate_map_pin_entry trigger, which only validates on insert/update) so deleting the item alone
-- would silently leave its pin dangling; both are removed together.
do $$
declare
  removed_ids text[];
begin
  select array_agg(id) into removed_ids from public.items where sub_type = 'HEALTH';

  if removed_ids is not null then
    raise notice 'items_subtype_remove_health: removing % row(s) with sub_type = HEALTH: %',
      array_length(removed_ids, 1), removed_ids;

    delete from public.map_pins where category = 'ITEM' and entry_id = any (removed_ids);
    delete from public.items where sub_type = 'HEALTH';
  end if;
end;
$$;

-- Drop the existing sub_type CHECK constraint by looking up its actual name (Postgres' default
-- auto-generated name for an unnamed column-level CHECK) rather than assuming it, then recreate it
-- narrowed to the remaining values, explicitly named this time for any future migration to target.
do $$
declare
  existing_constraint text;
begin
  select conname into existing_constraint
  from pg_constraint
  where conrelid = 'public.items'::regclass
    and contype = 'c'
    and pg_get_constraintdef(oid) ilike '%sub_type%';

  execute format('alter table public.items drop constraint %I', existing_constraint);
end;
$$;

alter table public.items
  add constraint items_sub_type_check check (sub_type in ('AMMO', 'ARMOR'));

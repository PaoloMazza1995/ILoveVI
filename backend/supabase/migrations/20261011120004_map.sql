-- MapConfig describes the current map image/tiles; MapPin places one located entry on it.
-- category values match EntryCategory's Kotlin constant names (WEAPON/VEHICLE/ITEM), consistent
-- with the sub_type convention used for weapons/vehicles/items.

create table public.map_config (
  map_version integer primary key,
  source_image_url text not null,
  width_px integer not null check (width_px > 0),
  height_px integer not null check (height_px > 0),
  tile_url_template text not null,
  min_zoom integer not null,
  max_zoom integer not null,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint map_config_zoom_range check (min_zoom <= max_zoom)
);

create table public.map_pins (
  id text primary key,
  category text not null check (category in ('WEAPON', 'VEHICLE', 'ITEM')),
  entry_id text not null,
  x real not null check (x >= 0 and x <= 1),
  y real not null check (y >= 0 and y <= 1),
  zone text,
  map_version integer not null references public.map_config (map_version),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  constraint map_pins_one_per_entry unique (category, entry_id)
);

create index map_config_updated_at_idx on public.map_config (updated_at, map_version);
create index map_pins_updated_at_idx on public.map_pins (updated_at, id);
create index map_pins_map_version_idx on public.map_pins (map_version);

create trigger map_config_set_updated_at before update on public.map_config
  for each row execute function public.set_updated_at();
create trigger map_pins_set_updated_at before update on public.map_pins
  for each row execute function public.set_updated_at();

-- entry_id has no plain FK since it points at one of three tables depending on category; this
-- trigger enforces the same "must exist" guarantee a FK would give.
create or replace function public.validate_map_pin_entry()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if new.category = 'WEAPON' then
    if not exists (select 1 from public.weapons where id = new.entry_id) then
      raise exception 'map_pins.entry_id % not found in weapons for category WEAPON', new.entry_id;
    end if;
  elsif new.category = 'VEHICLE' then
    if not exists (select 1 from public.vehicles where id = new.entry_id) then
      raise exception 'map_pins.entry_id % not found in vehicles for category VEHICLE', new.entry_id;
    end if;
  elsif new.category = 'ITEM' then
    if not exists (select 1 from public.items where id = new.entry_id) then
      raise exception 'map_pins.entry_id % not found in items for category ITEM', new.entry_id;
    end if;
  end if;
  return new;
end;
$$;

create trigger map_pins_validate_entry before insert or update on public.map_pins
  for each row execute function public.validate_map_pin_entry();

alter table public.map_config enable row level security;
alter table public.map_pins enable row level security;

create policy map_config_select_anyone on public.map_config for select using (true);
create policy map_config_admin_insert on public.map_config for insert with check (public.is_admin());
create policy map_config_admin_update on public.map_config for update using (public.is_admin()) with check (public.is_admin());
create policy map_config_admin_delete on public.map_config for delete using (public.is_admin());

create policy map_pins_select_anyone on public.map_pins for select using (true);
create policy map_pins_admin_insert on public.map_pins for insert with check (public.is_admin());
create policy map_pins_admin_update on public.map_pins for update using (public.is_admin()) with check (public.is_admin());
create policy map_pins_admin_delete on public.map_pins for delete using (public.is_admin());

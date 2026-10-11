-- Weapons, vehicles and items: the three concrete Entry types in :core:model. sub_type CHECK
-- values match the Kotlin enum constant names exactly (WeaponType/VehicleType/ItemType) so the
-- data layer can deserialize the column straight into the enum with no translation table.

create table public.weapons (
  id text primary key,
  name text not null,
  description text not null,
  sub_type text not null check (sub_type in ('PISTOL', 'RIFLE', 'MACHINE_GUN', 'MELEE', 'EXPLOSIVE')),
  zone text,
  image_url text,
  attributes jsonb not null default '{}'::jsonb,
  price integer,
  ammo_type text,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table public.vehicles (
  id text primary key,
  name text not null,
  description text not null,
  sub_type text not null check (sub_type in ('CAR', 'MOTORCYCLE', 'BOAT', 'HELICOPTER', 'PLANE')),
  zone text,
  image_url text,
  attributes jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table public.items (
  id text primary key,
  name text not null,
  description text not null,
  sub_type text not null check (sub_type in ('HEALTH', 'AMMO', 'ARMOR')),
  zone text,
  image_url text,
  attributes jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create index weapons_updated_at_idx on public.weapons (updated_at, id);
create index vehicles_updated_at_idx on public.vehicles (updated_at, id);
create index items_updated_at_idx on public.items (updated_at, id);

create trigger weapons_set_updated_at before update on public.weapons
  for each row execute function public.set_updated_at();
create trigger vehicles_set_updated_at before update on public.vehicles
  for each row execute function public.set_updated_at();
create trigger items_set_updated_at before update on public.items
  for each row execute function public.set_updated_at();

alter table public.weapons enable row level security;
alter table public.vehicles enable row level security;
alter table public.items enable row level security;

create policy weapons_select_anyone on public.weapons for select using (true);
create policy weapons_admin_insert on public.weapons for insert with check (public.is_admin());
create policy weapons_admin_update on public.weapons for update using (public.is_admin()) with check (public.is_admin());
create policy weapons_admin_delete on public.weapons for delete using (public.is_admin());

create policy vehicles_select_anyone on public.vehicles for select using (true);
create policy vehicles_admin_insert on public.vehicles for insert with check (public.is_admin());
create policy vehicles_admin_update on public.vehicles for update using (public.is_admin()) with check (public.is_admin());
create policy vehicles_admin_delete on public.vehicles for delete using (public.is_admin());

create policy items_select_anyone on public.items for select using (true);
create policy items_admin_insert on public.items for insert with check (public.is_admin());
create policy items_admin_update on public.items for update using (public.is_admin()) with check (public.is_admin());
create policy items_admin_delete on public.items for delete using (public.is_admin());

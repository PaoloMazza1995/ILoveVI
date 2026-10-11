-- Radio stations and the songs on their playlists (:core:model Station / Song). No audio or
-- lyrics are stored, only metadata plus an optional cached Spotify track id.

create table public.stations (
  id text primary key,
  name text not null,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table public.songs (
  id text primary key,
  title text not null,
  artist text not null,
  station_id text not null references public.stations (id),
  spotify_track_id text,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create index stations_updated_at_idx on public.stations (updated_at, id);
create index songs_updated_at_idx on public.songs (updated_at, id);
create index songs_station_id_idx on public.songs (station_id);

create trigger stations_set_updated_at before update on public.stations
  for each row execute function public.set_updated_at();
create trigger songs_set_updated_at before update on public.songs
  for each row execute function public.set_updated_at();

alter table public.stations enable row level security;
alter table public.songs enable row level security;

create policy stations_select_anyone on public.stations for select using (true);
create policy stations_admin_insert on public.stations for insert with check (public.is_admin());
create policy stations_admin_update on public.stations for update using (public.is_admin()) with check (public.is_admin());
create policy stations_admin_delete on public.stations for delete using (public.is_admin());

create policy songs_select_anyone on public.songs for select using (true);
create policy songs_admin_insert on public.songs for insert with check (public.is_admin());
create policy songs_admin_update on public.songs for update using (public.is_admin()) with check (public.is_admin());
create policy songs_admin_delete on public.songs for delete using (public.is_admin());

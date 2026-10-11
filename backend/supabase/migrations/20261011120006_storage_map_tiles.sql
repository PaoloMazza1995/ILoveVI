-- map-tiles: public read (so the app can fetch tile images with no auth), admin-only write.
insert into storage.buckets (id, name, public)
values ('map-tiles', 'map-tiles', true)
on conflict (id) do nothing;

create policy map_tiles_select_anyone on storage.objects
  for select using (bucket_id = 'map-tiles');

create policy map_tiles_admin_insert on storage.objects
  for insert with check (bucket_id = 'map-tiles' and public.is_admin());

create policy map_tiles_admin_update on storage.objects
  for update using (bucket_id = 'map-tiles' and public.is_admin())
  with check (bucket_id = 'map-tiles' and public.is_admin());

create policy map_tiles_admin_delete on storage.objects
  for delete using (bucket_id = 'map-tiles' and public.is_admin());

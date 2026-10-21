-- entry-images: public read (so the app can fetch catalog images with no auth), admin-only write.
-- Same shape as 20261011120006_storage_map_tiles.sql's map-tiles bucket.
insert into storage.buckets (id, name, public)
values ('entry-images', 'entry-images', true)
on conflict (id) do nothing;

create policy entry_images_select_anyone on storage.objects
  for select using (bucket_id = 'entry-images');

create policy entry_images_admin_insert on storage.objects
  for insert with check (bucket_id = 'entry-images' and public.is_admin());

create policy entry_images_admin_update on storage.objects
  for update using (bucket_id = 'entry-images' and public.is_admin())
  with check (bucket_id = 'entry-images' and public.is_admin());

create policy entry_images_admin_delete on storage.objects
  for delete using (bucket_id = 'entry-images' and public.is_admin());

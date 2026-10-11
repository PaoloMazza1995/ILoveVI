-- News/missions RSS feed list (:core:model FeedSource). NewsItem itself is a device-local cache
-- per the functional analysis data model (section 8) and is not part of this backend.

create table public.feed_sources (
  id text primary key,
  name text not null,
  url text not null,
  topic text not null check (topic in ('NEWS', 'MISSIONS')),
  enabled boolean not null default true,
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);

create index feed_sources_updated_at_idx on public.feed_sources (updated_at, id);

create trigger feed_sources_set_updated_at before update on public.feed_sources
  for each row execute function public.set_updated_at();

alter table public.feed_sources enable row level security;

create policy feed_sources_select_anyone on public.feed_sources for select using (true);
create policy feed_sources_admin_insert on public.feed_sources for insert with check (public.is_admin());
create policy feed_sources_admin_update on public.feed_sources for update using (public.is_admin()) with check (public.is_admin());
create policy feed_sources_admin_delete on public.feed_sources for delete using (public.is_admin());

-- updated_at is bumped by trigger on every UPDATE; soft-deleted rows stay visible to SELECT so
-- the app's delta sync (updated_at > :lastSync) can learn about the deletion.
begin;
select plan(2);

insert into public.stations (id, name, updated_at)
values ('test-station-updated', 'Test Station', '2020-01-01T00:00:00+00');

with before_row as (
  select updated_at from public.stations where id = 'test-station-updated'
),
updated_row as (
  update public.stations set name = 'Test Station Renamed'
  where id = 'test-station-updated'
  returning updated_at
)
select ok(
  (select updated_at from updated_row) > (select updated_at from before_row),
  'updated_at changes after an update'
);

insert into public.stations (id, name, updated_at, deleted_at)
values ('test-station-deleted', '[Deleted Station]', now(), now());

set local role anon;

select isnt_empty(
  $$ select 1 from public.stations
     where id = 'test-station-deleted' and deleted_at is not null $$,
  'a soft-deleted row is still visible to a delta sync select'
);

reset role;

select * from finish();
rollback;

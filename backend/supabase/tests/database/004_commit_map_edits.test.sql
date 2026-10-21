-- commit_map_edits: admin can commit a new entry + pin together atomically; anonymous cannot;
-- a bad coordinate fails the whole call and saves nothing; retrying the same payload (same
-- client-generated ids) creates no duplicates.
begin;
select plan(10);

insert into public.map_config (map_version, source_image_url, width_px, height_px, tile_url_template, min_zoom, max_zoom)
values (9100, 'https://example.com/map.png', 4096, 2048, 'https://example.com/tiles/{z}/{x}/{y}.png', 0, 5);

-- Admin: a new weapon and its pin, saved together in one call.
select set_config(
  'request.jwt.claims',
  '{"role":"authenticated","app_metadata":{"role":"admin"}}',
  true
);
set local role authenticated;

select lives_ok(
  $$ select public.commit_map_edits('{
       "map_version": 9100,
       "entries": {"weapons": [{"id":"commit-weapon-1","name":"Commit Weapon","description":"A test weapon.","sub_type":"PISTOL"}]},
       "pins": [{"id":"commit-pin-1","category":"WEAPON","entry_id":"commit-weapon-1","x":0.5,"y":0.5,"map_version":9100}]
     }'::jsonb) $$,
  'admin can commit a new entry and its pin together'
);

select isnt_empty(
  $$ select 1 from public.weapons where id = 'commit-weapon-1' $$,
  'the new entry was saved'
);

select isnt_empty(
  $$ select 1 from public.map_pins where id = 'commit-pin-1' $$,
  'the new pin was saved'
);

-- Retrying the exact same payload (same ids) must not create duplicates.
select lives_ok(
  $$ select public.commit_map_edits('{
       "map_version": 9100,
       "entries": {"weapons": [{"id":"commit-weapon-1","name":"Commit Weapon","description":"A test weapon.","sub_type":"PISTOL"}]},
       "pins": [{"id":"commit-pin-1","category":"WEAPON","entry_id":"commit-weapon-1","x":0.5,"y":0.5,"map_version":9100}]
     }'::jsonb) $$,
  'retrying the same commit payload is safe'
);

select is(
  (select count(*) from public.weapons where id = 'commit-weapon-1')::int,
  1,
  'retry created no duplicate entry'
);

select is(
  (select count(*) from public.map_pins where id = 'commit-pin-1')::int,
  1,
  'retry created no duplicate pin'
);

reset role;

-- Anonymous cannot commit.
set local role anon;
select set_config('request.jwt.claims', '{"role":"anon"}', true);

select throws_like(
  $$ select public.commit_map_edits('{
       "map_version": 9100,
       "entries": {"weapons": [{"id":"commit-weapon-anon","name":"x","description":"y","sub_type":"PISTOL"}]},
       "pins": []
     }'::jsonb) $$,
  '%admin%',
  'anonymous commit is refused'
);

reset role;

-- Admin again: a bad coordinate (x > 1) must fail the whole call and save nothing from it.
select set_config(
  'request.jwt.claims',
  '{"role":"authenticated","app_metadata":{"role":"admin"}}',
  true
);
set local role authenticated;

select throws_like(
  $$ select public.commit_map_edits('{
       "map_version": 9100,
       "entries": {"weapons": [{"id":"commit-weapon-bad","name":"Bad Weapon","description":"d","sub_type":"PISTOL"}]},
       "pins": [{"id":"commit-pin-bad","category":"WEAPON","entry_id":"commit-weapon-bad","x":1.5,"y":0.5,"map_version":9100}]
     }'::jsonb) $$,
  '%constraint%',
  'a bad coordinate rejects the whole commit'
);

select is_empty(
  $$ select 1 from public.weapons where id = 'commit-weapon-bad' $$,
  'the entry from the failed commit was not saved'
);

select is_empty(
  $$ select 1 from public.map_pins where id = 'commit-pin-bad' $$,
  'the pin from the failed commit was not saved'
);

reset role;

select * from finish();
rollback;

-- map_pins: coordinates must be normalized (0..1), and entry_id must exist in the table matching
-- category (enforced by the validate_map_pin_entry trigger, since a plain FK can't target one of
-- three tables conditionally).
begin;
select plan(2);

insert into public.map_config (map_version, source_image_url, width_px, height_px, tile_url_template, min_zoom, max_zoom)
values (9001, 'https://example.com/map.png', 4096, 2048, 'https://example.com/tiles/{z}/{x}/{y}.png', 0, 5);

insert into public.weapons (id, name, description, sub_type)
values ('test-weapon-pin', 'Pin Weapon', 'A test weapon.', 'PISTOL');

select throws_like(
  $$ insert into public.map_pins (id, category, entry_id, x, y, map_version)
     values ('test-pin-badx', 'WEAPON', 'test-weapon-pin', 1.5, 0.5, 9001) $$,
  '%constraint%',
  'map_pins rejects x outside 0..1'
);

select throws_like(
  $$ insert into public.map_pins (id, category, entry_id, x, y, map_version)
     values ('test-pin-missing', 'WEAPON', 'does-not-exist', 0.5, 0.5, 9001) $$,
  '%not found%',
  'map_pins rejects a pin pointing to a missing entry'
);

select * from finish();
rollback;

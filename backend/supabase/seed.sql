-- Small, obviously-fake dataset for local tests and UI previews. Real content can only be
-- collected after the game releases (19 November 2026) — see Functional Analysis v0.3 section 12.

insert into public.weapons (id, name, description, sub_type, zone, price, ammo_type) values
  ('weapon-1', '[Weapon 1]', '[Placeholder description for Weapon 1.]', 'PISTOL', '[Zone 1]', 500, '9mm'),
  ('weapon-2', '[Weapon 2]', '[Placeholder description for Weapon 2.]', 'RIFLE', '[Zone 2]', 2500, '5.56mm');

insert into public.vehicles (id, name, description, sub_type, zone) values
  ('vehicle-1', '[Vehicle 1]', '[Placeholder description for Vehicle 1.]', 'CAR', '[Zone 1]'),
  ('vehicle-2', '[Vehicle 2]', '[Placeholder description for Vehicle 2.]', 'MOTORCYCLE', '[Zone 3]');

insert into public.items (id, name, description, sub_type, zone) values
  ('item-2', '[Item 2]', '[Placeholder description for Item 2.]', 'ARMOR', '[Zone 2]');

insert into public.map_config (map_version, source_image_url, width_px, height_px, tile_url_template, min_zoom, max_zoom) values
  (1, 'https://example.com/map/v1/source.png', 4096, 4096, 'https://example.com/map/v1/tiles/{z}/{x}/{y}.png', 0, 5);

insert into public.map_pins (id, category, entry_id, x, y, zone, map_version) values
  ('pin-weapon-1', 'WEAPON', 'weapon-1', 0.25, 0.40, '[Zone 1]', 1),
  ('pin-vehicle-1', 'VEHICLE', 'vehicle-1', 0.55, 0.65, '[Zone 3]', 1),
  ('pin-item-2', 'ITEM', 'item-2', 0.70, 0.20, '[Zone 2]', 1);

insert into public.stations (id, name) values
  ('station-1', '[Station 1]'),
  ('station-2', '[Station 2]');

insert into public.songs (id, title, artist, station_id) values
  ('song-1', '[Song 1]', '[Artist 1]', 'station-1'),
  ('song-2', '[Song 2]', '[Artist 2]', 'station-1'),
  ('song-3', '[Song 3]', '[Artist 3]', 'station-2');

insert into public.feed_sources (id, name, url, topic, enabled) values
  ('feed-1', '[Feed 1]', 'https://example.com/feed-1.xml', 'NEWS', true),
  ('feed-2', '[Feed 2]', 'https://example.com/feed-2.xml', 'MISSIONS', true),
  -- Real feed, added at the user's explicit request after confirming the trade-off: a Google
  -- News RSS search for "GTA 6"/"Grand Theft Auto VI", not an official/curated source — a
  -- keyword search can surface tangential results (opinion pieces, influencer takes), and this
  -- repo has not independently verified Google's terms of use for this kind of scheduled,
  -- programmatic consumption. The app fetches and parses this (and every other enabled feed_sources
  -- url) directly on-device — see `:core:data`'s `ContentSyncerImpl.syncNews()` and
  -- docs/ROADMAP.md's own notes for the full context.
  ('google-news-gta6', '[Google News - GTA 6]', 'https://news.google.com/rss/search?q=%22GTA+6%22+OR+%22Grand+Theft+Auto+VI%22&hl=en-US&gl=US&ceid=US:en', 'NEWS', true);

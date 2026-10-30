package com.pamaz.vi.core.data.seed

/**
 * A small, obviously-fake bundled dataset (one row each, same bracketed-placeholder naming
 * `backend/supabase/seed.sql` uses) so the app is never empty on a first launch with no network —
 * [SeedImporter] applies this once, then `ContentSyncer` takes over. Every id is prefixed
 * `seed-`, distinct from any real backend id, so a later real sync's upserts can never collide
 * with (or accidentally "update") a seed row by id.
 *
 * A plain compiled-in Kotlin string, not a platform resource file: this module has no Compose
 * dependency (so no `composeResources`), and this repo has no other established mechanism for
 * bundling a plain-text asset into a KMP library uniformly across Android/iOS/JVM — a committed
 * `.json` file read via a generated build-time constant (the same shape `:core:network`'s
 * `generateNetworkBuildConfig` uses for secrets) would be pure overhead for content that has no
 * secrecy requirement and never changes without a code change anyway.
 */
internal val SEED_JSON = """
{
  "map_config": [
    {
      "map_version": 1,
      "source_image_url": "https://example.com/seed-map/v1/source.png",
      "width_px": 1024,
      "height_px": 1024,
      "tile_url_template": "https://example.com/seed-map/v1/tiles/{z}/{x}/{y}.png",
      "min_zoom": 0,
      "max_zoom": 3,
      "updated_at": "2026-01-01T00:00:00Z"
    }
  ],
  "stations": [
    { "id": "seed-station-1", "name": "[Seed Station 1]", "updated_at": "2026-01-01T00:00:00Z" }
  ],
  "weapons": [
    {
      "id": "seed-weapon-1",
      "name": "[Seed Weapon 1]",
      "description": "[Placeholder seed weapon description.]",
      "sub_type": "PISTOL",
      "zone": "[Seed Zone 1]",
      "price": 100,
      "ammo_type": "9mm",
      "updated_at": "2026-01-01T00:00:00Z"
    }
  ],
  "vehicles": [
    {
      "id": "seed-vehicle-1",
      "name": "[Seed Vehicle 1]",
      "description": "[Placeholder seed vehicle description.]",
      "sub_type": "CAR",
      "zone": "[Seed Zone 1]",
      "updated_at": "2026-01-01T00:00:00Z"
    }
  ],
  "items": [
    {
      "id": "seed-item-1",
      "name": "[Seed Item 1]",
      "description": "[Placeholder seed item description.]",
      "sub_type": "ARMOR",
      "zone": "[Seed Zone 1]",
      "updated_at": "2026-01-01T00:00:00Z"
    }
  ],
  "songs": [
    {
      "id": "seed-song-1",
      "title": "[Seed Song 1]",
      "artist": "[Seed Artist 1]",
      "station_id": "seed-station-1",
      "updated_at": "2026-01-01T00:00:00Z"
    }
  ],
  "map_pins": [
    {
      "id": "seed-pin-1",
      "category": "WEAPON",
      "entry_id": "seed-weapon-1",
      "x": 0.5,
      "y": 0.5,
      "zone": "[Seed Zone 1]",
      "map_version": 1,
      "updated_at": "2026-01-01T00:00:00Z"
    }
  ]
}
""".trimIndent()

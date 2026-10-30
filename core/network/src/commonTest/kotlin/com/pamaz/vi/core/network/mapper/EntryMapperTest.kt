package com.pamaz.vi.core.network.mapper

import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.core.network.dto.VehicleDto
import com.pamaz.vi.core.network.dto.WeaponDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val json = Json { ignoreUnknownKeys = true }

/** Fixture JSON mimicking a real PostgREST `weapons` row (snake_case, optional columns present). */
private const val WEAPON_ROW_JSON = """
{
  "id": "weapon-1",
  "name": "Pistol",
  "description": "A sidearm.",
  "sub_type": "PISTOL",
  "zone": "Vice City",
  "image_url": "https://example.com/pistol.png",
  "attributes": {"magazine_size": "12"},
  "price": 250,
  "ammo_type": "9mm",
  "updated_at": "2026-01-01T00:00:00Z",
  "deleted_at": null
}
"""

/** Same row, every nullable column actually `null`/absent — the "nulls" fixture. */
private const val WEAPON_ROW_JSON_ALL_NULLS = """
{
  "id": "weapon-2",
  "name": "Knife",
  "description": "A blade.",
  "sub_type": "MELEE",
  "updated_at": "2026-01-02T00:00:00Z"
}
"""

/** Same row, soft-deleted — the "tombstone" fixture. */
private const val WEAPON_ROW_JSON_TOMBSTONE = """
{
  "id": "weapon-3",
  "name": "Rifle",
  "description": "A rifle.",
  "sub_type": "RIFLE",
  "updated_at": "2026-01-03T00:00:00Z",
  "deleted_at": "2026-01-04T00:00:00Z"
}
"""

/** Same row, an unrecognized `sub_type` the Kotlin enum doesn't know. */
private const val WEAPON_ROW_JSON_INVALID_SUBTYPE = """
{
  "id": "weapon-4",
  "name": "Mystery",
  "description": "???",
  "sub_type": "FLAMETHROWER",
  "updated_at": "2026-01-05T00:00:00Z"
}
"""

class EntryMapperTest {
    @Test
    fun weaponRow_withEveryField_mapsToWeapon() {
        val dto = json.decodeFromString<WeaponDto>(WEAPON_ROW_JSON)
        val change = dto.toChange()

        assertEquals("weapon-1", change.id)
        assertNull(change.deletedAt)
        val weapon = assertNotNull(change.item)
        assertEquals("Pistol", weapon.name)
        assertEquals(WeaponType.PISTOL, weapon.subType)
        assertEquals("Vice City", weapon.zone)
        assertEquals(250, weapon.price)
        assertEquals("9mm", weapon.ammoType)
        assertEquals(mapOf("magazine_size" to "12"), weapon.attributes)
    }

    @Test
    fun weaponRow_withNullOptionalColumns_mapsWithDefaults() {
        val dto = json.decodeFromString<WeaponDto>(WEAPON_ROW_JSON_ALL_NULLS)
        val change = dto.toChange()

        val weapon = assertNotNull(change.item)
        assertEquals(WeaponType.MELEE, weapon.subType)
        assertNull(weapon.zone)
        assertNull(weapon.imageUrl)
        assertNull(weapon.price)
        assertNull(weapon.ammoType)
        assertEquals(emptyMap(), weapon.attributes)
    }

    @Test
    fun weaponRow_softDeleted_becomesTombstoneWithNoItem() {
        val dto = json.decodeFromString<WeaponDto>(WEAPON_ROW_JSON_TOMBSTONE)
        val change = dto.toChange()

        assertEquals("weapon-3", change.id)
        assertNull(change.item)
        assertNotNull(change.deletedAt)
    }

    @Test
    fun weaponRow_withUnknownSubType_isUnmappableNotCrashing() {
        val dto = json.decodeFromString<WeaponDto>(WEAPON_ROW_JSON_INVALID_SUBTYPE)
        val change = dto.toChange()

        assertEquals("weapon-4", change.id)
        assertNull(change.item)
        assertNull(change.deletedAt)
    }

    @Test
    fun vehicleRow_mapsSubTypeAndAttributes() {
        val dto = VehicleDto(
            id = "vehicle-1",
            name = "Sedan",
            description = "A car.",
            subType = "CAR",
            attributes = mapOf("top_speed" to "180"),
            updatedAt = "2026-01-01T00:00:00Z",
        )
        val vehicle = assertNotNull(dto.toChange().item)
        assertEquals("Sedan", vehicle.name)
        assertEquals(mapOf("top_speed" to "180"), vehicle.attributes)
    }
}

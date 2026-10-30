package com.pamaz.vi.core.network.mapper

import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.network.dto.MapConfigDto
import com.pamaz.vi.core.network.dto.MapPinDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val json = Json { ignoreUnknownKeys = true }

/** Fixture JSON mimicking a real PostgREST `map_pins` row with a category the Kotlin `EntryCategory` enum doesn't know about — the "invalid category" fixture. */
private const val MAP_PIN_ROW_JSON_INVALID_CATEGORY = """
{
  "id": "pin-1",
  "category": "BUILDING",
  "entry_id": "weapon-1",
  "x": 0.5,
  "y": 0.5,
  "map_version": 1,
  "updated_at": "2026-01-01T00:00:00Z"
}
"""

private const val MAP_PIN_ROW_JSON_VALID = """
{
  "id": "pin-2",
  "category": "WEAPON",
  "entry_id": "weapon-1",
  "x": 0.25,
  "y": 0.75,
  "zone": null,
  "map_version": 1,
  "updated_at": "2026-01-01T00:00:00Z",
  "deleted_at": null
}
"""

class MapMapperTest {
    @Test
    fun mapPinRow_withKnownCategory_buildsMatchingEntryRef() {
        val dto = json.decodeFromString<MapPinDto>(MAP_PIN_ROW_JSON_VALID)
        val pin = assertNotNull(dto.toChange().item)
        val ref = assertIs<EntryRef.ToWeapon>(pin.entryRef)
        assertEquals("weapon-1", ref.id.value)
        assertEquals(0.25f, pin.position.x)
        assertEquals(0.75f, pin.position.y)
    }

    @Test
    fun mapPinRow_withInvalidCategory_isUnmappableNotCrashing() {
        val dto = json.decodeFromString<MapPinDto>(MAP_PIN_ROW_JSON_INVALID_CATEGORY)
        val change = dto.toChange()

        assertEquals("pin-1", change.id)
        assertNull(change.item)
        assertNull(change.deletedAt)
    }

    @Test
    fun mapPinRow_softDeleted_becomesTombstoneWithNoItem() {
        val dto = MapPinDto(
            id = "pin-3",
            category = "WEAPON",
            entryId = "weapon-1",
            x = 0.1f,
            y = 0.1f,
            mapVersion = 1,
            updatedAt = "2026-01-01T00:00:00Z",
            deletedAt = "2026-01-02T00:00:00Z",
        )
        val change = dto.toChange()
        assertNull(change.item)
        assertNotNull(change.deletedAt)
    }

    @Test
    fun mapConfigRow_usesMapVersionAsChangeId() {
        val dto = MapConfigDto(
            mapVersion = 7,
            sourceImageUrl = "https://example.com/map.png",
            widthPx = 4096,
            heightPx = 2048,
            tileUrlTemplate = "https://example.com/tiles/{z}/{x}/{y}.png",
            minZoom = 0,
            maxZoom = 5,
            updatedAt = "2026-01-01T00:00:00Z",
        )
        val change = dto.toChange()
        assertEquals("7", change.id)
        assertNotNull(change.item)
    }

    @Test
    fun mapConfigRow_withInvalidZoomRange_isUnmappableNotCrashing() {
        val dto = MapConfigDto(
            mapVersion = 8,
            sourceImageUrl = "https://example.com/map.png",
            widthPx = 4096,
            heightPx = 2048,
            tileUrlTemplate = "https://example.com/tiles/{z}/{x}/{y}.png",
            minZoom = 5,
            maxZoom = 0,
            updatedAt = "2026-01-01T00:00:00Z",
        )
        val change = dto.toChange()
        assertEquals("8", change.id)
        assertNull(change.item)
        assertNull(change.deletedAt)
    }
}

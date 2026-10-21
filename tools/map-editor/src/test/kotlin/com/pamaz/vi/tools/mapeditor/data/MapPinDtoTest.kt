package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.WeaponId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/** "Upsert mapping to the backend DTO" (acceptance). */
class MapPinDtoTest {

    @Test
    fun toUpsertRow_mapsEveryColumn_andAlwaysSendsDeletedAtAsNull() {
        val pin = MapPin(
            id = MapPinId("ignored-stale-id"),
            entryRef = EntryRef.ToWeapon(WeaponId("w1")),
            position = NormalizedPoint(0.1234f, 0.5678f),
            zone = "Vinewood",
            mapVersion = 3,
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )

        val row = pin.toUpsertRow()

        assertEquals("weapon-w1", row.id)
        assertEquals("WEAPON", row.category)
        assertEquals("w1", row.entryId)
        assertEquals(0.1234f, row.x)
        assertEquals(0.5678f, row.y)
        assertEquals("Vinewood", row.zone)
        assertEquals(3, row.mapVersion)
        assertNull(row.deletedAt)
    }

    @Test
    fun toMapPin_isTheInverseOfToUpsertRow_forNormalizedPointAndEntryRef() {
        val row = MapPinRow(
            id = "item-i1",
            category = "ITEM",
            entryId = "i1",
            x = 0.42f,
            y = 0.17f,
            zone = "Zone 2",
            mapVersion = 1,
            updatedAt = "2026-01-01T00:00:00Z",
        )

        val pin = row.toMapPin()

        assertEquals(NormalizedPoint(0.42f, 0.17f), pin?.position)
        assertEquals("i1", (pin?.entryRef as? EntryRef.ToItem)?.id?.value)
        assertEquals("Zone 2", pin?.zone)
        assertEquals(1, pin?.mapVersion)
    }

    @Test
    fun pinRowId_isDeterministic_fromCategoryAndEntryId() {
        assertEquals("vehicle-v1", pinRowId("VEHICLE", "v1"))
        assertEquals(pinRowId("VEHICLE", "v1"), pinRowId("vehicle", "v1"))
    }
}

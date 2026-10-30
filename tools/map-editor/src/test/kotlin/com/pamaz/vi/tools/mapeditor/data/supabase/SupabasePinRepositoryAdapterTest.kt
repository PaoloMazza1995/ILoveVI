package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.WeaponId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class SupabasePinRepositoryAdapterTest {
    @Test
    fun mapPinMapsToDbPinPreservingCategoryAndRawEntryId() {
        val pin = MapPin(
            id = MapPinId("pin-1"),
            entryRef = EntryRef.ToWeapon(WeaponId("weapon-7")),
            position = NormalizedPoint(0.25f, 0.75f),
            zone = "Vice Beach",
            mapVersion = 3,
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )

        val dbPin = pin.toDbPin()

        assertEquals("pin-1", dbPin.id)
        assertEquals(EntryCategory.WEAPON, dbPin.category)
        assertEquals("weapon-7", dbPin.entryId)
        assertEquals(NormalizedPoint(0.25f, 0.75f), dbPin.position)
        assertEquals("Vice Beach", dbPin.zone)
    }
}

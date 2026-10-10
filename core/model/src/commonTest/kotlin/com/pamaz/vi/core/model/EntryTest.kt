package com.pamaz.vi.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class EntryTest {
    private val now = kotlin.time.Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun weaponExposesWeaponCategoryAndStringEntryId() {
        val entry: Entry = Weapon(
            id = WeaponId("w1"),
            name = "Pistol",
            description = "A handgun.",
            subType = WeaponType.PISTOL,
            updatedAt = now,
        )
        assertEquals(EntryCategory.WEAPON, entry.category)
        assertEquals("w1", entry.entryId)
    }

    @Test
    fun vehicleExposesVehicleCategoryAndStringEntryId() {
        val entry: Entry = Vehicle(
            id = VehicleId("v1"),
            name = "Sedan",
            description = "A city car.",
            subType = VehicleType.CAR,
            updatedAt = now,
        )
        assertEquals(EntryCategory.VEHICLE, entry.category)
        assertEquals("v1", entry.entryId)
    }

    @Test
    fun itemExposesItemCategoryAndStringEntryId() {
        val entry: Entry = Item(
            id = ItemId("i1"),
            name = "Medkit",
            description = "Restores health.",
            subType = ItemType.HEALTH,
            updatedAt = now,
        )
        assertEquals(EntryCategory.ITEM, entry.category)
        assertEquals("i1", entry.entryId)
    }
}

package com.pamaz.vi.design.components

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * `EntryCategory.icon()`/`WeaponType.icon()`/`VehicleType.icon()`/`ItemType.icon()`/`Entry.icon()`
 * are all written as a `when` with no `else` branch, so the compiler itself already enforces
 * exhaustiveness (a new enum value fails the build, per EntryIcons.kt's own KDoc) — what these
 * tests check instead is the one thing the compiler can't: that every value maps to the *right*
 * icon, not just *some* icon.
 */
class EntryIconsTest {

    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun entryCategory_mapsToItsCategoryIcon() {
        assertEquals(IloveViIcon.PISTOL, EntryCategory.WEAPON.icon())
        assertEquals(IloveViIcon.CAR, EntryCategory.VEHICLE.icon())
        assertEquals(IloveViIcon.BOX, EntryCategory.ITEM.icon())
    }

    @Test
    fun weaponType_mapsToItsOwnIcon() {
        assertEquals(IloveViIcon.PISTOL, WeaponType.PISTOL.icon())
        assertEquals(IloveViIcon.RIFLE, WeaponType.RIFLE.icon())
        assertEquals(IloveViIcon.MACHINE_GUN, WeaponType.MACHINE_GUN.icon())
        assertEquals(IloveViIcon.MELEE, WeaponType.MELEE.icon())
        assertEquals(IloveViIcon.EXPLOSIVE, WeaponType.EXPLOSIVE.icon())
    }

    @Test
    fun vehicleType_mapsToItsOwnIcon() {
        assertEquals(IloveViIcon.CAR, VehicleType.CAR.icon())
        assertEquals(IloveViIcon.MOTORCYCLE, VehicleType.MOTORCYCLE.icon())
        assertEquals(IloveViIcon.BOAT, VehicleType.BOAT.icon())
        assertEquals(IloveViIcon.HELICOPTER, VehicleType.HELICOPTER.icon())
        assertEquals(IloveViIcon.PLANE, VehicleType.PLANE.icon())
    }

    @Test
    fun itemType_mapsToItsOwnIcon() {
        assertEquals(IloveViIcon.AMMO, ItemType.AMMO.icon())
        assertEquals(IloveViIcon.ARMOR, ItemType.ARMOR.icon())
    }

    @Test
    fun entry_icon_isItsSubTypesIcon_forEveryConcreteEntryType() {
        val weapon = Weapon(
            id = WeaponId("w1"), name = "Pistol", description = "A reliable handgun.",
            subType = WeaponType.RIFLE, updatedAt = now,
        )
        val vehicle = Vehicle(
            id = VehicleId("v1"), name = "Sedan", description = "A city car.",
            subType = VehicleType.BOAT, updatedAt = now,
        )
        val item = Item(
            id = ItemId("i1"), name = "Armor Vest", description = "Restores armor.",
            subType = ItemType.ARMOR, updatedAt = now,
        )

        assertEquals(IloveViIcon.RIFLE, weapon.icon())
        assertEquals(IloveViIcon.BOAT, vehicle.icon())
        assertEquals(IloveViIcon.ARMOR, item.icon())
    }
}

package com.pamaz.vi.tools.mapeditor.model

import com.pamaz.vi.core.model.EntryCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The single real-column-driven "extra fields" definition (acceptance: "entry form validation"). */
class EntryFieldSpecTest {

    @Test
    fun extraFieldsFor_weapon_isPriceAndAmmoType() {
        val keys = EntryFields.extraFieldsFor(EntryCategory.WEAPON).map { it.key }
        assertEquals(listOf(EntryFields.PRICE, EntryFields.AMMO_TYPE), keys)
    }

    @Test
    fun extraFieldsFor_vehicleAndItem_areEmpty_noPlaceholderColumnsThatDontExist() {
        assertTrue(EntryFields.extraFieldsFor(EntryCategory.VEHICLE).isEmpty())
        assertTrue(EntryFields.extraFieldsFor(EntryCategory.ITEM).isEmpty())
    }

    @Test
    fun validateExtra_acceptsABlankOptionalPrice() {
        assertEquals(emptyList(), EntryFields.validateExtra(EntryCategory.WEAPON, emptyMap()))
        assertEquals(
            emptyList(),
            EntryFields.validateExtra(EntryCategory.WEAPON, mapOf(EntryFields.PRICE to "")),
        )
    }

    @Test
    fun validateExtra_rejectsANonIntegerPrice() {
        assertEquals(
            listOf(EntryFields.PRICE),
            EntryFields.validateExtra(
                EntryCategory.WEAPON,
                mapOf(EntryFields.PRICE to "not-a-number")
            ),
        )
    }

    @Test
    fun validateExtra_acceptsAValidPrice() {
        assertEquals(
            emptyList(),
            EntryFields.validateExtra(EntryCategory.WEAPON, mapOf(EntryFields.PRICE to "500"))
        )
    }

    @Test
    fun subTypes_matchTheRealCoreModelEnums() {
        assertEquals(
            com.pamaz.vi.core.model.WeaponType.entries.map { it.name },
            EntryCategory.WEAPON.subTypes(),
        )
        assertEquals(
            com.pamaz.vi.core.model.VehicleType.entries.map { it.name },
            EntryCategory.VEHICLE.subTypes(),
        )
        assertEquals(
            com.pamaz.vi.core.model.ItemType.entries.map { it.name },
            EntryCategory.ITEM.subTypes(),
        )
    }
}

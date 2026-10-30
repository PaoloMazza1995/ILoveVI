package com.pamaz.vi.tools.mapeditor.ui

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.design.IloveViCategory
import com.pamaz.vi.design.components.IloveViIcon
import com.pamaz.vi.design.components.IloveViMapPinCategory
import com.pamaz.vi.design.components.icon

internal fun EntryCategory.toPinCategory(): IloveViMapPinCategory = when (this) {
    EntryCategory.WEAPON -> IloveViMapPinCategory.Weapon
    EntryCategory.VEHICLE -> IloveViMapPinCategory.Vehicle
    EntryCategory.ITEM -> IloveViMapPinCategory.Item
}

internal fun EntryCategory.toDesignCategory(): IloveViCategory = when (this) {
    EntryCategory.WEAPON -> IloveViCategory.Weapons
    EntryCategory.VEHICLE -> IloveViCategory.Vehicles
    EntryCategory.ITEM -> IloveViCategory.Items
}

/** The real sub-type icon when known, falling back to [category]'s generic one — never throws on a stale/unknown [subType] string. */
internal fun iconFor(category: EntryCategory, subType: String?): IloveViIcon {
    if (subType.isNullOrBlank()) return category.icon()
    return try {
        when (category) {
            EntryCategory.WEAPON -> WeaponType.valueOf(subType).icon()
            EntryCategory.VEHICLE -> VehicleType.valueOf(subType).icon()
            EntryCategory.ITEM -> ItemType.valueOf(subType).icon()
        }
    } catch (e: IllegalArgumentException) {
        category.icon()
    }
}

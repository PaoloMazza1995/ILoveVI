package com.pamaz.vi.tools.mapeditor.model

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.WeaponType

/** The real `core:model` sub-type enum values for [this] category, as raw names — drives the sub-type chip row and picks a sensible default when the active category changes. */
fun EntryCategory.subTypes(): List<String> = when (this) {
    EntryCategory.WEAPON -> WeaponType.entries.map { it.name }
    EntryCategory.VEHICLE -> VehicleType.entries.map { it.name }
    EntryCategory.ITEM -> ItemType.entries.map { it.name }
}

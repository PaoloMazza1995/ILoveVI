package com.pamaz.vi.core.model

import kotlinx.serialization.Serializable

/**
 * The three map/catalog categories: Weapons, Vehicles and Items. The UI maps each value to a
 * color (pink/orange/teal); that mapping is a presentation concern and intentionally lives
 * outside this module.
 */
@Serializable
enum class EntryCategory {
    /** Firearms and melee weapons ([Weapon]). */
    WEAPON,

    /** Cars, motorcycles, boats, helicopters and planes ([Vehicle]). */
    VEHICLE,

    /** Health, ammo and armor pickups ([Item]). */
    ITEM,
}

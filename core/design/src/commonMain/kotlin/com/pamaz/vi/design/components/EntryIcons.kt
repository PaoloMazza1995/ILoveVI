package com.pamaz.vi.design.components

import com.pamaz.vi.core.model.Entry
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponType

/**
 * Maps core:model's category/sub-type enums to the icon set in `IloveViIcons.kt` (step I). Every
 * `when` below is exhaustive (no `else`) on purpose: a new enum value in `:core:model` must fail
 * this module's build, not silently fall back to the wrong icon.
 *
 * [PISTOL][IloveViIcon.PISTOL]/[CAR][IloveViIcon.CAR]/[BOX][IloveViIcon.BOX] double as both a
 * sub-type icon (Pistol/Car) and the generic *category* icon ([EntryCategory.icon]) — there is no
 * separate "weapon in general"/"vehicle in general" glyph.
 */
fun EntryCategory.icon(): IloveViIcon = when (this) {
    EntryCategory.WEAPON -> IloveViIcon.PISTOL
    EntryCategory.VEHICLE -> IloveViIcon.CAR
    EntryCategory.ITEM -> IloveViIcon.BOX
}

/** One icon per [WeaponType] value. */
fun WeaponType.icon(): IloveViIcon = when (this) {
    WeaponType.PISTOL -> IloveViIcon.PISTOL
    WeaponType.RIFLE -> IloveViIcon.RIFLE
    WeaponType.MACHINE_GUN -> IloveViIcon.MACHINE_GUN
    WeaponType.MELEE -> IloveViIcon.MELEE
    WeaponType.EXPLOSIVE -> IloveViIcon.EXPLOSIVE
}

/** One icon per [VehicleType] value. */
fun VehicleType.icon(): IloveViIcon = when (this) {
    VehicleType.CAR -> IloveViIcon.CAR
    VehicleType.MOTORCYCLE -> IloveViIcon.MOTORCYCLE
    VehicleType.BOAT -> IloveViIcon.BOAT
    VehicleType.HELICOPTER -> IloveViIcon.HELICOPTER
    VehicleType.PLANE -> IloveViIcon.PLANE
}

/** One icon per [ItemType] value. */
fun ItemType.icon(): IloveViIcon = when (this) {
    ItemType.AMMO -> IloveViIcon.AMMO
    ItemType.ARMOR -> IloveViIcon.ARMOR
}

/**
 * The sub-type icon of any [Entry] — [Weapon.subType], [Vehicle.subType] or [Item.subType]'s own
 * icon, whichever concrete type [this] is. `:core:model`'s [Entry] is a sealed interface with
 * exactly these three implementations, so this `when` is exhaustive without `else` too, even
 * though it's written in a different module (`:core:design`) than the one that declares [Entry].
 */
fun Entry.icon(): IloveViIcon = when (this) {
    is Weapon -> subType.icon()
    is Vehicle -> subType.icon()
    is Item -> subType.icon()
}

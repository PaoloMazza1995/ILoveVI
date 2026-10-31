package com.pamaz.vi.design.components

import com.pamaz.vi.core.model.Entry
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.design.IloveViCategory
import ilovevi.core.design.generated.resources.Res
import ilovevi.core.design.generated.resources.design_subtype_ammo
import ilovevi.core.design.generated.resources.design_subtype_armor
import ilovevi.core.design.generated.resources.design_subtype_boat
import ilovevi.core.design.generated.resources.design_subtype_car
import ilovevi.core.design.generated.resources.design_subtype_explosive
import ilovevi.core.design.generated.resources.design_subtype_helicopter
import ilovevi.core.design.generated.resources.design_subtype_machine_gun
import ilovevi.core.design.generated.resources.design_subtype_melee
import ilovevi.core.design.generated.resources.design_subtype_motorcycle
import ilovevi.core.design.generated.resources.design_subtype_pistol
import ilovevi.core.design.generated.resources.design_subtype_plane
import ilovevi.core.design.generated.resources.design_subtype_rifle
import org.jetbrains.compose.resources.StringResource

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

/**
 * The [IloveViCategory] (color + — unlocalized — label) matching a `:core:model` [EntryCategory] —
 * added for `:feature:catalog`'s chips/badges/thumbnails, which all need this module's category
 * color but only ever carry the `:core:model` enum, never [IloveViCategory] itself.
 */
fun EntryCategory.toIloveViCategory(): IloveViCategory = when (this) {
    EntryCategory.WEAPON -> IloveViCategory.Weapons
    EntryCategory.VEHICLE -> IloveViCategory.Vehicles
    EntryCategory.ITEM -> IloveViCategory.Items
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

/**
 * The same 12 sub-type name strings `IloveViIcon`'s own `contentDescription` uses (step I),
 * exposed here as the human-readable *display* label a feature screen renders next to the icon
 * (`:feature:catalog`'s "type - zone" row subtitle and attributes table) — not duplicated as a
 * second set of strings, since they're already exactly these words.
 */
fun WeaponType.labelRes(): StringResource = when (this) {
    WeaponType.PISTOL -> Res.string.design_subtype_pistol
    WeaponType.RIFLE -> Res.string.design_subtype_rifle
    WeaponType.MACHINE_GUN -> Res.string.design_subtype_machine_gun
    WeaponType.MELEE -> Res.string.design_subtype_melee
    WeaponType.EXPLOSIVE -> Res.string.design_subtype_explosive
}

/** One label resource per [VehicleType] value — see [WeaponType.labelRes]'s KDoc. */
fun VehicleType.labelRes(): StringResource = when (this) {
    VehicleType.CAR -> Res.string.design_subtype_car
    VehicleType.MOTORCYCLE -> Res.string.design_subtype_motorcycle
    VehicleType.BOAT -> Res.string.design_subtype_boat
    VehicleType.HELICOPTER -> Res.string.design_subtype_helicopter
    VehicleType.PLANE -> Res.string.design_subtype_plane
}

/** One label resource per [ItemType] value — see [WeaponType.labelRes]'s KDoc. */
fun ItemType.labelRes(): StringResource = when (this) {
    ItemType.AMMO -> Res.string.design_subtype_ammo
    ItemType.ARMOR -> Res.string.design_subtype_armor
}

/** The sub-type label resource of any [Entry] — see [Entry.icon]'s KDoc for why this `when` is exhaustive without `else`. */
fun Entry.labelRes(): StringResource = when (this) {
    is Weapon -> subType.labelRes()
    is Vehicle -> subType.labelRes()
    is Item -> subType.labelRes()
}

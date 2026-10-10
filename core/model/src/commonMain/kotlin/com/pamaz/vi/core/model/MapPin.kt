package com.pamaz.vi.core.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Typed id of a [MapPin]. Serializes as a plain JSON string, not as an object. */
@JvmInline
@Serializable
value class MapPinId(val value: String)

/**
 * Points a [MapPin] at the [Weapon], [Vehicle] or [Item] it marks.
 *
 * A sealed hierarchy was chosen over a raw `(category: EntryCategory, entryId: String)` pair so
 * the category and the typed id can never disagree (e.g. category `WEAPON` paired with a
 * [VehicleId]): the compiler enforces the pairing by construction, and [category] is derived
 * from the branch instead of being a second, independently-settable field that could drift out
 * of sync with the id it's supposed to describe.
 */
@Serializable
sealed interface EntryRef {
    /** The category this reference belongs to, derived from which [Entry] type it points to. */
    val category: EntryCategory

    /** Points at a [Weapon]. */
    @Serializable
    data class ToWeapon(val id: WeaponId) : EntryRef {
        override val category: EntryCategory get() = EntryCategory.WEAPON
    }

    /** Points at a [Vehicle]. */
    @Serializable
    data class ToVehicle(val id: VehicleId) : EntryRef {
        override val category: EntryCategory get() = EntryCategory.VEHICLE
    }

    /** Points at an [Item]. */
    @Serializable
    data class ToItem(val id: ItemId) : EntryRef {
        override val category: EntryCategory get() = EntryCategory.ITEM
    }
}

/**
 * A marker on the map for one located [Weapon], [Vehicle] or [Item]. There is at most one
 * [MapPin] per located entry; an entry with no known coordinates simply has no [MapPin] row.
 *
 * The map shows no user/player position in this version: there is deliberately no location,
 * permission or "current position" concept anywhere in this module.
 */
@Serializable
data class MapPin(
    val id: MapPinId,
    /** Which entry this pin marks, and its category — see [EntryRef]'s KDoc for why this isn't a raw `entryId: String`. */
    val entryRef: EntryRef,
    val position: NormalizedPoint,
    /** In-game area/location name, when known. */
    val zone: String? = null,
    /** Last time this pin's data changed, used for delta sync. */
    val updatedAt: kotlin.time.Instant,
) {
    /** Forwards [EntryRef.category], so callers don't need to branch on [entryRef] just for this. */
    val category: EntryCategory get() = entryRef.category
}

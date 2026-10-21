package com.pamaz.vi.tools.mapeditor.model

import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.WeaponId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of the CSV/JSON import-export format (PLT-06): `category, entry_id, x, y, zone`. The
 * offline local-file format (PLT-09) is the same shape plus [mapVersion], since there's no
 * `map_config` row to supply it implicitly when working from a local image.
 *
 * Plain strings/floats here, not [EntryRef]/[com.pamaz.vi.core.model.NormalizedPoint] directly:
 * this is the literal on-disk/wire shape, which may be malformed (unknown category, out-of-range
 * x/y) — [com.pamaz.vi.tools.mapeditor.`import`.ImportValidator] is what turns a list of these
 * into trusted domain values, never this class's own `init`.
 */
@Serializable
data class PinRow(
    val category: String,
    @SerialName("entry_id") val entryId: String,
    val x: Float,
    val y: Float,
    val zone: String? = null,
    @SerialName("map_version") val mapVersion: Int? = null,
)

/** [EntryRef.category]'s backend/CSV spelling, e.g. `EntryCategory.WEAPON.name == "WEAPON"`. */
fun EntryRef.toPinRowCategory(): String = category.name

/** The raw id string inside any [EntryRef] branch, regardless of which one. */
fun EntryRef.rawEntryId(): String = when (this) {
    is EntryRef.ToWeapon -> id.value
    is EntryRef.ToVehicle -> id.value
    is EntryRef.ToItem -> id.value
}

/** Builds an [EntryRef] from a row's [PinRow.category]/[PinRow.entryId], or `null` if the category isn't one of WEAPON/VEHICLE/ITEM. */
fun PinRow.toEntryRefOrNull(): EntryRef? = when (category.trim().uppercase()) {
    "WEAPON" -> EntryRef.ToWeapon(WeaponId(entryId))
    "VEHICLE" -> EntryRef.ToVehicle(VehicleId(entryId))
    "ITEM" -> EntryRef.ToItem(ItemId(entryId))
    else -> null
}

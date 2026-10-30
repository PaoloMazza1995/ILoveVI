package com.pamaz.vi.core.network.mapper

import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.dto.MapConfigDto
import com.pamaz.vi.core.network.dto.MapPinDto
import kotlin.time.Instant

/** `map_pins.category` + `entry_id` → [EntryRef], the one place this module builds that sealed type from raw strings. `null` for any category outside the three known ones. */
internal fun categoryAndEntryIdToEntryRef(category: String, entryId: String): EntryRef? =
    when (category) {
        "WEAPON" -> EntryRef.ToWeapon(WeaponId(entryId))
        "VEHICLE" -> EntryRef.ToVehicle(VehicleId(entryId))
        "ITEM" -> EntryRef.ToItem(ItemId(entryId))
        else -> null
    }

/** Maps a `map_config` row. [Change.id] is [MapConfigDto.mapVersion] as a string, since this table's primary key is an integer, not a text `id`. An invalid row (bad `tile_url_template`, non-positive size, `min_zoom > max_zoom` — [MapConfig]'s own `require`s) is unmappable — see [rowChange]. */
internal fun MapConfigDto.toChange(): Change<MapConfig> =
    rowChange(mapVersion.toString(), deletedAt) {
        MapConfig(
            mapVersion = mapVersion,
            sourceImageUrl = sourceImageUrl,
            widthPx = widthPx,
            heightPx = heightPx,
            tileUrlTemplate = tileUrlTemplate,
            minZoom = minZoom,
            maxZoom = maxZoom,
            updatedAt = Instant.parse(updatedAt),
        )
    }

/** Maps a `map_pins` row. An unrecognized [MapPinDto.category] makes the row unmappable — see [rowChange] and [categoryAndEntryIdToEntryRef]. */
internal fun MapPinDto.toChange(): Change<MapPin> = rowChange(id, deletedAt) {
    val entryRef = categoryAndEntryIdToEntryRef(category, entryId)
        ?: error("Unknown map_pins.category \"$category\" for pin \"$id\".")
    MapPin(
        id = MapPinId(id),
        entryRef = entryRef,
        position = NormalizedPoint(x, y),
        zone = zone,
        mapVersion = mapVersion,
        updatedAt = Instant.parse(updatedAt),
    )
}

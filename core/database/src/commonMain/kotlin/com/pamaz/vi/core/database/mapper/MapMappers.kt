package com.pamaz.vi.core.database.mapper

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.WeaponId
import kotlin.time.Instant

/** A [MapPin] enriched with the name of the entry it marks, as produced by the per-category pin+entry joins in MapPins.sq. */
internal data class MapPinWithEntry(val pin: MapPin, val entryName: String)

private fun entryRefFor(category: EntryCategory, entryId: String): EntryRef = when (category) {
    EntryCategory.WEAPON -> EntryRef.ToWeapon(WeaponId(entryId))
    EntryCategory.VEHICLE -> EntryRef.ToVehicle(VehicleId(entryId))
    EntryCategory.ITEM -> EntryRef.ToItem(ItemId(entryId))
}

internal fun mapPinRowToMapPin(
    id: String,
    category: String,
    entryId: String,
    x: Double,
    y: Double,
    zone: String?,
    mapVersion: Long,
    updatedAt: Long,
    deletedAt: Long?,
): MapPin = MapPin(
    id = MapPinId(id),
    entryRef = entryRefFor(EntryCategory.valueOf(category), entryId),
    position = NormalizedPoint(x.toFloat(), y.toFloat()),
    zone = zone,
    mapVersion = mapVersion.toInt(),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

internal fun pinJoinRowToMapPinWithEntry(
    id: String,
    category: String,
    entryId: String,
    x: Double,
    y: Double,
    zone: String?,
    mapVersion: Long,
    updatedAt: Long,
    deletedAt: Long?,
    entryName: String,
    entrySubType: String,
): MapPinWithEntry = MapPinWithEntry(
    pin = mapPinRowToMapPin(id, category, entryId, x, y, zone, mapVersion, updatedAt, deletedAt),
    entryName = entryName,
)

internal fun mapConfigRowToMapConfig(
    mapVersion: Long,
    sourceImageUrl: String,
    widthPx: Long,
    heightPx: Long,
    tileUrlTemplate: String,
    minZoom: Long,
    maxZoom: Long,
    updatedAt: Long,
    deletedAt: Long?,
): MapConfig = MapConfig(
    mapVersion = mapVersion.toInt(),
    sourceImageUrl = sourceImageUrl,
    widthPx = widthPx.toInt(),
    heightPx = heightPx.toInt(),
    tileUrlTemplate = tileUrlTemplate,
    minZoom = minZoom.toInt(),
    maxZoom = maxZoom.toInt(),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.tools.mapeditor.model.rawEntryId
import com.pamaz.vi.tools.mapeditor.model.toEntryRefOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** The `map_pins` row shape for reads — `backend/supabase/migrations/20261011120004_map.sql`. */
@Serializable
data class MapPinRow(
    val id: String,
    val category: String,
    @SerialName("entry_id") val entryId: String,
    val x: Float,
    val y: Float,
    val zone: String? = null,
    @SerialName("map_version") val mapVersion: Int,
    @SerialName("updated_at") val updatedAt: String,
)

/** The upsert payload — every column explicit (no defaults) so `deleted_at` is always sent, even as `null`: a default-valued property equal to its default is dropped by `kotlinx.serialization.json`'s `encodeDefaults = false`, which would silently fail to *reactivate* a previously soft-deleted pin on re-import. */
@Serializable
data class MapPinUpsertRow(
    val id: String,
    val category: String,
    @SerialName("entry_id") val entryId: String,
    val x: Float,
    val y: Float,
    val zone: String?,
    @SerialName("map_version") val mapVersion: Int,
    @SerialName("deleted_at") val deletedAt: String?,
)

/** `map_pins.id` isn't server-generated (`text primary key`, no default) — this tool derives a stable one from the natural key `(category, entry_id)`, matching the unique constraint already on that pair. */
fun pinRowId(category: String, entryId: String): String = "${category.lowercase()}-${entryId}"

fun MapPinRow.toMapPin(): MapPin? {
    val entryRef =
        com.pamaz.vi.tools.mapeditor.model.PinRow(category, entryId, x, y, zone).toEntryRefOrNull()
            ?: return null
    return MapPin(
        id = MapPinId(id),
        entryRef = entryRef,
        position = NormalizedPoint(x, y),
        zone = zone,
        mapVersion = mapVersion,
        updatedAt = Instant.parse(updatedAt),
    )
}

fun MapPin.toUpsertRow(): MapPinUpsertRow = MapPinUpsertRow(
    id = pinRowId(entryRef.category.name, entryRef.rawEntryId()),
    category = entryRef.category.name,
    entryId = entryRef.rawEntryId(),
    x = position.x,
    y = position.y,
    zone = zone,
    mapVersion = mapVersion,
    deletedAt = null,
)

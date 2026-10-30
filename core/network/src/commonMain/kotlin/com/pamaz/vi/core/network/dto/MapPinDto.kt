package com.pamaz.vi.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The `map_pins` row shape — `backend/supabase/migrations/20261011120004_map.sql`. [category]/[entryId] become an `EntryRef` in the mapper, not here (see `MapMappers.kt`). */
@Serializable
internal data class MapPinDto(
    val id: String,
    val category: String,
    @SerialName("entry_id") val entryId: String,
    val x: Float,
    val y: Float,
    val zone: String? = null,
    @SerialName("map_version") val mapVersion: Int,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

package com.pamaz.vi.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The `stations` row shape — `backend/supabase/migrations/20261011120003_stations_and_songs.sql`. */
@Serializable
internal data class StationDto(
    val id: String,
    val name: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

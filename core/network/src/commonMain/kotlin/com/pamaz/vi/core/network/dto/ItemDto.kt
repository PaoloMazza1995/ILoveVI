package com.pamaz.vi.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The `items` row shape — `backend/supabase/migrations/20261011120002_weapons_vehicles_items.sql`. */
@Serializable
internal data class ItemDto(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("sub_type") val subType: String,
    val zone: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

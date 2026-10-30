package com.pamaz.vi.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The `map_config` row shape — `backend/supabase/migrations/20261011120004_map.sql`. Primary key is [mapVersion], not a text `id`. */
@Serializable
internal data class MapConfigDto(
    @SerialName("map_version") val mapVersion: Int,
    @SerialName("source_image_url") val sourceImageUrl: String,
    @SerialName("width_px") val widthPx: Int,
    @SerialName("height_px") val heightPx: Int,
    @SerialName("tile_url_template") val tileUrlTemplate: String,
    @SerialName("min_zoom") val minZoom: Int,
    @SerialName("max_zoom") val maxZoom: Int,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

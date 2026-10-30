package com.pamaz.vi.core.data.seed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [SEED_JSON]'s root shape and one row DTO per table — a small, self-contained set, not reused
 * from `:core:network`'s own DTOs (those are `internal` to that module, and this seed's shape is
 * fully under this module's own control anyway, so there is nothing to keep in sync).
 */
@Serializable
internal data class SeedPayload(
    @SerialName("map_config") val mapConfig: List<SeedMapConfigDto> = emptyList(),
    val stations: List<SeedStationDto> = emptyList(),
    val weapons: List<SeedWeaponDto> = emptyList(),
    val vehicles: List<SeedVehicleDto> = emptyList(),
    val items: List<SeedItemDto> = emptyList(),
    val songs: List<SeedSongDto> = emptyList(),
    @SerialName("map_pins") val mapPins: List<SeedMapPinDto> = emptyList(),
)

@Serializable
internal data class SeedMapConfigDto(
    @SerialName("map_version") val mapVersion: Int,
    @SerialName("source_image_url") val sourceImageUrl: String,
    @SerialName("width_px") val widthPx: Int,
    @SerialName("height_px") val heightPx: Int,
    @SerialName("tile_url_template") val tileUrlTemplate: String,
    @SerialName("min_zoom") val minZoom: Int,
    @SerialName("max_zoom") val maxZoom: Int,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
internal data class SeedStationDto(
    val id: String,
    val name: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
internal data class SeedWeaponDto(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("sub_type") val subType: String,
    val zone: String? = null,
    val price: Int? = null,
    @SerialName("ammo_type") val ammoType: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
internal data class SeedVehicleDto(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("sub_type") val subType: String,
    val zone: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
internal data class SeedItemDto(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("sub_type") val subType: String,
    val zone: String? = null,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
internal data class SeedSongDto(
    val id: String,
    val title: String,
    val artist: String,
    @SerialName("station_id") val stationId: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
internal data class SeedMapPinDto(
    val id: String,
    val category: String,
    @SerialName("entry_id") val entryId: String,
    val x: Float,
    val y: Float,
    val zone: String? = null,
    @SerialName("map_version") val mapVersion: Int,
    @SerialName("updated_at") val updatedAt: String,
)

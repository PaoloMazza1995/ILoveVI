package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.core.model.MapConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
private data class MapConfigRow(
    @SerialName("map_version") val mapVersion: Int,
    @SerialName("source_image_url") val sourceImageUrl: String,
    @SerialName("width_px") val widthPx: Int,
    @SerialName("height_px") val heightPx: Int,
    @SerialName("tile_url_template") val tileUrlTemplate: String,
    @SerialName("min_zoom") val minZoom: Int,
    @SerialName("max_zoom") val maxZoom: Int,
    @SerialName("updated_at") val updatedAt: String,
)

private fun MapConfigRow.toMapConfig() = MapConfig(
    mapVersion = mapVersion,
    sourceImageUrl = sourceImageUrl,
    widthPx = widthPx,
    heightPx = heightPx,
    tileUrlTemplate = tileUrlTemplate,
    minZoom = minZoom,
    maxZoom = maxZoom,
    updatedAt = Instant.parse(updatedAt),
)

/** Loads `map_config` (PLT-02) — read-only, no admin session required. */
class MapConfigRepository(private val client: SupabaseClient) {
    /** The highest `map_version` row — "the current map" this tool opens onto by default. */
    suspend fun loadCurrent(): MapConfig? =
        client.from("map_config").select {
            order("map_version", Order.DESCENDING)
            limit(1)
        }.decodeList<MapConfigRow>().firstOrNull()?.toMapConfig()

    /** Every map version on record, newest first — used to flag pins placed on an older one as "to reposition" (`:tools:tilegen`'s README). */
    suspend fun loadAll(): List<MapConfig> =
        client.from("map_config").select {
            order("map_version", Order.DESCENDING)
        }.decodeList<MapConfigRow>().map { it.toMapConfig() }
}

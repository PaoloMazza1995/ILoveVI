package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.core.model.MapConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class MapConfigInsertRow(
    @SerialName("map_version") val mapVersion: Int,
    @SerialName("source_image_url") val sourceImageUrl: String,
    @SerialName("width_px") val widthPx: Int,
    @SerialName("height_px") val heightPx: Int,
    @SerialName("tile_url_template") val tileUrlTemplate: String,
    @SerialName("min_zoom") val minZoom: Int,
    @SerialName("max_zoom") val maxZoom: Int,
)

/**
 * PLT-08's registration step (§2f): writes `map_config` through the signed-in admin's own session
 * — `map_config_admin_insert`/`map_config_admin_update` (`backend/supabase/migrations/
 * 20261011120004_map.sql`) already allow this, so unlike `:tools:tilegen`'s CLI (service-role,
 * `SupabaseAdminClient.insertMapConfig`) this never needs that key. Two methods, not one upsert,
 * because §3's version decision is made by the caller (`SupabaseMapPublisher`) and each path
 * writes a different, deliberately narrow set of columns — `updateInPlace` never touches
 * `map_version` (it's the primary key), `insertNewVersion` only ever creates a row that doesn't
 * exist yet.
 */
class MapConfigWriter(private val client: SupabaseClient) {
    suspend fun updateInPlace(config: MapConfig) {
        client.from("map_config").update({
            set("source_image_url", config.sourceImageUrl)
            set("width_px", config.widthPx)
            set("height_px", config.heightPx)
            set("tile_url_template", config.tileUrlTemplate)
            set("min_zoom", config.minZoom)
            set("max_zoom", config.maxZoom)
        }) {
            filter { eq("map_version", config.mapVersion) }
        }
    }

    suspend fun insertNewVersion(config: MapConfig) {
        client.from("map_config").insert(
            MapConfigInsertRow(
                mapVersion = config.mapVersion,
                sourceImageUrl = config.sourceImageUrl,
                widthPx = config.widthPx,
                heightPx = config.heightPx,
                tileUrlTemplate = config.tileUrlTemplate,
                minZoom = config.minZoom,
                maxZoom = config.maxZoom,
            ),
        )
    }
}

package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.tools.mapeditor.data.MapConfigResult
import com.pamaz.vi.tools.mapeditor.data.RawMapConfigRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
internal data class MapConfigRow(
    @SerialName("map_version") val mapVersion: Int,
    @SerialName("source_image_url") val sourceImageUrl: String,
    @SerialName("width_px") val widthPx: Int,
    @SerialName("height_px") val heightPx: Int,
    @SerialName("tile_url_template") val tileUrlTemplate: String,
    @SerialName("min_zoom") val minZoom: Int,
    @SerialName("max_zoom") val maxZoom: Int,
    @SerialName("updated_at") val updatedAt: String,
)

private fun MapConfigRow.toRawRow() = RawMapConfigRow(
    mapVersion = mapVersion,
    sourceImageUrl = sourceImageUrl,
    widthPx = widthPx,
    heightPx = heightPx,
    tileUrlTemplate = tileUrlTemplate,
    minZoom = minZoom,
    maxZoom = maxZoom,
)

/**
 * The bug this fixes: [MapConfig]'s constructor `require(...)`s a non-blank template with
 * `{z}`/`{x}`/`{y}`, positive width/height and `minZoom <= maxZoom` — throwing
 * `IllegalArgumentException` straight out of a row that's merely bad *data*, which used to kill
 * whatever coroutine called [MapConfigRepository.loadCurrent] (the map screen's `load()`, "Load
 * map image..."'s `plan()`). Every one of those checks, plus the one [MapConfig] itself doesn't
 * make (`source_image_url` blank), is caught here and turned into [MapConfigResult.Invalid]
 * instead — `internal`, not `private`, so `MapConfigRepositoryTest.kt` can build a [MapConfigRow]
 * directly and exercise every bad-value case without a real network call.
 */
internal fun MapConfigRow.toMapConfigResult(): MapConfigResult {
    if (sourceImageUrl.isBlank()) {
        return MapConfigResult.Invalid("source_image_url is empty.", toRawRow())
    }
    return try {
        MapConfigResult.Loaded(
            MapConfig(
                mapVersion = mapVersion,
                sourceImageUrl = sourceImageUrl,
                widthPx = widthPx,
                heightPx = heightPx,
                tileUrlTemplate = tileUrlTemplate,
                minZoom = minZoom,
                maxZoom = maxZoom,
                updatedAt = Instant.parse(updatedAt),
            ),
        )
    } catch (e: Exception) {
        MapConfigResult.Invalid(e.message ?: "Invalid map_config row.", toRawRow())
    }
}

/** Loads `map_config` (PLT-02) — read-only, no admin session required. `loadCurrent()` already matches [com.pamaz.vi.tools.mapeditor.data.MapConfigRepository] exactly, so it implements that interface directly rather than needing a wrapper adapter. */
class MapConfigRepository(private val client: SupabaseClient) :
    com.pamaz.vi.tools.mapeditor.data.MapConfigRepository {
    /** The highest `map_version` row — "the current map" this tool opens onto by default. Never throws: a decode failure or a row that fails [MapConfigRow.toMapConfigResult]'s validation both become [MapConfigResult.Invalid], not an exception. */
    override suspend fun loadCurrent(): MapConfigResult = try {
        val row = client.from("map_config").select {
            order("map_version", Order.DESCENDING)
            limit(1)
        }.decodeList<MapConfigRow>().firstOrNull()
        row?.toMapConfigResult() ?: MapConfigResult.Missing
    } catch (e: Exception) {
        MapConfigResult.Invalid(
            e.message ?: "Could not load map_config.",
            RawMapConfigRow(null, null, null, null, null, null, null),
        )
    }

    /** Every map version on record, newest first — used to flag pins placed on an older one as "to reposition" (`:tools:tilegen`'s README). Rows that fail validation are skipped rather than thrown — this already tolerates older, now-invalid rows the same way `loadCurrent()` does. */
    suspend fun loadAll(): List<MapConfig> =
        client.from("map_config").select {
            order("map_version", Order.DESCENDING)
        }.decodeList<MapConfigRow>()
            .mapNotNull { (it.toMapConfigResult() as? MapConfigResult.Loaded)?.config }
}

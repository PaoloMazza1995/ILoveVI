package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.map.TileUrl
import com.pamaz.vi.tools.mapeditor.data.MapTileChecker
import com.pamaz.vi.tools.mapeditor.model.MapLoadError
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.withTimeoutOrNull

private const val CHECK_TIMEOUT_MILLIS = 6_000L

/**
 * PLT-08: GETs tile `0/0/0` of [MapConfig.tileUrlTemplate] (reused from `core:model.map.TileUrl`,
 * the same builder `:core:mapview` itself uses for every other tile) and turns a non-2xx status, a
 * thrown exception (DNS/TLS/connection failure) or a timeout into a plain-words [MapLoadError]
 * carrying the exact URL that was requested, instead of letting `EditorState.load()` hang forever.
 */
class SupabaseMapTileChecker(private val httpClient: HttpClient) : MapTileChecker {
    override suspend fun checkReachable(config: MapConfig): MapLoadError? {
        val url = TileUrl.build(config.tileUrlTemplate, 0, 0, 0)
        return try {
            val response = withTimeoutOrNull(CHECK_TIMEOUT_MILLIS) { httpClient.get(url) }
                ?: return MapLoadError(
                    "The map tile server didn't respond within ${CHECK_TIMEOUT_MILLIS / 1000}s. Check your network connection.",
                    url,
                )
            if (response.status.isSuccess()) {
                null
            } else {
                MapLoadError("The first tile returned HTTP ${response.status.value}.", url)
            }
        } catch (e: Exception) {
            MapLoadError(
                "Could not reach the map tile server: ${e.message ?: e::class.simpleName}",
                url
            )
        }
    }
}

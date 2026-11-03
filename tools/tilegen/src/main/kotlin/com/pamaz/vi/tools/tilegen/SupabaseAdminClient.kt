package com.pamaz.vi.tools.tilegen

import com.pamaz.vi.core.model.MapConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readBytes

/**
 * Admin (service-role) credentials for one Supabase project, loaded from the environment — never
 * from a file in this repo. [serviceRoleKey] bypasses Row Level Security entirely, which is what
 * lets this tool write to the admin-only `map_config`/`map-tiles` policies defined in
 * `backend/supabase/migrations`.
 */
data class SupabaseAdminConfig(val baseUrl: String, val serviceRoleKey: String) {
    companion object {
        /** Reads `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY`; throws a clear error if either is unset. */
        fun fromEnvironment(env: (String) -> String? = System::getenv): SupabaseAdminConfig {
            val baseUrl = env("SUPABASE_URL")
                ?: error("SUPABASE_URL environment variable is not set")
            val serviceRoleKey = env("SUPABASE_SERVICE_ROLE_KEY")
                ?: error("SUPABASE_SERVICE_ROLE_KEY environment variable is not set")
            return SupabaseAdminConfig(baseUrl.trimEnd('/'), serviceRoleKey)
        }
    }
}

@Serializable
private data class MapVersionRow(@SerialName("map_version") val mapVersion: Int)

@Serializable
private data class MapConfigInsert(
    @SerialName("map_version") val mapVersion: Int,
    @SerialName("source_image_url") val sourceImageUrl: String,
    @SerialName("width_px") val widthPx: Int,
    @SerialName("height_px") val heightPx: Int,
    @SerialName("tile_url_template") val tileUrlTemplate: String,
    @SerialName("min_zoom") val minZoom: Int,
    @SerialName("max_zoom") val maxZoom: Int,
)

/**
 * Talks to the `map-tiles` Storage bucket and `map_config` table directly over PostgREST/Storage
 * REST, the same way `backend/README.md`'s admin examples do — no Supabase SDK is added just for
 * these three calls.
 */
object SupabaseAdminClient {
    /**
     * The next `map_config.map_version` to use: the current highest plus one, or `1` if the table
     * is empty. Used when the CLI isn't given an explicit `--map-version`.
     */
    suspend fun nextMapVersion(httpClient: HttpClient, config: SupabaseAdminConfig): Int {
        val response = httpClient.get("${config.baseUrl}/rest/v1/map_config") {
            parameter("select", "map_version")
            parameter("order", "map_version.desc")
            parameter("limit", "1")
            header("apikey", config.serviceRoleKey)
            header(HttpHeaders.Authorization, "Bearer ${config.serviceRoleKey}")
        }
        require(response.status.isSuccess()) {
            "failed to read the current map_version: HTTP ${response.status.value} ${response.bodyAsText()}"
        }
        val rows = response.body<List<MapVersionRow>>()
        return (rows.firstOrNull()?.mapVersion ?: 0) + 1
    }

    /**
     * Uploads every tile file under `<outputDir>/<mapVersion>/` to `map-tiles/<mapVersion>/...`,
     * overwriting any existing object at the same path (`x-upsert`) so re-running tilegen for the
     * same map version is idempotent. Returns the number of tiles uploaded (or that would be, when
     * [dryRun]).
     *
     * `Cache-Control: max-age=31536000, immutable` on every object: each map version has its own
     * folder (`<mapVersion>/...`) and a version's tiles, once generated, never change in place —
     * re-running tilegen for the *same* version only happens to retry a failed upload, never to
     * publish different pixels under the same URL — so a client (or CDN in front of Storage) never
     * needs to revalidate a cached tile.
     */
    suspend fun uploadTiles(
        httpClient: HttpClient,
        config: SupabaseAdminConfig,
        outputDir: Path,
        mapVersion: Int,
        dryRun: Boolean,
        format: TileFormat = TileFormat.WEBP,
    ): Int {
        val versionDir = outputDir.resolve(mapVersion.toString())
        val tileFiles = Files.walk(versionDir).use { stream ->
            stream.filter { Files.isRegularFile(it) }.toList()
        }
        val tileContentType = tileContentType(format)

        if (!dryRun) {
            for (file in tileFiles) {
                val relativePath =
                    versionDir.relativize(file).toString().replace(File.separatorChar, '/')
                val objectPath = "$mapVersion/$relativePath"
                val response =
                    httpClient.post("${config.baseUrl}/storage/v1/object/map-tiles/$objectPath") {
                        header("apikey", config.serviceRoleKey)
                        header(HttpHeaders.Authorization, "Bearer ${config.serviceRoleKey}")
                        header("x-upsert", "true")
                        header(HttpHeaders.CacheControl, "max-age=31536000, immutable")
                        contentType(tileContentType)
                        setBody(file.readBytes())
                    }
                require(response.status.isSuccess()) {
                    "failed to upload $objectPath: HTTP ${response.status.value} ${response.bodyAsText()}"
                }
            }
        }
        return tileFiles.size
    }

    /**
     * Inserts [mapConfig] as a `map_config` row. [mapConfig] is only ever constructed by the
     * caller via the real [MapConfig] class, so its own `init`-block invariants (positive
     * dimensions, `minZoom <= maxZoom`, a template with `{z}`/`{x}`/`{y}`) are already guaranteed
     * before this ever reaches the network. `updated_at` is omitted from the request body on
     * purpose — the column's `DEFAULT now()` sets it server-side.
     */
    suspend fun insertMapConfig(
        httpClient: HttpClient,
        config: SupabaseAdminConfig,
        mapConfig: MapConfig,
        dryRun: Boolean,
    ): MapConfig {
        if (dryRun) return mapConfig

        val response = httpClient.post("${config.baseUrl}/rest/v1/map_config") {
            header("apikey", config.serviceRoleKey)
            header(HttpHeaders.Authorization, "Bearer ${config.serviceRoleKey}")
            header("Prefer", "return=minimal")
            contentType(ContentType.Application.Json)
            setBody(
                MapConfigInsert(
                    mapVersion = mapConfig.mapVersion,
                    sourceImageUrl = mapConfig.sourceImageUrl,
                    widthPx = mapConfig.widthPx,
                    heightPx = mapConfig.heightPx,
                    tileUrlTemplate = mapConfig.tileUrlTemplate,
                    minZoom = mapConfig.minZoom,
                    maxZoom = mapConfig.maxZoom,
                ),
            )
        }
        require(response.status.isSuccess()) {
            "failed to insert map_config row: HTTP ${response.status.value} ${response.bodyAsText()}"
        }
        return mapConfig
    }
}

internal fun tileContentType(format: TileFormat): ContentType = when (format) {
    TileFormat.WEBP -> ContentType.Image.WEBP
    TileFormat.PNG -> ContentType.Image.PNG
}

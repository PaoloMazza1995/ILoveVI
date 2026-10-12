package com.pamaz.vi.tools.tilegen

import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.map.TileScheme
import io.ktor.client.HttpClient
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Instant

/**
 * One tilegen run's inputs. This is the same request type the CLI's `main` builds from
 * command-line flags and that `:tools:map-editor` can build directly in-process — the only
 * difference between "CLI" and "library" use is who constructs this object.
 */
data class TileGenRequest(
    val source: TileSource,
    val outputDir: Path,
    val tileSize: Int = 256,
    val backgroundColor: Int = TilePyramidGenerator.DEFAULT_BACKGROUND_COLOR,
    /** Explicit map version, or `null` to auto-derive it (see [TileGenPipeline.run]). */
    val mapVersion: Int? = null,
    val upload: Boolean = false,
    /** When paired with [upload], simulates the upload/insert calls instead of making them. */
    val dryRun: Boolean = false,
    val supabase: SupabaseAdminConfig? = null,
) {
    init {
        require(!upload || supabase != null) { "upload requires a SupabaseAdminConfig" }
    }
}

/** What one tilegen run produced. [mapConfig] is only non-null when [TileGenRequest.upload] was set. */
data class TileGenResult(
    val scheme: TileScheme,
    val mapVersion: Int,
    val tileCount: Int,
    val versionOutputDir: Path,
    val mapConfig: MapConfig?,
    val uploadedTileCount: Int?,
)

/**
 * Orchestrates one tilegen run end to end: fetch (if remote) -> tile -> upload (if requested) ->
 * register the `map_config` row (if requested). Exposed as a single suspend function so both the
 * CLI and `:tools:map-editor` call the exact same code path.
 */
object TileGenPipeline {
    suspend fun run(request: TileGenRequest, httpClient: HttpClient): TileGenResult {
        val tempDir = if (request.source is TileSource.Remote) createTempDownloadDir() else null
        try {
            val sourceImagePath = when (val source = request.source) {
                is TileSource.Local -> source.path
                is TileSource.Remote -> SourceImageFetcher.fetch(source.url, tempDir!!, httpClient)
            }

            val dimensions = readImageDimensions(sourceImagePath)
            val scheme = TileScheme(dimensions.widthPx, dimensions.heightPx, request.tileSize)

            val mapVersion = request.mapVersion ?: if (request.upload) {
                SupabaseAdminClient.nextMapVersion(httpClient, request.supabase!!)
            } else {
                1
            }

            val stats = TilePyramidGenerator.generate(
                sourceImage = sourceImagePath,
                scheme = scheme,
                outputDir = request.outputDir,
                mapVersion = mapVersion,
                backgroundColor = request.backgroundColor,
            )

            var mapConfig: MapConfig? = null
            var uploadedTileCount: Int? = null
            if (request.upload) {
                val supabase = request.supabase!!
                uploadedTileCount = SupabaseAdminClient.uploadTiles(
                    httpClient, supabase, request.outputDir, mapVersion, request.dryRun,
                )

                val sourceDescriptor = when (val source = request.source) {
                    is TileSource.Remote -> source.url
                    is TileSource.Local -> source.path.toString()
                }
                val tileUrlTemplate =
                    "${supabase.baseUrl}/storage/v1/object/public/map-tiles/$mapVersion/{z}/{x}/{y}.png"

                val config = MapConfig(
                    mapVersion = mapVersion,
                    sourceImageUrl = sourceDescriptor,
                    widthPx = dimensions.widthPx,
                    heightPx = dimensions.heightPx,
                    tileUrlTemplate = tileUrlTemplate,
                    minZoom = scheme.minZoom,
                    maxZoom = scheme.maxZoom,
                    // Unused by insertMapConfig (the DB column defaults to now()); MapConfig's
                    // constructor requires a value, so this is a throwaway placeholder.
                    updatedAt = Instant.fromEpochMilliseconds(0),
                )
                mapConfig = SupabaseAdminClient.insertMapConfig(
                    httpClient,
                    supabase,
                    config,
                    request.dryRun
                )
            }

            return TileGenResult(
                scheme = scheme,
                mapVersion = mapVersion,
                tileCount = stats.tileCount,
                versionOutputDir = request.outputDir.resolve(mapVersion.toString()),
                mapConfig = mapConfig,
                uploadedTileCount = uploadedTileCount,
            )
        } finally {
            tempDir?.let { dir ->
                Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(Files::delete)
            }
        }
    }
}

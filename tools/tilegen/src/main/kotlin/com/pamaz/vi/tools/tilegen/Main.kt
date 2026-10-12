package com.pamaz.vi.tools.tilegen

import com.pamaz.vi.tools.tilegen.placeholder.PlaceholderMapImage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.system.exitProcess

/**
 * CLI entry point. Two commands:
 * - `tilegen --source <url|path> --out <dir> [--upload] [--dry-run] [--map-version N]` — the main
 *   pipeline (see [TileGenPipeline]).
 * - `tilegen generate-placeholder --out <path> [--width N] [--height N]` — writes a clearly-fake
 *   dev map image (see [PlaceholderMapImage]) to use as `--source` locally.
 */
fun main(args: Array<String>) {
    try {
        if (args.firstOrNull() == "generate-placeholder") {
            runGeneratePlaceholder(parsePlaceholderArgs(args.drop(1)))
        } else {
            runBlocking { runTileGen(parseTileGenArgs(args.toList())) }
        }
    } catch (e: TileGenArgsException) {
        System.err.println("error: ${e.message}")
        exitProcess(1)
    } catch (e: IllegalStateException) {
        System.err.println("error: ${e.message}")
        exitProcess(1)
    } catch (e: IllegalArgumentException) {
        System.err.println("error: ${e.message}")
        exitProcess(1)
    }
}

private fun runGeneratePlaceholder(args: PlaceholderCliArgs) {
    val image = PlaceholderMapImage.generate(args.width, args.height)
    val file = File(args.outputPath)
    file.parentFile?.mkdirs()
    ImageIO.write(image, "png", file)
    println("Wrote placeholder map (${args.width}x${args.height}) to ${file.path}")
}

private suspend fun runTileGen(args: TileGenCliArgs) {
    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        followRedirects = true
    }
    try {
        val supabase = if (args.upload) SupabaseAdminConfig.fromEnvironment() else null
        val request = TileGenRequest(
            source = toTileSource(args.source),
            outputDir = Path.of(args.outputDir),
            tileSize = args.tileSize,
            backgroundColor = args.backgroundColor,
            mapVersion = args.mapVersion,
            upload = args.upload,
            dryRun = args.dryRun,
            supabase = supabase,
        )
        val result = TileGenPipeline.run(request, httpClient)

        println(
            "Generated ${result.tileCount} tiles (zoom ${result.scheme.minZoom}..${result.scheme.maxZoom}) " +
                    "for map_version ${result.mapVersion} at ${result.versionOutputDir}",
        )
        if (args.upload) {
            println(
                if (args.dryRun) {
                    "Dry run: would have uploaded ${result.uploadedTileCount} tiles and inserted the map_config row."
                } else {
                    "Uploaded ${result.uploadedTileCount} tiles and inserted the map_config row " +
                            "for map_version ${result.mapVersion}."
                },
            )
        }
    } finally {
        httpClient.close()
    }
}

private fun toTileSource(source: String): TileSource =
    if (source.startsWith("http://") || source.startsWith("https://")) {
        TileSource.Remote(source)
    } else {
        TileSource.Local(Path.of(source))
    }

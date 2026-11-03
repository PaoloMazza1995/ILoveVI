package com.pamaz.vi.tools.tilegen

/** Thrown for any command-line usage error; `main` catches this and prints a clean message instead of a stack trace. */
class TileGenArgsException(message: String) : IllegalArgumentException(message)

/** Parsed `tilegen --source <url|path> --out <dir> [--upload] [--dry-run] [--map-version N] ...` flags. */
data class TileGenCliArgs(
    val source: String,
    val outputDir: String,
    val upload: Boolean,
    val dryRun: Boolean,
    val mapVersion: Int?,
    val backgroundColor: Int,
    val tileSize: Int,
    val format: TileFormat,
)

/** Parsed `tilegen generate-placeholder --out <path> [--width N] [--height N]` flags. */
data class PlaceholderCliArgs(
    val outputPath: String,
    val width: Int,
    val height: Int,
)

private const val USAGE = "usage: tilegen --source <url|path> --out <dir> [--upload] [--dry-run] " +
        "[--map-version N] [--tile-size N] [--background-color RRGGBB] [--format webp|png]\n" +
        "       tilegen generate-placeholder --out <path> [--width N] [--height N]"

fun parseTileGenArgs(args: List<String>): TileGenCliArgs {
    var source: String? = null
    var outputDir: String? = null
    var upload = false
    var dryRun = false
    var mapVersion: Int? = null
    var backgroundColor = TilePyramidGenerator.DEFAULT_BACKGROUND_COLOR
    var tileSize = 256
    var format = TileFormat.WEBP

    var i = 0
    fun nextValue(flag: String): String {
        i++
        return args.getOrNull(i) ?: throw TileGenArgsException("$flag requires a value\n$USAGE")
    }

    while (i < args.size) {
        when (args[i]) {
            "--source" -> source = nextValue("--source")
            "--out" -> outputDir = nextValue("--out")
            "--upload" -> upload = true
            "--dry-run" -> dryRun = true
            "--map-version" -> mapVersion = nextValue("--map-version").toIntOrNull()
                ?: throw TileGenArgsException("--map-version must be an integer")

            "--tile-size" -> tileSize = nextValue("--tile-size").toIntOrNull()
                ?: throw TileGenArgsException("--tile-size must be an integer")

            "--background-color" -> backgroundColor = parseHexColor(nextValue("--background-color"))
            "--format" -> format = try {
                TileFormat.fromExtension(nextValue("--format"))
            } catch (e: IllegalArgumentException) {
                throw TileGenArgsException(e.message ?: "invalid --format")
            }

            else -> throw TileGenArgsException("unknown argument: ${args[i]}\n$USAGE")
        }
        i++
    }

    return TileGenCliArgs(
        source = source
            ?: throw TileGenArgsException("missing required --source <url|path>\n$USAGE"),
        outputDir = outputDir ?: throw TileGenArgsException("missing required --out <dir>\n$USAGE"),
        upload = upload,
        dryRun = dryRun,
        mapVersion = mapVersion,
        backgroundColor = backgroundColor,
        tileSize = tileSize,
        format = format,
    )
}

fun parsePlaceholderArgs(args: List<String>): PlaceholderCliArgs {
    var outputPath: String? = null
    var width = com.pamaz.vi.tools.tilegen.placeholder.PlaceholderMapImage.DEFAULT_WIDTH_PX
    var height = com.pamaz.vi.tools.tilegen.placeholder.PlaceholderMapImage.DEFAULT_HEIGHT_PX

    var i = 0
    fun nextValue(flag: String): String {
        i++
        return args.getOrNull(i) ?: throw TileGenArgsException("$flag requires a value\n$USAGE")
    }

    while (i < args.size) {
        when (args[i]) {
            "--out" -> outputPath = nextValue("--out")
            "--width" -> width = nextValue("--width").toIntOrNull()
                ?: throw TileGenArgsException("--width must be an integer")

            "--height" -> height = nextValue("--height").toIntOrNull()
                ?: throw TileGenArgsException("--height must be an integer")

            else -> throw TileGenArgsException("unknown argument: ${args[i]}\n$USAGE")
        }
        i++
    }

    return PlaceholderCliArgs(
        outputPath = outputPath
            ?: throw TileGenArgsException("missing required --out <path>\n$USAGE"),
        width = width,
        height = height,
    )
}

/** Parses a bare `RRGGBB` hex string into an opaque ARGB [Int]. */
internal fun parseHexColor(hex: String): Int {
    val normalized = hex.removePrefix("#")
    if (normalized.length != 6) {
        throw TileGenArgsException("--background-color must be 6 hex digits (RRGGBB), was: $hex")
    }
    val rgb = normalized.toIntOrNull(16)
        ?: throw TileGenArgsException("--background-color must be 6 hex digits (RRGGBB), was: $hex")
    return (0xFF shl 24) or rgb
}

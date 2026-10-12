package com.pamaz.vi.tools.tilegen

import com.pamaz.vi.core.model.map.TileScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * Cuts a source map image into a [TileScheme]-shaped tile pyramid on disk, at
 * `<outputDir>/<mapVersion>/<zoom>/<tileX>/<tileY>.<format>`.
 *
 * A 16k-ish source image (the real GTA VI map, once it exists) can be hundreds of megabytes when
 * fully decoded, so this never holds the whole image in memory. It works in two memory-bounded
 * phases instead:
 *
 * 1. The base ([TileScheme.maxZoom]) level is built by streaming the source image one horizontal
 *    strip at a time (`ImageReadParam.sourceRegion`), one tile-row tall — peak memory is one strip
 *    plus one tile, never the whole image.
 * 2. Every lower zoom level is built from the 4 child tiles just written at the level above it
 *    (half-scaled and composited), the standard tile-pyramid technique — the source image is
 *    never touched again once the base level exists, and only 4 child tiles + 1 output tile are
 *    ever in memory at once.
 */
object TilePyramidGenerator {
    /** Neutral placeholder fill for the area of an edge tile that falls outside the source image. */
    const val DEFAULT_BACKGROUND_COLOR: Int = 0xFF1A1D22.toInt()

    private const val FORMAT = "png"

    data class Stats(val tileCount: Int)

    /**
     * Generates the full pyramid for [scheme] from [sourceImage] under
     * `<outputDir>/<mapVersion>/...`. [backgroundColor] (ARGB) pads the part of any edge tile that
     * falls past the image's actual width/height.
     */
    suspend fun generate(
        sourceImage: Path,
        scheme: TileScheme,
        outputDir: Path,
        mapVersion: Int,
        backgroundColor: Int = DEFAULT_BACKGROUND_COLOR,
    ): Stats = withContext(Dispatchers.IO) {
        require(scheme.tileSize % 2 == 0) {
            "tileSize must be even for pyramid downsampling to divide tiles exactly, was ${scheme.tileSize}"
        }

        val versionDir = outputDir.resolve(mapVersion.toString())
        var tileCount = generateBaseLevel(sourceImage, scheme, versionDir, backgroundColor)
        for (zoom in scheme.maxZoom - 1 downTo scheme.minZoom) {
            tileCount += generatePyramidLevel(scheme, versionDir, zoom, backgroundColor)
        }
        Stats(tileCount)
    }

    private fun generateBaseLevel(
        sourceImage: Path,
        scheme: TileScheme,
        versionDir: Path,
        backgroundColor: Int,
    ): Int {
        val zoom = scheme.maxZoom
        val tilesWide = scheme.tilesWide(zoom)
        val tilesHigh = scheme.tilesHigh(zoom)
        var written = 0

        withImageReader(sourceImage) { reader ->
            for (tileY in 0 until tilesHigh) {
                val rowStart = tileY * scheme.tileSize
                val rowHeight = minOf(scheme.tileSize, scheme.heightPx - rowStart)
                val param = reader.defaultReadParam.apply {
                    sourceRegion = Rectangle(0, rowStart, scheme.widthPx, rowHeight)
                }
                val strip = reader.read(0, param)

                for (tileX in 0 until tilesWide) {
                    val colStart = tileX * scheme.tileSize
                    val colWidth = minOf(scheme.tileSize, scheme.widthPx - colStart)

                    val tile = blankTile(scheme.tileSize, backgroundColor)
                    val g = tile.createGraphics()
                    g.drawImage(strip.getSubimage(colStart, 0, colWidth, rowHeight), 0, 0, null)
                    g.dispose()

                    writeTile(tile, versionDir, zoom, tileX, tileY)
                    written++
                }
            }
        }
        return written
    }

    private fun generatePyramidLevel(
        scheme: TileScheme,
        versionDir: Path,
        zoom: Int,
        backgroundColor: Int,
    ): Int {
        val tilesWide = scheme.tilesWide(zoom)
        val tilesHigh = scheme.tilesHigh(zoom)
        val childZoom = zoom + 1
        val childTilesWide = scheme.tilesWide(childZoom)
        val childTilesHigh = scheme.tilesHigh(childZoom)
        val half = scheme.tileSize / 2
        var written = 0

        for (tileY in 0 until tilesHigh) {
            for (tileX in 0 until tilesWide) {
                val tile = blankTile(scheme.tileSize, backgroundColor)
                val g = tile.createGraphics()
                g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
                )

                for (dy in 0..1) {
                    for (dx in 0..1) {
                        val childX = tileX * 2 + dx
                        val childY = tileY * 2 + dy
                        if (childX >= childTilesWide || childY >= childTilesHigh) continue

                        val childFile = tileFile(versionDir, childZoom, childX, childY)
                        val child = ImageIO.read(childFile)
                            ?: error("missing child tile $childFile while building pyramid level $zoom")
                        g.drawImage(child, dx * half, dy * half, half, half, null)
                    }
                }
                g.dispose()

                writeTile(tile, versionDir, zoom, tileX, tileY)
                written++
            }
        }
        return written
    }

    private fun blankTile(tileSize: Int, backgroundColor: Int): BufferedImage {
        val tile = BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB)
        val g = tile.createGraphics()
        g.color = Color(backgroundColor, true)
        g.fillRect(0, 0, tileSize, tileSize)
        g.dispose()
        return tile
    }

    private fun tileFile(versionDir: Path, zoom: Int, tileX: Int, tileY: Int): java.io.File =
        versionDir.resolve(zoom.toString()).resolve(tileX.toString()).resolve("$tileY.$FORMAT")
            .toFile()

    private fun writeTile(
        image: BufferedImage,
        versionDir: Path,
        zoom: Int,
        tileX: Int,
        tileY: Int
    ) {
        val file = tileFile(versionDir, zoom, tileX, tileY)
        Files.createDirectories(file.parentFile.toPath())
        ImageIO.write(image, FORMAT, file)
    }
}

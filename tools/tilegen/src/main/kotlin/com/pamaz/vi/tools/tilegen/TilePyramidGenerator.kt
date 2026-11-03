package com.pamaz.vi.tools.tilegen

import com.luciad.imageio.webp.CompressionType
import com.luciad.imageio.webp.WebPWriteParam
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.tools.tilegen.TilePyramidGenerator.writeTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.FileImageOutputStream
import javax.imageio.stream.MemoryCacheImageOutputStream

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

    data class Stats(val tileCount: Int)

    /**
     * Generates the full pyramid for [scheme] from [sourceImage] under
     * `<outputDir>/<mapVersion>/...`, as [format] tiles (default [TileFormat.WEBP] — see that
     * enum's KDoc for why). [backgroundColor] (ARGB) pads the part of any edge tile that falls
     * past the image's actual width/height.
     */
    suspend fun generate(
        sourceImage: Path,
        scheme: TileScheme,
        outputDir: Path,
        mapVersion: Int,
        backgroundColor: Int = DEFAULT_BACKGROUND_COLOR,
        format: TileFormat = TileFormat.WEBP,
    ): Stats = withContext(Dispatchers.IO) {
        require(scheme.tileSize % 2 == 0) {
            "tileSize must be even for pyramid downsampling to divide tiles exactly, was ${scheme.tileSize}"
        }

        val versionDir = outputDir.resolve(mapVersion.toString())
        var tileCount = generateBaseLevel(sourceImage, scheme, versionDir, backgroundColor, format)
        for (zoom in scheme.maxZoom - 1 downTo scheme.minZoom) {
            tileCount += generatePyramidLevel(scheme, versionDir, zoom, backgroundColor, format)
        }
        Stats(tileCount)
    }

    private fun generateBaseLevel(
        sourceImage: Path,
        scheme: TileScheme,
        versionDir: Path,
        backgroundColor: Int,
        format: TileFormat,
    ): Int {
        val zoom = scheme.maxZoom
        val tilesWide = scheme.tilesWide(zoom)
        val tilesHigh = scheme.tilesHigh(zoom)
        var written = 0

        // Even at the largest supported source image (32768px per side), one strip is
        // 32768 x tileSize pixels — ~33MB decoded at the default 256px tile size — never the
        // whole image at once; see this object's own KDoc and the module README's "Memory
        // safety" section for the measured bound.
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

                    writeTile(tile, versionDir, zoom, tileX, tileY, format)
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
        format: TileFormat,
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

                        val childFile = tileFile(versionDir, childZoom, childX, childY, format)
                        val child = ImageIO.read(childFile)
                            ?: error("missing child tile $childFile while building pyramid level $zoom")
                        g.drawImage(child, dx * half, dy * half, half, half, null)
                    }
                }
                g.dispose()

                writeTile(tile, versionDir, zoom, tileX, tileY, format)
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

    private fun tileFile(
        versionDir: Path,
        zoom: Int,
        tileX: Int,
        tileY: Int,
        format: TileFormat
    ): File =
        versionDir.resolve(zoom.toString()).resolve(tileX.toString())
            .resolve("$tileY.${format.extension}").toFile()

    private fun writeTile(
        image: BufferedImage,
        versionDir: Path,
        zoom: Int,
        tileX: Int,
        tileY: Int,
        format: TileFormat,
    ) {
        val file = tileFile(versionDir, zoom, tileX, tileY, format)
        Files.createDirectories(file.parentFile.toPath())
        FileImageOutputStream(file).use { output -> encodeTile(image, format, output) }
    }

    /**
     * Encodes [image] as [format] straight into [output] — shared by [writeTile] (a real file) and
     * [sampleTileByteSizes] (an in-memory buffer, for a storage-size estimate before any real
     * generation happens) so there's exactly one place that knows how to produce the actual bytes
     * a client will eventually download.
     *
     * [com.luciad.imageio.webp.WebPWriter]'s standard `javax.imageio` contract: `compressionMode`
     * must be set to [ImageWriteParam.MODE_EXPLICIT] before [ImageWriteParam.setCompressionQuality]
     * takes effect — the same requirement every other `ImageWriteParam`-based JVM encoder (e.g.
     * JPEG's own) has. The map is always fully opaque, so [CompressionType.Lossy] at
     * [WEBP_LOSSY_QUALITY] is used unconditionally; there's no lossless/transparency case this
     * tool ever needs.
     */
    private fun encodeTile(
        image: BufferedImage,
        format: TileFormat,
        output: javax.imageio.stream.ImageOutputStream,
    ) {
        when (format) {
            TileFormat.PNG -> {
                val writer = ImageIO.getImageWritersByFormatName("png").next()
                writer.output = output
                writer.write(image)
                writer.dispose()
            }

            TileFormat.WEBP -> {
                val writer = ImageIO.getImageWritersByMIMEType("image/webp").next()
                val param = (writer.defaultWriteParam as WebPWriteParam).apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionType = CompressionType.Lossy
                    compressionQuality = WEBP_LOSSY_QUALITY
                }
                writer.output = output
                writer.write(null, IIOImage(image, null, null), param)
                writer.dispose()
            }
        }
    }

    /**
     * Encodes [sampleCount] real tiles from [sourceImage]'s base ([TileScheme.maxZoom]) level —
     * evenly spaced across the first row — as [format], entirely in memory, returning each one's
     * actual encoded byte size. Used by `:tools:map-editor`'s "Load map image..." dialog to show a
     * real storage-size estimate *before* committing to generating the full pyramid (task item 7b):
     * reads only the one strip these samples come from, never the whole source image.
     */
    fun sampleTileByteSizes(
        sourceImage: Path,
        scheme: TileScheme,
        format: TileFormat = TileFormat.WEBP,
        sampleCount: Int = 3,
    ): List<Long> {
        val zoom = scheme.maxZoom
        val tilesWide = scheme.tilesWide(zoom)
        val rowHeight = minOf(scheme.tileSize, scheme.heightPx)
        val sampleTileXs = (0 until minOf(sampleCount, tilesWide)).map { i ->
            if (tilesWide <= 1) 0 else i * (tilesWide - 1) / maxOf(
                1,
                minOf(sampleCount, tilesWide) - 1
            )
        }.distinct()

        return withImageReader(sourceImage) { reader ->
            val param = reader.defaultReadParam.apply {
                sourceRegion = Rectangle(0, 0, scheme.widthPx, rowHeight)
            }
            val strip = reader.read(0, param)
            sampleTileXs.map { tileX ->
                val colStart = tileX * scheme.tileSize
                val colWidth = minOf(scheme.tileSize, scheme.widthPx - colStart)
                val tile = blankTile(scheme.tileSize, DEFAULT_BACKGROUND_COLOR)
                val g = tile.createGraphics()
                g.drawImage(strip.getSubimage(colStart, 0, colWidth, rowHeight), 0, 0, null)
                g.dispose()

                val bytes = ByteArrayOutputStream()
                MemoryCacheImageOutputStream(bytes).use { output ->
                    encodeTile(
                        tile,
                        format,
                        output
                    )
                }
                bytes.size().toLong()
            }
        }
    }
}

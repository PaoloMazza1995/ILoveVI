package com.pamaz.vi.tools.tilegen

import com.pamaz.vi.core.model.map.TileScheme
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TilePyramidGeneratorTest {

    private fun syntheticImage(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = x * 255 / width
                val g = y * 255 / height
                val b = (x + y) % 256
                image.setRGB(x, y, (r shl 16) or (g shl 8) or b)
            }
        }
        return image
    }

    private fun tileFile(
        outDir: java.nio.file.Path,
        mapVersion: Int,
        zoom: Int,
        tileX: Int,
        tileY: Int
    ) =
        outDir.resolve(mapVersion.toString()).resolve(zoom.toString()).resolve(tileX.toString())
            .resolve("$tileY.png").toFile()

    @Test
    fun tileCountsAndDimensions_1000x700() = runBlocking {
        val tempDir = createTempDirectory("tilegen-test-")
        try {
            val sourceFile = tempDir.resolve("source.png").toFile()
            ImageIO.write(syntheticImage(1000, 700), "png", sourceFile)

            val outDir = tempDir.resolve("out")
            val scheme = TileScheme(1000, 700)
            val stats =
                TilePyramidGenerator.generate(sourceFile.toPath(), scheme, outDir, mapVersion = 1)

            var expectedTotal = 0
            for (zoom in scheme.minZoom..scheme.maxZoom) {
                val tilesWide = scheme.tilesWide(zoom)
                val tilesHigh = scheme.tilesHigh(zoom)
                expectedTotal += tilesWide * tilesHigh
                for (tileY in 0 until tilesHigh) {
                    for (tileX in 0 until tilesWide) {
                        val file = tileFile(outDir, 1, zoom, tileX, tileY)
                        assertTrue(file.exists(), "expected tile $zoom/$tileX/$tileY to exist")
                        val tileImage = ImageIO.read(file)
                        assertEquals(256, tileImage.width, "tile $zoom/$tileX/$tileY width")
                        assertEquals(256, tileImage.height, "tile $zoom/$tileX/$tileY height")
                    }
                }
            }
            assertEquals(expectedTotal, stats.tileCount)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun stitchingTopLevel_reproducesOriginalWithinTolerance() = runBlocking {
        val tempDir = createTempDirectory("tilegen-test-")
        try {
            val sourceFile = tempDir.resolve("source.png").toFile()
            val original = syntheticImage(1000, 700)
            ImageIO.write(original, "png", sourceFile)

            val outDir = tempDir.resolve("out")
            val scheme = TileScheme(1000, 700)
            TilePyramidGenerator.generate(sourceFile.toPath(), scheme, outDir, mapVersion = 1)

            val zoom = scheme.maxZoom
            val tilesWide = scheme.tilesWide(zoom)
            val tilesHigh = scheme.tilesHigh(zoom)
            val stitched = BufferedImage(
                tilesWide * scheme.tileSize,
                tilesHigh * scheme.tileSize,
                BufferedImage.TYPE_INT_RGB,
            )
            val g = stitched.createGraphics()
            for (tileY in 0 until tilesHigh) {
                for (tileX in 0 until tilesWide) {
                    val tileImage = ImageIO.read(tileFile(outDir, 1, zoom, tileX, tileY))
                    g.drawImage(tileImage, tileX * scheme.tileSize, tileY * scheme.tileSize, null)
                }
            }
            g.dispose()

            var maxDiff = 0
            for (y in 0 until 700) {
                for (x in 0 until 1000) {
                    val expected = original.getRGB(x, y)
                    val actual = stitched.getRGB(x, y)
                    maxDiff = maxOf(maxDiff, componentwiseMaxDiff(expected, actual))
                }
            }
            assertTrue(
                maxDiff <= 2,
                "expected stitched image to match the original within tolerance, max diff was $maxDiff"
            )
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    private fun componentwiseMaxDiff(a: Int, b: Int): Int {
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        return maxOf(abs(ar - br), abs(ag - bg), abs(ab - bb))
    }
}

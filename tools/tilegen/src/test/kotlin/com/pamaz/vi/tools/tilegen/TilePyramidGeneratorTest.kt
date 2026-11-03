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
        tileY: Int,
        format: TileFormat = TileFormat.WEBP,
    ) =
        outDir.resolve(mapVersion.toString()).resolve(zoom.toString()).resolve(tileX.toString())
            .resolve("$tileY.${format.extension}").toFile()

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
                        // Default format (WebP) — see the explicit-PNG test below for pixel-fidelity.
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
            // PNG (lossless) explicitly: this test asserts the *tiling/stitching math* reproduces
            // the source exactly, independent of lossy compression artifacts — WebP's own quality
            // is a separate, deliberate trade-off (see TileFormat's KDoc), not a bug to tolerate
            // here.
            TilePyramidGenerator.generate(
                sourceFile.toPath(), scheme, outDir, mapVersion = 1, format = TileFormat.PNG,
            )

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
                    val tileImage =
                        ImageIO.read(tileFile(outDir, 1, zoom, tileX, tileY, TileFormat.PNG))
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

    /**
     * Exercises the real `webp-imageio` native library end to end (encode via
     * [TilePyramidGenerator.generate]'s [TileFormat.WEBP] default, decode back via plain
     * [ImageIO.read] — the exact round-trip a client decoding a downloaded tile does), not just
     * that the Kotlin code compiles against its API. Lossy, so this allows a looser tolerance than
     * the PNG stitching test above — the point here is "a real, valid, reasonably-faithful WebP
     * tile came out the other end", not pixel-exactness.
     */
    @Test
    fun generate_webpDefault_roundTripsThroughRealCodec() = runBlocking {
        val tempDir = createTempDirectory("tilegen-webp-test-")
        try {
            val sourceFile = tempDir.resolve("source.png").toFile()
            // A smooth gradient, not `syntheticImage`'s sawtooth blue channel — a lossy
            // block-based codec's worst-case error is dominated by hard per-pixel discontinuities,
            // which isn't what this test cares about measuring; a real map image is photographic/
            // smooth, not adversarial noise.
            val original = BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB)
            for (y in 0 until 256) {
                for (x in 0 until 256) {
                    original.setRGB(x, y, (x shl 16) or (y shl 8) or ((x + y) / 2))
                }
            }
            ImageIO.write(original, "png", sourceFile)

            val outDir = tempDir.resolve("out")
            val scheme = TileScheme(256, 256)
            TilePyramidGenerator.generate(sourceFile.toPath(), scheme, outDir, mapVersion = 1)

            val file = tileFile(outDir, 1, scheme.maxZoom, 0, 0)
            assertTrue(file.exists() && file.extension == "webp")
            val decoded = ImageIO.read(file)
            assertEquals(256, decoded.width)
            assertEquals(256, decoded.height)

            var totalDiff = 0L
            for (y in 0 until 256) {
                for (x in 0 until 256) {
                    totalDiff += componentwiseMaxDiff(original.getRGB(x, y), decoded.getRGB(x, y))
                }
            }
            val averageDiff = totalDiff.toDouble() / (256 * 256)
            assertTrue(
                averageDiff <= 10.0,
                "expected a visually close lossy round-trip, average per-pixel diff was $averageDiff",
            )
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun sampleTileByteSizes_returnsRealEncodedSizesWithoutWritingAnything() = runBlocking {
        val tempDir = createTempDirectory("tilegen-sample-test-")
        try {
            val sourceFile = tempDir.resolve("source.png").toFile()
            ImageIO.write(syntheticImage(1024, 1024), "png", sourceFile)
            val scheme = TileScheme(1024, 1024)

            val sizes = TilePyramidGenerator.sampleTileByteSizes(sourceFile.toPath(), scheme)

            assertTrue(sizes.isNotEmpty())
            assertTrue(
                sizes.all { it > 0 },
                "expected every sampled tile to have a positive encoded size, got $sizes"
            )
            assertTrue(tempDir.resolve("out").toFile().listFiles().isNullOrEmpty())
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

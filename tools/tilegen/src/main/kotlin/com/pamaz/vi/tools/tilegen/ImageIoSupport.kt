package com.pamaz.vi.tools.tilegen

import java.io.IOException
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream

/**
 * Pixel dimensions of an image, read from its header only — [ImageIO]'s readers report
 * [ImageReader.getWidth]/[ImageReader.getHeight] without decoding pixel data, so this never loads
 * the (potentially huge) source map image into memory. Public (not `internal`) so
 * `:tools:map-editor` can validate/plan a chosen image's tile pyramid before committing to
 * tiling it, reusing this instead of a second dimension-reading implementation.
 */
data class ImageDimensions(val widthPx: Int, val heightPx: Int)

/** Opens an [ImageReader] for [path] and runs [block], disposing the reader afterwards either way. */
fun <T> withImageReader(path: Path, block: (ImageReader) -> T): T {
    val stream: ImageInputStream = ImageIO.createImageInputStream(path.toFile())
        ?: throw IOException("could not open $path as an image input stream")
    return stream.use {
        val reader = ImageIO.getImageReaders(it).asSequence().firstOrNull()
            ?: throw IllegalArgumentException("$path is not a readable image (no ImageIO reader found for it)")
        reader.input = it
        try {
            block(reader)
        } finally {
            reader.dispose()
        }
    }
}

fun readImageDimensions(path: Path): ImageDimensions =
    withImageReader(path) { reader -> ImageDimensions(reader.getWidth(0), reader.getHeight(0)) }

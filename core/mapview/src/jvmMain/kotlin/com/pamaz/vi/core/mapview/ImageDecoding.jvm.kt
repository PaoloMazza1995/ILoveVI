package com.pamaz.vi.core.mapview

import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.Surface

/**
 * `Image.makeFromEncoded` wraps the encoded bytes lazily — its pixels are only rasterized when
 * drawn. Downscaling therefore rasters straight into a target-sized [Surface] (never a
 * full-resolution one), satisfying "never keep two full-size copies in memory" for the JVM/desktop
 * target. Shared almost verbatim with the iOS actual (both are Skia-backed); kept as two small
 * per-target files rather than a custom `skikoMain` intermediate source set, since this module
 * doesn't otherwise need one.
 */
internal actual fun decodeImageBitmapDownscaled(
    bytes: ByteArray,
    maxDimensionPx: Int
): DecodedImage {
    val image = Image.makeFromEncoded(bytes)
    val originalWidth = image.width
    val originalHeight = image.height
    val target = downscaledSize(originalWidth, originalHeight, maxDimensionPx)

    if (target.widthPx == originalWidth && target.heightPx == originalHeight) {
        return DecodedImage(image.toComposeImageBitmap(), originalWidth, originalHeight)
    }

    val surface = Surface.makeRasterN32Premul(target.widthPx, target.heightPx)
    val srcRect = Rect.makeWH(originalWidth.toFloat(), originalHeight.toFloat())
    val dstRect = Rect.makeWH(target.widthPx.toFloat(), target.heightPx.toFloat())
    surface.canvas.drawImageRect(
        image, srcRect, dstRect, SamplingMode.LINEAR, Paint().apply { isAntiAlias = true }, true,
    )
    val scaled = surface.makeImageSnapshot()
    return DecodedImage(scaled.toComposeImageBitmap(), originalWidth, originalHeight)
}

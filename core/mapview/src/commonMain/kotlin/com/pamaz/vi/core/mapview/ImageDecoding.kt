package com.pamaz.vi.core.mapview

import androidx.compose.ui.graphics.ImageBitmap

/** [decodeImageBitmapDownscaled]'s result: the decoded (and possibly downscaled) [bitmap], plus the source's original pixel size for logging. */
internal data class DecodedImage(
    val bitmap: ImageBitmap,
    val originalWidthPx: Int,
    val originalHeightPx: Int,
)

/**
 * Decodes [bytes] (PNG/JPEG/etc.) to an [ImageBitmap], downscaling via [downscaledSize] so
 * neither side exceeds [maxDimensionPx] while preserving aspect ratio. Each platform actual is
 * responsible for never fully materializing a larger-than-target raster along the way (Android:
 * `BitmapFactory`'s `inSampleSize`; JVM/iOS: Skia rasters straight into a target-sized `Surface`
 * from a lazily-decoded `Image`) — see each actual's KDoc.
 *
 * Must be called off the main/UI thread; this function itself does not switch dispatchers.
 */
internal expect fun decodeImageBitmapDownscaled(bytes: ByteArray, maxDimensionPx: Int): DecodedImage

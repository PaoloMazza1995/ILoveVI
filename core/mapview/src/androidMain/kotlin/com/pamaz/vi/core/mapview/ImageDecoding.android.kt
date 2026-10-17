package com.pamaz.vi.core.mapview

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Reads only [BitmapFactory.Options.inJustDecodeBounds] first (no pixels decoded yet), picks the
 * largest power-of-two [BitmapFactory.Options.inSampleSize] that still reaches
 * [downscaledSize]'s target, then decodes once at that subsampled size — the full-resolution
 * bitmap is never allocated when downscaling, satisfying "never keep two full-size copies in
 * memory" for the Android target.
 */
internal actual fun decodeImageBitmapDownscaled(
    bytes: ByteArray,
    maxDimensionPx: Int
): DecodedImage {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val originalWidth = bounds.outWidth
    val originalHeight = bounds.outHeight

    val target = downscaledSize(originalWidth, originalHeight, maxDimensionPx)
    var sampleSize = 1
    while (originalWidth / (sampleSize * 2) >= target.widthPx &&
        originalHeight / (sampleSize * 2) >= target.heightPx
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        ?: error("BitmapFactory could not decode the image ($originalWidth x $originalHeight source)")
    return DecodedImage(bitmap.asImageBitmap(), originalWidth, originalHeight)
}

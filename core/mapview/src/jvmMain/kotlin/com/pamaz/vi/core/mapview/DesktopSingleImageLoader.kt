package com.pamaz.vi.core.mapview

import java.io.File

/**
 * Builds a [MapContent.SingleImage] from a maintainer-chosen local PNG/JPEG file — the desktop
 * "Open image" mode `:tools:map-editor` (step 5B) is expected to offer, so a placement session can
 * start from any local image without a backend, same as [rememberDevMapImage] does for the
 * bundled dev map. Runs the same decode-and-downscale-to-[MAX_SINGLE_IMAGE_DIMENSION_PX] pipeline;
 * call this off the UI thread (e.g. from a coroutine on `Dispatchers.IO`), same as
 * [rememberDevMapImage] does internally.
 *
 * @throws java.io.IOException if [file] doesn't exist or can't be decoded as an image.
 */
fun loadSingleImageMapContentFromFile(file: File): MapContent.SingleImage {
    val decoded = decodeImageBitmapDownscaled(file.readBytes(), MAX_SINGLE_IMAGE_DIMENSION_PX)
    logMapViewDebug(
        "Loaded ${file.name}: original ${decoded.originalWidthPx}x${decoded.originalHeightPx}px, " +
                "used ${decoded.bitmap.width}x${decoded.bitmap.height}px.",
    )
    return MapContent.SingleImage(decoded.bitmap)
}

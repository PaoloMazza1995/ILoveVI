package com.pamaz.vi.core.mapview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.pamaz.vi.design.IloveViColors
import ilovevi.core.mapview.generated.resources.Res
import ilovevi.core.mapview.generated.resources.map_dev
import ilovevi.core.mapview.generated.resources.map_dev_fallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.rememberResourceEnvironment

/** Longer side, in pixels, that [rememberDevMapImage] ever decodes a bitmap at — see [downscaledSize]. */
internal const val MAX_SINGLE_IMAGE_DIMENSION_PX = 4096

private const val ULTIMATE_PLACEHOLDER_WIDTH_PX = 512
private const val ULTIMATE_PLACEHOLDER_HEIGHT_PX = 384

/** `println`s [message] only when [MapViewBuildConfig.singleImageDevModeEnabled] — never in a release build. */
internal fun logMapViewDebug(message: String) {
    if (MapViewBuildConfig.singleImageDevModeEnabled) {
        println("[core:mapview] $message")
    }
}

/**
 * A flat [IloveViColors.bg800] bitmap with no text or grid — the absolute last resort if even
 * `map_dev_fallback.png` can't be read (it ships bundled in this module, so this should never
 * actually trigger outside a broken build). No cross-platform text/shape drawing is attempted
 * here on purpose: [loadDevMapImageOrPlaceholder] already has a real grid-with-labels placeholder
 * (`map_dev_fallback.png`, generated the same way as `:tools:tilegen`'s `PlaceholderMapImage`)
 * for the common "the real dev image is missing" case; this one only needs to never crash.
 */
internal fun generateUltimatePlaceholderBitmap(): ImageBitmap {
    val bitmap = ImageBitmap(ULTIMATE_PLACEHOLDER_WIDTH_PX, ULTIMATE_PLACEHOLDER_HEIGHT_PX)
    val canvas = Canvas(bitmap)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(
            ULTIMATE_PLACEHOLDER_WIDTH_PX.toFloat(),
            ULTIMATE_PLACEHOLDER_HEIGHT_PX.toFloat()
        ),
    ) {
        drawRect(color = IloveViColors.bg800, size = size)
    }
    return bitmap
}

/**
 * The real decision logic behind [rememberDevMapImage], factored out as a plain, generic suspend
 * function so it's unit-testable without Compose, real platform resource I/O, or even a real
 * [ImageBitmap] (see `DevMapImageTest`, which uses `T = String`) — this function only cares about
 * *which* loader ran, never about what an "image" actually is. A release build ([devModeEnabled]
 * `false`) never calls [loadDevImage] at all; otherwise the real dev image is tried first, then
 * the bundled grid-with-labels fallback ([loadFallbackImage]), then [generateUltimatePlaceholder]
 * — a failure at any step is [log]ged and falls through to the next one, never thrown. Each loader
 * is responsible for its own detailed (e.g. dimension) logging; this function only logs
 * chain-level fallback events.
 */
internal suspend fun <T> loadDevMapImageOrPlaceholder(
    devModeEnabled: Boolean,
    loadDevImage: suspend () -> T,
    loadFallbackImage: suspend () -> T,
    generateUltimatePlaceholder: () -> T,
    log: (String) -> Unit = {},
): T {
    suspend fun loadFallbackOrUltimate(): T = try {
        loadFallbackImage()
    } catch (e: Exception) {
        log("map_dev_fallback.png missing or failed to decode (${e.message}) — using the in-memory placeholder.")
        generateUltimatePlaceholder()
    }

    if (!devModeEnabled) {
        log("single-image dev mode disabled (release build) — using the bundled placeholder.")
        return loadFallbackOrUltimate()
    }

    return try {
        loadDevImage()
    } catch (e: Exception) {
        log("map_dev.png missing or failed to decode (${e.message}) — using the bundled placeholder.")
        loadFallbackOrUltimate()
    }
}

/**
 * Loads the bundled dev map image (`composeResources/drawable/map_dev.png`) off the main thread,
 * downscaling it to [MAX_SINGLE_IMAGE_DIMENSION_PX] if needed — see [loadDevMapImageOrPlaceholder]
 * for the full fallback chain. Returns `null` while loading; show a loading placeholder (e.g. a
 * plain surface) for that brief window.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun rememberDevMapImage(): ImageBitmap? {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    val environment = rememberResourceEnvironment()

    LaunchedEffect(Unit) {
        image = withContext(Dispatchers.Default) {
            loadDevMapImageOrPlaceholder(
                devModeEnabled = MapViewBuildConfig.singleImageDevModeEnabled,
                loadDevImage = {
                    val decoded = decodeImageBitmapDownscaled(
                        getDrawableResourceBytes(environment, Res.drawable.map_dev),
                        MAX_SINGLE_IMAGE_DIMENSION_PX,
                    )
                    logMapViewDebug(
                        "map_dev.png loaded: original ${decoded.originalWidthPx}x${decoded.originalHeightPx}px, " +
                                "used ${decoded.bitmap.width}x${decoded.bitmap.height}px.",
                    )
                    decoded.bitmap
                },
                loadFallbackImage = {
                    val decoded = decodeImageBitmapDownscaled(
                        getDrawableResourceBytes(environment, Res.drawable.map_dev_fallback),
                        MAX_SINGLE_IMAGE_DIMENSION_PX,
                    )
                    logMapViewDebug("map_dev_fallback.png loaded: ${decoded.bitmap.width}x${decoded.bitmap.height}px.")
                    decoded.bitmap
                },
                generateUltimatePlaceholder = ::generateUltimatePlaceholderBitmap,
                log = ::logMapViewDebug,
            )
        }
    }

    return image
}

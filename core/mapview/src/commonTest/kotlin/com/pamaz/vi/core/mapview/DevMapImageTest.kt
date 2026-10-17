package com.pamaz.vi.core.mapview

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers the "missing resource" acceptance check without any real platform resource I/O, Compose
 * composition, or [androidx.compose.ui.graphics.ImageBitmap] (constructing a real one needs a
 * native Skia binary this test environment doesn't have on its test classpath — `T = String`
 * fakes sidestep that entirely): [loadDevMapImageOrPlaceholder] is a plain, generic suspend
 * function over injected lambdas (see its KDoc), so each step of the fallback chain can be forced
 * independently.
 */
class DevMapImageTest {

    @Test
    fun devModeEnabled_devImageReadable_returnsTheDevImage() = runTest {
        val result = loadDevMapImageOrPlaceholder(
            devModeEnabled = true,
            loadDevImage = { "dev" },
            loadFallbackImage = { error("fallback should not be read when the dev image succeeds") },
            generateUltimatePlaceholder = { error("should not reach the ultimate placeholder") },
        )
        assertEquals("dev", result)
    }

    @Test
    fun devModeEnabled_devImageFails_fallsBackToTheBundledFallbackImage() = runTest {
        val result = loadDevMapImageOrPlaceholder(
            devModeEnabled = true,
            loadDevImage = { throw IllegalStateException("map_dev.png not found") },
            loadFallbackImage = { "fallback" },
            generateUltimatePlaceholder = { error("should not reach the ultimate placeholder") },
        )
        assertEquals("fallback", result)
    }

    @Test
    fun devModeEnabled_bothImagesFail_fallsBackToTheUltimatePlaceholder() = runTest {
        val result = loadDevMapImageOrPlaceholder(
            devModeEnabled = true,
            loadDevImage = { throw IllegalStateException("missing") },
            loadFallbackImage = { throw IllegalStateException("missing too") },
            generateUltimatePlaceholder = { "ultimate" },
        )
        assertEquals("ultimate", result)
    }

    @Test
    fun devModeDisabled_neverReadsTheDevImage_usesTheFallbackDirectly() = runTest {
        var devImageReadAttempted = false
        val result = loadDevMapImageOrPlaceholder(
            devModeEnabled = false,
            loadDevImage = { devImageReadAttempted = true; "dev" },
            loadFallbackImage = { "fallback" },
            generateUltimatePlaceholder = { error("should not reach the ultimate placeholder") },
        )
        assertEquals("fallback", result)
        assertFalse(
            devImageReadAttempted,
            "a release build must never attempt to read the bundled dev image"
        )
    }

    @Test
    fun devModeDisabled_fallbackAlsoFails_fallsBackToTheUltimatePlaceholder() = runTest {
        val result = loadDevMapImageOrPlaceholder(
            devModeEnabled = false,
            loadDevImage = { error("should not be called in a release build") },
            loadFallbackImage = { throw IllegalStateException("missing") },
            generateUltimatePlaceholder = { "ultimate" },
        )
        assertEquals("ultimate", result)
    }

    @Test
    fun aFallbackStep_logsAClearMessage() = runTest {
        val messages = mutableListOf<String>()
        loadDevMapImageOrPlaceholder(
            devModeEnabled = true,
            loadDevImage = { throw IllegalStateException("boom") },
            loadFallbackImage = { "fallback" },
            generateUltimatePlaceholder = { error("should not reach the ultimate placeholder") },
            log = { messages += it },
        )
        assertTrue(
            messages.any { "map_dev.png" in it },
            "expected a log message naming map_dev.png, got: $messages"
        )
    }
}

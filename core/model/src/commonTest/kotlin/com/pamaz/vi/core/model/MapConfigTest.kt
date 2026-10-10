package com.pamaz.vi.core.model

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Instant

class MapConfigTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun validConfig(
        widthPx: Int = 4096,
        heightPx: Int = 2048,
        tileUrlTemplate: String = "https://example.com/tiles/{z}/{x}/{y}.png",
        minZoom: Int = 0,
        maxZoom: Int = 5,
    ) = MapConfig(
        mapVersion = 1,
        sourceImageUrl = "https://example.com/map.png",
        widthPx = widthPx,
        heightPx = heightPx,
        tileUrlTemplate = tileUrlTemplate,
        minZoom = minZoom,
        maxZoom = maxZoom,
        updatedAt = now,
    )

    @Test
    fun acceptsValidConfig() {
        validConfig()
    }

    @Test
    fun rejectsZeroWidth() {
        assertFailsWith<IllegalArgumentException> { validConfig(widthPx = 0) }
    }

    @Test
    fun rejectsNegativeWidth() {
        assertFailsWith<IllegalArgumentException> { validConfig(widthPx = -1) }
    }

    @Test
    fun rejectsZeroHeight() {
        assertFailsWith<IllegalArgumentException> { validConfig(heightPx = 0) }
    }

    @Test
    fun rejectsNegativeHeight() {
        assertFailsWith<IllegalArgumentException> { validConfig(heightPx = -1) }
    }

    @Test
    fun rejectsMinZoomGreaterThanMaxZoom() {
        assertFailsWith<IllegalArgumentException> { validConfig(minZoom = 6, maxZoom = 5) }
    }

    @Test
    fun acceptsMinZoomEqualToMaxZoom() {
        validConfig(minZoom = 3, maxZoom = 3)
    }

    @Test
    fun rejectsTemplateMissingZPlaceholder() {
        assertFailsWith<IllegalArgumentException> {
            validConfig(tileUrlTemplate = "https://example.com/tiles/{x}/{y}.png")
        }
    }

    @Test
    fun rejectsTemplateMissingXPlaceholder() {
        assertFailsWith<IllegalArgumentException> {
            validConfig(tileUrlTemplate = "https://example.com/tiles/{z}/{y}.png")
        }
    }

    @Test
    fun rejectsTemplateMissingYPlaceholder() {
        assertFailsWith<IllegalArgumentException> {
            validConfig(tileUrlTemplate = "https://example.com/tiles/{z}/{x}.png")
        }
    }
}

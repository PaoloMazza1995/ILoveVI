package com.pamaz.vi.core.model.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TileUrlTest {

    @Test
    fun buildsUrlFromTemplate() {
        val url = TileUrl.build("https://example.com/tiles/{z}/{x}/{y}.png", z = 4, x = 7, y = 2)
        assertEquals("https://example.com/tiles/4/7/2.png", url)
    }

    @Test
    fun rejectsTemplateMissingZPlaceholder() {
        assertFailsWith<IllegalArgumentException> {
            TileUrl.build("https://example.com/tiles/{x}/{y}.png", z = 1, x = 1, y = 1)
        }
    }

    @Test
    fun rejectsTemplateMissingXPlaceholder() {
        assertFailsWith<IllegalArgumentException> {
            TileUrl.build("https://example.com/tiles/{z}/{y}.png", z = 1, x = 1, y = 1)
        }
    }

    @Test
    fun rejectsTemplateMissingYPlaceholder() {
        assertFailsWith<IllegalArgumentException> {
            TileUrl.build("https://example.com/tiles/{z}/{x}.png", z = 1, x = 1, y = 1)
        }
    }

    @Test
    fun rejectsTemplateMissingAllPlaceholders() {
        assertFailsWith<IllegalArgumentException> {
            TileUrl.build("https://example.com/tiles.png", z = 1, x = 1, y = 1)
        }
    }
}

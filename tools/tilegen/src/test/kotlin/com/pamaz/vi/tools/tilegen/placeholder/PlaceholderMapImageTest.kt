package com.pamaz.vi.tools.tilegen.placeholder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class PlaceholderMapImageTest {

    @Test
    fun generatesRequestedDimensions() {
        val image = PlaceholderMapImage.generate(widthPx = 500, heightPx = 300, gridSpacingPx = 50)
        assertEquals(500, image.width)
        assertEquals(300, image.height)
    }

    @Test
    fun drawsGridLinesAtExpectedSpacing() {
        val image = PlaceholderMapImage.generate(widthPx = 500, heightPx = 300, gridSpacingPx = 50)
        val betweenLines = image.getRGB(25, 25)
        val onGridLine = image.getRGB(50, 25)
        assertNotEquals(betweenLines, onGridLine)
    }

    @Test
    fun rejectsNonPositiveDimensions() {
        assertFailsWith<IllegalArgumentException> {
            PlaceholderMapImage.generate(
                widthPx = 0,
                heightPx = 100
            )
        }
        assertFailsWith<IllegalArgumentException> {
            PlaceholderMapImage.generate(
                widthPx = 100,
                heightPx = -1
            )
        }
    }

    @Test
    fun rejectsNonPositiveGridSpacing() {
        assertFailsWith<IllegalArgumentException> {
            PlaceholderMapImage.generate(widthPx = 100, heightPx = 100, gridSpacingPx = 0)
        }
    }

    @Test
    fun defaultDimensionsMatchTheRequestedPlaceholderSpec() {
        assertEquals(8192, PlaceholderMapImage.DEFAULT_WIDTH_PX)
        assertEquals(6144, PlaceholderMapImage.DEFAULT_HEIGHT_PX)
    }
}

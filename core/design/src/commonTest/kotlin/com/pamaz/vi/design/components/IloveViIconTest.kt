package com.pamaz.vi.design.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [IloveViIcon] is plain data (an [androidx.compose.ui.graphics.vector.ImageVector] per enum
 * constant) — no Composition is needed to build or inspect one, so this runs as a regular
 * `kotlin.test` test, not a Compose UI test.
 */
class IloveViIconTest {

    @Test
    fun every_icon_hasA24x24Viewport() {
        for (icon in IloveViIcon.entries) {
            val vector = icon.imageVector
            assertEquals(24f, vector.viewportWidth, "$icon viewportWidth")
            assertEquals(24f, vector.viewportHeight, "$icon viewportHeight")
        }
    }

    @Test
    fun every_icon_buildsANonEmptyVector() {
        for (icon in IloveViIcon.entries) {
            assertTrue(icon.imageVector.root.size > 0, "$icon has no path data")
        }
    }

    @Test
    fun thereAreExactly13Icons_andNoHealth() {
        assertEquals(13, IloveViIcon.entries.size)
        assertTrue(IloveViIcon.entries.none { it.name.contains("HEALTH", ignoreCase = true) })
    }
}

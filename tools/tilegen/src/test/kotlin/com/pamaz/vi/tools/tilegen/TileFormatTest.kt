package com.pamaz.vi.tools.tilegen

import io.ktor.http.ContentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TileFormatTest {

    @Test
    fun fromExtension_parsesBothFormatsCaseInsensitively() {
        assertEquals(TileFormat.WEBP, TileFormat.fromExtension("webp"))
        assertEquals(TileFormat.WEBP, TileFormat.fromExtension("WEBP"))
        assertEquals(TileFormat.PNG, TileFormat.fromExtension("png"))
    }

    @Test
    fun fromExtension_rejectsUnknownFormat() {
        assertFailsWith<IllegalArgumentException> { TileFormat.fromExtension("jpg") }
    }

    @Test
    fun tileContentType_matchesEachFormat() {
        assertEquals(ContentType.Image.WEBP, tileContentType(TileFormat.WEBP))
        assertEquals(ContentType.Image.PNG, tileContentType(TileFormat.PNG))
    }
}

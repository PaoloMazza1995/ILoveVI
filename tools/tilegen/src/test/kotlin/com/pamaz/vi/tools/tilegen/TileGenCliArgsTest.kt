package com.pamaz.vi.tools.tilegen

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TileGenCliArgsTest {

    @Test
    fun parsesRequiredFlagsWithDefaults() {
        val args =
            parseTileGenArgs(listOf("--source", "https://example.com/map.png", "--out", "/tmp/out"))

        assertEquals("https://example.com/map.png", args.source)
        assertEquals("/tmp/out", args.outputDir)
        assertFalse(args.upload)
        assertFalse(args.dryRun)
        assertNull(args.mapVersion)
        assertEquals(256, args.tileSize)
        assertEquals(TilePyramidGenerator.DEFAULT_BACKGROUND_COLOR, args.backgroundColor)
    }

    @Test
    fun parsesAllFlags() {
        val args = parseTileGenArgs(
            listOf(
                "--source", "/local/map.png",
                "--out", "/tmp/out",
                "--upload",
                "--dry-run",
                "--map-version", "7",
                "--tile-size", "512",
                "--background-color", "112233",
            ),
        )

        assertEquals("/local/map.png", args.source)
        assertTrue(args.upload)
        assertTrue(args.dryRun)
        assertEquals(7, args.mapVersion)
        assertEquals(512, args.tileSize)
        assertEquals((0xFF shl 24) or 0x112233, args.backgroundColor)
    }

    @Test
    fun missingSource_throws() {
        assertFailsWith<TileGenArgsException> { parseTileGenArgs(listOf("--out", "/tmp/out")) }
    }

    @Test
    fun missingOut_throws() {
        assertFailsWith<TileGenArgsException> {
            parseTileGenArgs(
                listOf(
                    "--source",
                    "https://example.com/map.png"
                )
            )
        }
    }

    @Test
    fun flagMissingValue_throws() {
        assertFailsWith<TileGenArgsException> {
            parseTileGenArgs(listOf("--source", "https://example.com/map.png", "--out"))
        }
    }

    @Test
    fun nonIntegerMapVersion_throws() {
        assertFailsWith<TileGenArgsException> {
            parseTileGenArgs(
                listOf(
                    "--source",
                    "https://example.com/map.png",
                    "--out",
                    "/tmp/out",
                    "--map-version",
                    "abc"
                ),
            )
        }
    }

    @Test
    fun unknownFlag_throws() {
        assertFailsWith<TileGenArgsException> {
            parseTileGenArgs(
                listOf(
                    "--source",
                    "https://example.com/map.png",
                    "--out",
                    "/tmp/out",
                    "--bogus"
                )
            )
        }
    }

    @Test
    fun parsesPlaceholderArgsWithDefaults() {
        val args = parsePlaceholderArgs(listOf("--out", "/tmp/placeholder.png"))
        assertEquals("/tmp/placeholder.png", args.outputPath)
        assertEquals(8192, args.width)
        assertEquals(6144, args.height)
    }

    @Test
    fun parsesPlaceholderArgsWithOverrides() {
        val args = parsePlaceholderArgs(
            listOf(
                "--out",
                "/tmp/placeholder.png",
                "--width",
                "100",
                "--height",
                "80"
            )
        )
        assertEquals(100, args.width)
        assertEquals(80, args.height)
    }

    @Test
    fun parseHexColor_rejectsWrongLength() {
        assertFailsWith<TileGenArgsException> { parseHexColor("ABC") }
    }

    @Test
    fun parseHexColor_rejectsNonHexDigits() {
        assertFailsWith<TileGenArgsException> { parseHexColor("ZZZZZZ") }
    }

    @Test
    fun parseHexColor_acceptsOptionalHashPrefix() {
        assertEquals(parseHexColor("112233"), parseHexColor("#112233"))
    }
}

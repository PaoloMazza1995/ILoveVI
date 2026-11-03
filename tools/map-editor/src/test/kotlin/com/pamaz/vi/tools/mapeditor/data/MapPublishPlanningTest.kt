package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.tools.mapeditor.model.DbPin
import com.pamaz.vi.tools.mapeditor.model.MapVersionAction
import com.pamaz.vi.tools.tilegen.ImageDimensions
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MapPublishPlanningTest {

    // --- validate (§2a: format, readability, file size) ----------------------------------------

    @Test
    fun validate_rejectsAMissingFile() {
        val message = MapPublishPlanning.validate(File("/no/such/file-ilovevi-test.png"))
        assertTrue(message != null && message.contains("Could not read"))
    }

    @Test
    fun validate_rejectsAnUnsupportedExtension() {
        val tempDir = kotlin.io.path.createTempDirectory().toFile()
        val file = File(tempDir, "map.gif").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val message = MapPublishPlanning.validate(file)
        assertTrue(message != null && message.contains("PNG or JPEG"))
    }

    @Test
    fun validate_rejectsAFileOverTheSizeLimit() {
        val tempDir = kotlin.io.path.createTempDirectory().toFile()
        val file = File(tempDir, "map.png").apply {
            writeBytes(ByteArray((MapPublishPlanning.MAX_SOURCE_IMAGE_BYTES + 1).toInt()))
        }
        val message = MapPublishPlanning.validate(file)
        assertTrue(message != null && message.contains("MB"))
    }

    @Test
    fun validate_acceptsAReadablePngOrJpegUnderTheSizeLimit() {
        val tempDir = kotlin.io.path.createTempDirectory().toFile()
        val png = File(tempDir, "map.png").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val jpeg = File(tempDir, "map.jpeg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        assertNull(MapPublishPlanning.validate(png))
        assertNull(MapPublishPlanning.validate(jpeg))
    }

    // --- validateDimensions (§2a: "size at most 16384 px per side") -----------------------------

    @Test
    fun validateDimensions_rejectsEitherSideOverTheLimit() {
        assertTrue(
            MapPublishPlanning.validateDimensions(
                ImageDimensions(
                    MapPublishPlanning.MAX_DIMENSION_PX + 1,
                    100
                )
            ) != null
        )
        assertTrue(
            MapPublishPlanning.validateDimensions(
                ImageDimensions(
                    100,
                    MapPublishPlanning.MAX_DIMENSION_PX + 1
                )
            ) != null
        )
    }

    @Test
    fun validateDimensions_acceptsExactlyTheLimit() {
        assertNull(
            MapPublishPlanning.validateDimensions(
                ImageDimensions(
                    MapPublishPlanning.MAX_DIMENSION_PX,
                    MapPublishPlanning.MAX_DIMENSION_PX
                )
            )
        )
    }

    // --- decideVersion (§3) ----------------------------------------------------------------------

    private fun pin(mapVersion: Int) = DbPin(
        id = "pin-$mapVersion",
        category = EntryCategory.WEAPON,
        entryId = "weapon-1",
        position = NormalizedPoint(0.1f, 0.1f),
        zone = null,
        mapVersion = mapVersion,
    )

    @Test
    fun decideVersion_withNoCurrentConfig_startsAtVersion1() {
        val decision =
            MapPublishPlanning.decideVersion(currentMapVersion = null, allPins = emptyList())
        assertEquals(1, decision.mapVersion)
        assertEquals(MapVersionAction.NEW_VERSION, decision.action)
        assertEquals(0, decision.pinsToReposition)
    }

    @Test
    fun decideVersion_withNoPinsOnTheCurrentVersion_updatesInPlace() {
        // the broken-v1-with-no-pins-yet case from the task's own acceptance criteria -- also
        // exercised for a current row that's MapConfigResult.Invalid, not just Loaded, since
        // decideVersion only ever needs the raw map_version, never the rest of the row.
        val decision =
            MapPublishPlanning.decideVersion(currentMapVersion = 1, allPins = emptyList())
        assertEquals(1, decision.mapVersion)
        assertEquals(MapVersionAction.UPDATE_IN_PLACE, decision.action)
        assertEquals(0, decision.pinsToReposition)
    }

    @Test
    fun decideVersion_withPinsOnTheCurrentVersion_bumpsToANewVersion_andWarnsHowManyMustMove() {
        val pins = listOf(pin(1), pin(1), pin(1))
        val decision = MapPublishPlanning.decideVersion(currentMapVersion = 1, allPins = pins)
        assertEquals(2, decision.mapVersion)
        assertEquals(MapVersionAction.NEW_VERSION, decision.action)
        assertEquals(3, decision.pinsToReposition)
    }

    // --- buildPlan (§2b: tile size 256, zoom/tile-count from the image's own size) ----------------

    @Test
    fun buildPlan_4096Image_reachesZoom4With256Tiles() {
        val plan = MapPublishPlanning.buildPlan(
            file = File("map.png"),
            dimensions = ImageDimensions(4096, 4096),
            tileUrlTemplate = "https://example.com/{z}/{x}/{y}.png",
            decision = MapPublishPlanning.VersionDecision(1, MapVersionAction.NEW_VERSION, 0),
        )
        assertEquals(256, plan.tileSize)
        assertEquals(0, plan.minZoom)
        assertEquals(4, plan.maxZoom)
        // 1 + 4 + 16 + 64 + 256 tiles across zoom 0..4 for an exact power-of-two square image.
        assertEquals(1 + 4 + 16 + 64 + 256, plan.tileCount)
    }

    /**
     * Task item 7's own example numbers — 341/1365/5461/21845 tiles for 4096/8192/16384/32768px —
     * are each the geometric series `1 + 4 + 16 + ... + 4^maxZoom` across every pyramid level, not
     * just the base one; [buildPlan_4096Image_reachesZoom4With256Tiles] above only ever asserted
     * the base level's own 256, not the cumulative total [MapPublishPlan.tileCount] actually is.
     * This covers the exact four sizes task item 7a names, plus the native-zoom-from-size formula
     * (`2^z * 256 >= the long side`: 4096->4, 8192->5, 16384->6, 32768->7) each one implies.
     */
    @Test
    fun buildPlan_8192Image_reachesZoom5With1365Tiles() {
        val plan = MapPublishPlanning.buildPlan(
            file = File("map.png"),
            dimensions = ImageDimensions(8192, 8192),
            tileUrlTemplate = "https://example.com/{z}/{x}/{y}.png",
            decision = MapPublishPlanning.VersionDecision(1, MapVersionAction.NEW_VERSION, 0),
        )
        assertEquals(5, plan.maxZoom)
        assertEquals(1 + 4 + 16 + 64 + 256 + 1024, plan.tileCount)
        assertEquals(1365, plan.tileCount)
    }

    @Test
    fun buildPlan_16384Image_reachesZoom6With5461Tiles() {
        val plan = MapPublishPlanning.buildPlan(
            file = File("map.png"),
            dimensions = ImageDimensions(16384, 16384),
            tileUrlTemplate = "https://example.com/{z}/{x}/{y}.png",
            decision = MapPublishPlanning.VersionDecision(1, MapVersionAction.NEW_VERSION, 0),
        )
        assertEquals(6, plan.maxZoom)
        assertEquals(5461, plan.tileCount)
    }

    @Test
    fun buildPlan_32768Image_reachesZoom7With21845Tiles() {
        val plan = MapPublishPlanning.buildPlan(
            file = File("map.png"),
            dimensions = ImageDimensions(32768, 32768),
            tileUrlTemplate = "https://example.com/{z}/{x}/{y}.png",
            decision = MapPublishPlanning.VersionDecision(1, MapVersionAction.NEW_VERSION, 0),
        )
        assertEquals(7, plan.maxZoom)
        assertEquals(21845, plan.tileCount)
    }

    @Test
    fun validateDimensions_accepts32768_theRaisedMaxDimension() {
        assertEquals(null, MapPublishPlanning.validateDimensions(ImageDimensions(32768, 32768)))
    }

    @Test
    fun validateDimensions_rejectsOver32768() {
        assertTrue(MapPublishPlanning.validateDimensions(ImageDimensions(32769, 100)) != null)
    }

    @Test
    fun buildPlan_256Image_isASingleTileAtZoom0() {
        val plan = MapPublishPlanning.buildPlan(
            file = File("map.png"),
            dimensions = ImageDimensions(256, 256),
            tileUrlTemplate = "https://example.com/{z}/{x}/{y}.png",
            decision = MapPublishPlanning.VersionDecision(1, MapVersionAction.NEW_VERSION, 0),
        )
        assertEquals(0, plan.maxZoom)
        assertEquals(1, plan.tileCount)
    }

    @Test
    fun buildPlan_nonSquareImage_usesItsOwnWidthAndHeight_noPadding() {
        // core:model's TileScheme already supports independent width/height (MapViewport does
        // too) -- confirmed by reading both before writing this test -- so a non-square image is
        // never padded to a square canvas here.
        val plan = MapPublishPlanning.buildPlan(
            file = File("map.png"),
            dimensions = ImageDimensions(1000, 700),
            tileUrlTemplate = "https://example.com/{z}/{x}/{y}.png",
            decision = MapPublishPlanning.VersionDecision(1, MapVersionAction.NEW_VERSION, 0),
        )
        assertEquals(1000, plan.widthPx)
        assertEquals(700, plan.heightPx)
        assertEquals(2, plan.maxZoom) // largest side 1000 -> 256 -> 512 -> 1024 (2 doublings)
    }
}

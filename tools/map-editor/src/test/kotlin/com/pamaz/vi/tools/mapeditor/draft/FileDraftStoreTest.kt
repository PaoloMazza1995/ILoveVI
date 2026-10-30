package com.pamaz.vi.tools.mapeditor.draft

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.tools.mapeditor.model.DraftEntry
import com.pamaz.vi.tools.mapeditor.model.DraftPin
import com.pamaz.vi.tools.mapeditor.model.DraftSnapshot
import com.pamaz.vi.tools.mapeditor.model.EntrySource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** EDT-61 (autosave round trip) and the §6 edge case "Draft file damaged." */
class FileDraftStoreTest {

    private lateinit var file: java.io.File
    private lateinit var store: FileDraftStore

    @BeforeTest
    fun setUp() {
        file = java.io.File.createTempFile("draft-store-test", ".json")
        file.delete()
        store = FileDraftStore(file)
    }

    @AfterTest
    fun tearDown() {
        file.delete()
        file.parentFile?.listFiles { f -> f.name.startsWith(file.name) && f.name.contains(".corrupt-") }
            ?.forEach { it.delete() }
    }

    @Test
    fun load_onAFreshFile_returnsAnEmptySnapshot() {
        val snapshot = store.load()
        assertTrue(snapshot.pins.isEmpty())
        assertTrue(snapshot.entries.isEmpty())
    }

    @Test
    fun saveThenLoad_roundTripsPinsAndEntries() {
        val entry = DraftEntry(
            id = "entry-1",
            category = EntryCategory.WEAPON,
            subType = "PISTOL",
            name = "Falcon .45",
            zone = "Vice Beach",
            description = "A compact pistol.",
            extra = mapOf("price" to "500"),
            uploadedImageUrl = "https://example.com/pistol.png",
        )
        val pins = listOf(
            DraftPin(
                id = "pin-1",
                position = NormalizedPoint(0.25f, 0.75f),
                category = EntryCategory.WEAPON,
                subType = "PISTOL",
                entry = EntrySource.Db("weapon-9"),
            ),
            DraftPin(
                id = "pin-2",
                position = NormalizedPoint(0.5f, 0.5f),
                category = EntryCategory.WEAPON,
                subType = "PISTOL",
                entry = EntrySource.Draft("entry-1"),
            ),
        )

        store.save(DraftSnapshot(pins, listOf(entry)))
        val loaded = store.load()

        assertEquals(pins, loaded.pins)
        assertEquals(listOf(entry), loaded.entries)
    }

    @Test
    fun saveThenLoad_roundTripsAPendingImageFile_byItsPath() {
        val image = java.io.File.createTempFile("draft-store-image-test", ".png")
        try {
            val entry = DraftEntry(
                id = "entry-1",
                category = EntryCategory.ITEM,
                subType = "AMMO",
                name = "Spare Mag",
                imageFile = image,
            )
            store.save(DraftSnapshot(emptyList(), listOf(entry)))

            val loaded = store.load()
            assertEquals(image.path, loaded.entries.single().imageFile?.path)
        } finally {
            image.delete()
        }
    }

    @Test
    fun load_onADamagedFile_returnsEmpty_andKeepsACopyOfTheOriginal() {
        file.writeText("{ not valid json at all")

        val snapshot = store.load()

        assertTrue(snapshot.pins.isEmpty())
        assertTrue(snapshot.entries.isEmpty())
        val corruptCopies =
            file.parentFile.listFiles { f -> f.name.startsWith(file.name) && f.name.contains(".corrupt-") }
        assertEquals(1, corruptCopies?.size)
        assertEquals("{ not valid json at all", corruptCopies!!.first().readText())
    }

    @Test
    fun clear_removesTheFile_soANewLoadIsEmptyAgain() {
        store.save(
            DraftSnapshot(
                listOf(
                    DraftPin(
                        "p",
                        NormalizedPoint(0f, 0f),
                        EntryCategory.ITEM,
                        "AMMO"
                    )
                ), emptyList()
            )
        )
        assertTrue(file.exists())

        store.clear()

        assertFalse(file.exists())
        assertTrue(store.load().pins.isEmpty())
    }
}

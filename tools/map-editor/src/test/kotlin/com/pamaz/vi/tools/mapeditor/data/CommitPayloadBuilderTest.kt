package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.tools.mapeditor.data.fake.FakeCommitService
import com.pamaz.vi.tools.mapeditor.model.DraftEntry
import com.pamaz.vi.tools.mapeditor.model.DraftPin
import com.pamaz.vi.tools.mapeditor.model.EntryFields
import com.pamaz.vi.tools.mapeditor.model.EntrySource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** "The payload built for the RPC (normalized coordinates, ids, map version)" (acceptance). */
class CommitPayloadBuilderTest {

    @Test
    fun build_stampsTheMapVersionOnEveryPinAndThePayloadItself() {
        val pin = DraftPin(
            id = "pin-1",
            position = NormalizedPoint(0.25f, 0.75f),
            category = EntryCategory.WEAPON,
            subType = "PISTOL",
            entry = EntrySource.Db("weapon-1"),
        )
        val payload = CommitPayloadBuilder.build(
            mapVersion = 7,
            draftPins = listOf(pin),
            draftEntries = emptyList()
        )

        val commitPin = payload.pins.single()
        assertEquals("pin-1", commitPin.id)
        assertEquals(EntryCategory.WEAPON, commitPin.category)
        assertEquals("weapon-1", commitPin.entryId)
        assertEquals(NormalizedPoint(0.25f, 0.75f), commitPin.position)
        assertEquals(7, commitPin.mapVersion)
        assertEquals(7, payload.mapVersion)
    }

    @Test
    fun build_resolvesAPinLinkedToADraftEntry_toThatEntrysIdAndCategory() {
        val entry = DraftEntry(
            id = "entry-1",
            category = EntryCategory.ITEM,
            subType = "ARMOR",
            name = "Riot Vest"
        )
        val pin = DraftPin(
            id = "pin-1",
            position = NormalizedPoint(0.1f, 0.1f),
            category = EntryCategory.ITEM,
            subType = "ARMOR",
            entry = EntrySource.Draft("entry-1"),
        )

        val payload = CommitPayloadBuilder.build(1, listOf(pin), listOf(entry))

        val commitPin = payload.pins.single()
        assertEquals("entry-1", commitPin.entryId)
        assertEquals(EntryCategory.ITEM, commitPin.category)
    }

    @Test
    fun build_dropsAnIncompletePin_insteadOfSendingAMalformedRow() {
        val pin =
            DraftPin("pin-1", NormalizedPoint(0f, 0f), EntryCategory.WEAPON, "PISTOL", entry = null)
        val payload = CommitPayloadBuilder.build(1, listOf(pin), emptyList())
        assertTrue(payload.pins.isEmpty())
    }

    @Test
    fun build_dropsAPinPointingAtADraftEntryThatIsntInThePayload() {
        val pin = DraftPin(
            "pin-1",
            NormalizedPoint(0f, 0f),
            EntryCategory.WEAPON,
            "PISTOL",
            entry = EntrySource.Draft("missing")
        )
        val payload = CommitPayloadBuilder.build(1, listOf(pin), emptyList())
        assertTrue(payload.pins.isEmpty())
    }

    @Test
    fun build_mapsEveryWeaponColumn_includingPriceAndAmmoType() {
        val entry = DraftEntry(
            id = "w-1",
            category = EntryCategory.WEAPON,
            subType = "RIFLE",
            name = "Rifle",
            zone = "Vinewood",
            description = "A rifle.",
            extra = mapOf(EntryFields.PRICE to "500", EntryFields.AMMO_TYPE to "5.56mm"),
            uploadedImageUrl = "https://example.com/rifle.png",
        )

        val commitEntry = CommitPayloadBuilder.build(1, emptyList(), listOf(entry)).entries.single()

        assertEquals("w-1", commitEntry.id)
        assertEquals("Rifle", commitEntry.name)
        assertEquals("A rifle.", commitEntry.description)
        assertEquals("RIFLE", commitEntry.subType)
        assertEquals("Vinewood", commitEntry.zone)
        assertEquals(500, commitEntry.price)
        assertEquals("5.56mm", commitEntry.ammoType)
        assertEquals("https://example.com/rifle.png", commitEntry.imageUrl)
    }

    @Test
    fun build_leavesPriceAndAmmoTypeNull_forVehiclesAndItems() {
        val entry = DraftEntry(
            id = "v-1",
            category = EntryCategory.VEHICLE,
            subType = "CAR",
            name = "Comet"
        )
        val commitEntry = CommitPayloadBuilder.build(1, emptyList(), listOf(entry)).entries.single()
        assertNull(commitEntry.price)
        assertNull(commitEntry.ammoType)
    }

    @Test
    fun fakeCommitService_recordsThePayloadItWasCalledWith() = runTest {
        val fake = FakeCommitService(delayMs = 0)
        val payload = CommitPayload(mapVersion = 1, entries = emptyList(), pins = emptyList())

        val result = fake.commit(payload, emptyMap())

        assertEquals(0, result.entriesCreated)
        assertEquals(0, result.pinsCreated)
    }
}

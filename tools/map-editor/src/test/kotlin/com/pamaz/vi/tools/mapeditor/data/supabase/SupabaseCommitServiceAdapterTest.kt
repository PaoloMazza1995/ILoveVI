package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.tools.mapeditor.data.CommitEntry
import com.pamaz.vi.tools.mapeditor.data.CommitPayload
import com.pamaz.vi.tools.mapeditor.data.CommitPin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupabaseCommitServiceAdapterTest {
    @Test
    fun translatesEntriesAndPinsToTheRpcPayloadShape() {
        val payload = CommitPayload(
            mapVersion = 2,
            entries = listOf(
                CommitEntry(
                    id = "weapon-1",
                    category = EntryCategory.WEAPON,
                    name = "Falcon .45",
                    description = "A pistol",
                    subType = "PISTOL",
                    zone = "Vice Beach",
                    imageUrl = "https://example.com/falcon.png",
                    price = 500,
                    ammoType = "9mm",
                ),
            ),
            pins = listOf(
                CommitPin(
                    id = "pin-1",
                    category = EntryCategory.WEAPON,
                    entryId = "weapon-1",
                    position = NormalizedPoint(0.1f, 0.2f),
                    zone = "Vice Beach",
                    mapVersion = 2,
                ),
            ),
        )

        val rpcPayload = payload.toSupabasePayload()

        assertEquals(2, rpcPayload.mapVersion)
        val entry = rpcPayload.entries.single()
        assertEquals("weapon-1", entry.id)
        assertEquals(500, entry.price)
        assertEquals("9mm", entry.ammoType)
        assertTrue(entry.attributes.isEmpty())

        val pin = rpcPayload.pins.single()
        assertEquals("pin-1", pin.id)
        assertEquals("weapon-1", pin.entryId)
        assertEquals(NormalizedPoint(0.1f, 0.2f), pin.position)
    }
}

package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.tools.mapeditor.model.DraftEntry
import com.pamaz.vi.tools.mapeditor.model.DraftPin
import com.pamaz.vi.tools.mapeditor.model.EntryFields
import com.pamaz.vi.tools.mapeditor.model.EntrySource
import java.io.File

/** One weapon/vehicle/item to upsert — matches `public.commit_map_edits`'s payload shape (FA §7). */
data class CommitEntry(
    val id: String,
    val category: EntryCategory,
    val name: String,
    val description: String,
    val subType: String,
    val zone: String?,
    val imageUrl: String?,
    val price: Int?,
    val ammoType: String?,
)

/** One `map_pins` row to upsert. */
data class CommitPin(
    val id: String,
    val category: EntryCategory,
    val entryId: String,
    val position: NormalizedPoint,
    val zone: String?,
    val mapVersion: Int,
)

/** The whole body of one Commit — everything `public.commit_map_edits` needs in a single call (EDT-72). */
data class CommitPayload(
    val mapVersion: Int,
    val entries: List<CommitEntry>,
    val pins: List<CommitPin>,
)

/** Counts `public.commit_map_edits` reports back — what the Commit success dialog shows (EDT-74). */
data class CommitResult(
    val entriesCreated: Int,
    val entriesUpdated: Int,
    val pinsCreated: Int,
    val pinsMoved: Int,
    val imagesUploaded: Int,
)

/** The atomic server-side commit of a draft (EDT-70..77) — real implementation in `data.supabase`, not wired by default. */
interface CommitService {
    suspend fun commit(payload: CommitPayload, pendingImages: Map<String, File>): CommitResult
}

/**
 * Builds the exact [CommitPayload] a Commit sends, from the current draft — a pure function so
 * the normalized coordinates/ids/map version it produces can be asserted on directly in tests,
 * with no network involved (see `CommitPayloadBuilderTest`). A pin with no resolvable entry (still
 * incomplete) is dropped rather than sent malformed — `EditorState.commit()` never calls this
 * while any pin is incomplete anyway (EDT-70), this is just defense in depth.
 */
object CommitPayloadBuilder {
    fun build(
        mapVersion: Int,
        draftPins: List<DraftPin>,
        draftEntries: List<DraftEntry>
    ): CommitPayload {
        val entriesById = draftEntries.associateBy { it.id }
        val entries = draftEntries.map { entry ->
            CommitEntry(
                id = entry.id,
                category = entry.category,
                name = entry.name,
                description = entry.description,
                subType = entry.subType,
                zone = entry.zone,
                imageUrl = entry.uploadedImageUrl,
                price = entry.extra[EntryFields.PRICE]?.toIntOrNull(),
                ammoType = entry.extra[EntryFields.AMMO_TYPE],
            )
        }
        val pins = draftPins.mapNotNull { pin ->
            val entryId = when (val source = pin.entry) {
                null -> return@mapNotNull null
                is EntrySource.Db -> source.id
                is EntrySource.Draft -> {
                    if (source.id !in entriesById) return@mapNotNull null
                    source.id
                }
            }
            CommitPin(
                id = pin.id,
                category = pin.category,
                entryId = entryId,
                position = pin.position,
                zone = null,
                mapVersion = mapVersion,
            )
        }
        return CommitPayload(mapVersion, entries, pins)
    }
}

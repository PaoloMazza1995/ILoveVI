package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.tools.mapeditor.data.CommitPayload
import com.pamaz.vi.tools.mapeditor.data.CommitResult
import com.pamaz.vi.tools.mapeditor.data.CommitService
import java.io.File

/**
 * Adapts [SupabasePostgrestCommitRepository] (the real `commit_map_edits` RPC caller, built
 * against this package's own older [CommitPayload]/[CommitEntry]/[CommitPin]/[CommitResult]
 * shapes — see that file's KDoc) to this step's [CommitService]/`data.CommitPayload` shape. The
 * two payload shapes carry the same information laid out differently (this step's entries have
 * dedicated `price`/`ammoType` fields instead of a generic `attributes` map); [CommitResult] only
 * differs by one field, [CommitResult.imagesUploaded] — the RPC itself never reports that count,
 * so it's taken from `pendingImages.size`: [SupabasePostgrestCommitRepository.commit] uploads
 * every one of them *before* calling the RPC, so by the time it returns without throwing, that
 * many images are known to have actually uploaded.
 */
class SupabaseCommitServiceAdapter(
    private val repository: CommitRepository,
) : CommitService {
    override suspend fun commit(
        payload: CommitPayload,
        pendingImages: Map<String, File>,
    ): CommitResult {
        val result = repository.commit(payload.toSupabasePayload(), pendingImages)
        return CommitResult(
            entriesCreated = result.entriesCreated,
            entriesUpdated = result.entriesUpdated,
            pinsCreated = result.pinsCreated,
            pinsMoved = result.pinsMoved,
            imagesUploaded = pendingImages.size,
        )
    }
}

internal fun CommitPayload.toSupabasePayload() =
    CommitPayload(
        mapVersion = mapVersion,
        entries = entries.map { entry ->
            CommitEntry(
                id = entry.id,
                category = entry.category,
                name = entry.name,
                description = entry.description,
                subType = entry.subType,
                zone = entry.zone,
                imageUrl = entry.imageUrl,
                price = entry.price,
                ammoType = entry.ammoType,
                attributes = emptyMap(),
            )
        },
        pins = pins.map { pin ->
            CommitPin(
                id = pin.id,
                category = pin.category,
                entryId = pin.entryId,
                position = pin.position,
                zone = pin.zone,
                mapVersion = pin.mapVersion,
            )
        },
    )

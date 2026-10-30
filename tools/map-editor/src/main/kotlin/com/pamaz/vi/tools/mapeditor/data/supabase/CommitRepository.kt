package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArrayBuilder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.util.UUID

/** One weapon/vehicle/item to upsert. */
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
    val attributes: Map<String, String>,
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

/** The whole body of one Commit — everything `public.commit_map_edits` needs in a single call. */
data class CommitPayload(
    val mapVersion: Int,
    val entries: List<CommitEntry>,
    val pins: List<CommitPin>,
)

/** Counts `public.commit_map_edits` reports back — what the Commit confirmation/success dialogs show. */
data class CommitResult(
    val entriesCreated: Int,
    val entriesUpdated: Int,
    val pinsCreated: Int,
    val pinsMoved: Int,
)

private fun CommitPayload.toRpcJson(): JsonObject = buildJsonObject {
    put("map_version", mapVersion)
    putJsonObject("entries") {
        EntryCategory.entries.forEach { category ->
            putJsonArray(category.tableName()) {
                entries.filter { it.category == category }.forEach { entry ->
                    addJsonObject {
                        put("id", entry.id)
                        put("name", entry.name)
                        put("description", entry.description)
                        put("sub_type", entry.subType)
                        entry.zone?.let { put("zone", it) }
                        entry.imageUrl?.let { put("image_url", it) }
                        if (category == EntryCategory.WEAPON) {
                            entry.price?.let { put("price", it) }
                            entry.ammoType?.let { put("ammo_type", it) }
                        }
                        putJsonObject("attributes") {
                            entry.attributes.forEach { (k, v) ->
                                put(
                                    k,
                                    v
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    putJsonArray("pins") {
        pins.forEach { pin ->
            addJsonObject {
                put("id", pin.id)
                put("category", pin.category.name)
                put("entry_id", pin.entryId)
                put("x", pin.position.x)
                put("y", pin.position.y)
                pin.zone?.let { put("zone", it) }
                put("map_version", pin.mapVersion)
            }
        }
    }
}

private fun EntryCategory.tableName(): String = when (this) {
    EntryCategory.WEAPON -> "weapons"
    EntryCategory.VEHICLE -> "vehicles"
    EntryCategory.ITEM -> "items"
}

private inline fun JsonArrayBuilder.addJsonObject(
    builderAction: JsonObjectBuilder.() -> Unit,
) = add(buildJsonObject(builderAction))

@Serializable
private data class CommitCountsRow(
    @SerialName("entries_created") val entriesCreated: Int,
    @SerialName("entries_updated") val entriesUpdated: Int,
    @SerialName("pins_created") val pinsCreated: Int,
    @SerialName("pins_moved") val pinsMoved: Int,
)

/** The atomic server-side commit of a draft — see `backend/supabase/migrations/20261021120003_commit_map_edits_rpc.sql`. */
interface CommitRepository {
    suspend fun commit(payload: CommitPayload, pendingImages: Map<String, File>): CommitResult
}

/**
 * Real [CommitRepository]: uploads every pending image to the `entry-images` bucket first (inside
 * the same [commit] call — a failed upload never reaches the RPC, so the draft is kept exactly as
 * the acceptance requires), then calls `commit_map_edits` via `postgrest.rpc`, verified against
 * the real `postgrest-kt-jvm-3.1.4` sources
 * (`Postgrest.rpc(function: String, parameters: JsonObject, request: ... = {}): PostgrestResult`)
 * and `storage-kt-jvm-3.1.4`'s `BucketApi.upload`/`publicUrl` — never the service-role key, same
 * anon-key client used everywhere else in this module.
 */
class SupabasePostgrestCommitRepository(private val client: SupabaseClient) : CommitRepository {
    override suspend fun commit(
        payload: CommitPayload,
        pendingImages: Map<String, File>,
    ): CommitResult {
        val bucket = client.storage.from("entry-images")
        val uploadedUrls = pendingImages.mapValues { (_, file) ->
            val path = "${UUID.randomUUID()}-${file.name}"
            bucket.upload(path, file.readBytes())
            bucket.publicUrl(path)
        }
        val withImages = payload.copy(
            entries = payload.entries.map { entry ->
                uploadedUrls[entry.id]?.let { entry.copy(imageUrl = it) } ?: entry
            },
        )
        val result = client.postgrest.rpc(
            "commit_map_edits",
            buildJsonObject { put("payload", withImages.toRpcJson()) },
        )
        val counts = result.decodeAs<CommitCountsRow>()
        return CommitResult(
            entriesCreated = counts.entriesCreated,
            entriesUpdated = counts.entriesUpdated,
            pinsCreated = counts.pinsCreated,
            pinsMoved = counts.pinsMoved,
        )
    }
}

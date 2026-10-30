package com.pamaz.vi.tools.mapeditor.draft

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.tools.mapeditor.data.DraftStore
import com.pamaz.vi.tools.mapeditor.model.DraftEntry
import com.pamaz.vi.tools.mapeditor.model.DraftPin
import com.pamaz.vi.tools.mapeditor.model.DraftSnapshot
import com.pamaz.vi.tools.mapeditor.model.EntrySource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class DraftPinRow(
    val id: String,
    val x: Float,
    val y: Float,
    val category: String,
    val subType: String,
    val entrySourceType: String? = null,
    val entrySourceId: String? = null,
)

@Serializable
private data class DraftEntryRow(
    val id: String,
    val category: String,
    val subType: String,
    val name: String,
    val zone: String? = null,
    val description: String = "",
    val extra: Map<String, String> = emptyMap(),
    val pendingImagePath: String? = null,
    val uploadedImageUrl: String? = null,
)

@Serializable
private data class DraftFile(
    val pins: List<DraftPinRow> = emptyList(),
    val entries: List<DraftEntryRow> = emptyList(),
)

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

/**
 * Real local-file persistence for the draft (EDT-61: "saved to a local JSON file after every
 * change and reloaded when the tool starts"). A picked-but-not-yet-uploaded image is persisted as
 * its local file path ([DraftEntry.imageFile] isn't directly serializable) and reopened as a
 * [File] on [load].
 *
 * **Damaged file** (§6 edge case): a parse failure returns an empty [DraftSnapshot] rather than
 * throwing, and the unreadable file is copied aside to `<name>.corrupt-<epochMillis>` next to it
 * before anything else touches it, so the original bytes aren't lost even though they can't be
 * understood.
 */
class FileDraftStore(private val file: File) : DraftStore {
    override fun load(): DraftSnapshot {
        if (!file.exists()) return DraftSnapshot()
        val draft = try {
            json.decodeFromString(DraftFile.serializer(), file.readText())
        } catch (e: Exception) {
            val corruptCopy =
                File(file.parentFile, "${file.name}.corrupt-${System.currentTimeMillis()}")
            file.copyTo(corruptCopy, overwrite = true)
            return DraftSnapshot()
        }
        val pins = draft.pins.map {
            DraftPin(
                id = it.id,
                position = NormalizedPoint(it.x, it.y),
                category = EntryCategory.valueOf(it.category),
                subType = it.subType,
                entry = it.toEntrySource(),
            )
        }
        val entries = draft.entries.map {
            DraftEntry(
                id = it.id,
                category = EntryCategory.valueOf(it.category),
                subType = it.subType,
                name = it.name,
                zone = it.zone,
                description = it.description,
                extra = it.extra,
                imageFile = it.pendingImagePath?.let(::File),
                uploadedImageUrl = it.uploadedImageUrl,
            )
        }
        return DraftSnapshot(pins, entries)
    }

    override fun save(snapshot: DraftSnapshot) {
        val draft = DraftFile(
            pins = snapshot.pins.map {
                DraftPinRow(
                    id = it.id,
                    x = it.position.x,
                    y = it.position.y,
                    category = it.category.name,
                    subType = it.subType,
                    entrySourceType = when (it.entry) {
                        null -> null
                        is EntrySource.Db -> "db"
                        is EntrySource.Draft -> "draft"
                    },
                    entrySourceId = it.entry?.id,
                )
            },
            entries = snapshot.entries.map {
                DraftEntryRow(
                    id = it.id,
                    category = it.category.name,
                    subType = it.subType,
                    name = it.name,
                    zone = it.zone,
                    description = it.description,
                    extra = it.extra,
                    pendingImagePath = it.imageFile?.path,
                    uploadedImageUrl = it.uploadedImageUrl,
                )
            },
        )
        file.writeText(json.encodeToString(DraftFile.serializer(), draft))
    }

    override fun clear() {
        if (file.exists()) file.delete()
    }

    private fun DraftPinRow.toEntrySource(): EntrySource? {
        val id = entrySourceId ?: return null
        return when (entrySourceType) {
            "db" -> EntrySource.Db(id)
            "draft" -> EntrySource.Draft(id)
            else -> null
        }
    }
}

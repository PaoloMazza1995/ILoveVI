package com.pamaz.vi.tools.mapeditor.model

import com.pamaz.vi.core.model.EntryCategory
import java.io.File

/**
 * A not-yet-committed weapon/vehicle/item, created inline from the Inspector's "New entry" form
 * (Map Editor Functional Analysis EDT-54). [id] is a client-generated UUID that becomes the real
 * `weapons`/`vehicles`/`items.id` on Commit, so a retried Commit can never create a duplicate row
 * (EDT-76). [extra] holds only the category-specific columns from
 * [EntryFields.extraFieldsFor] (e.g. a weapon's price/ammo type) — `name`/`subType`/`zone`/
 * `description` are dedicated properties here, not stuffed into a generic map, since the
 * Functional Analysis lists them as the form's own always-present fields.
 */
data class DraftEntry(
    val id: String,
    val category: EntryCategory,
    val subType: String,
    val name: String,
    val zone: String? = null,
    val description: String = "",
    val extra: Map<String, String> = emptyMap(),
    /** Picked but not yet uploaded — cleared (and [uploadedImageUrl] set) once Commit's upload succeeds. */
    val imageFile: File? = null,
    val uploadedImageUrl: String? = null,
) {
    /** EDT-54/§3 "Edge cases": a draft entry is usable once it at least has a name. */
    val isComplete: Boolean get() = name.isNotBlank()
}

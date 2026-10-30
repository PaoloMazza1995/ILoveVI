package com.pamaz.vi.tools.mapeditor.model

/**
 * Where a [DraftPin]'s linked entry currently lives: an already-saved database row, or a
 * not-yet-committed [DraftEntry]. **Not** `core:model`'s `EntryRef` (which encodes *which entity
 * type* — weapon/vehicle/item — a reference points to): this is a different axis entirely, "has
 * this id been committed yet," and reusing `EntryRef`'s name for it would conflate two unrelated
 * concepts in the same codebase.
 */
sealed interface EntrySource {
    val id: String

    data class Db(override val id: String) : EntrySource
    data class Draft(override val id: String) : EntrySource
}

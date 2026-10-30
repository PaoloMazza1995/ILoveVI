package com.pamaz.vi.tools.mapeditor.model

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint

/**
 * One pin in the local draft (Map Editor Functional Analysis EDT-30..33) — not written to the
 * database until Commit. [category]/[subType] are set at placement time from the toolbar's active
 * chips and stay the pin's own authoritative state (EDT-57: "for a pin with no entry, the category
 * can be changed in the Inspector") — they're independent of [entry], which starts `null`
 * ("incomplete," EDT-33) until linked to an existing or newly-created entry.
 */
data class DraftPin(
    val id: String,
    val position: NormalizedPoint,
    val category: EntryCategory,
    val subType: String,
    val entry: EntrySource? = null,
) {
    /**
     * EDT-50/§3 "Incomplete pin": true once [entry] exists and, for a still-draft entry, that
     * entry's own name isn't blank. [draftEntriesById] is the current draft's entries keyed by
     * [DraftEntry.id] — the caller (state layer) already has this map built for other reasons.
     */
    fun isComplete(draftEntriesById: Map<String, DraftEntry>): Boolean = when (val source = entry) {
        null -> false
        is EntrySource.Db -> true
        is EntrySource.Draft -> draftEntriesById[source.id]?.isComplete == true
    }
}

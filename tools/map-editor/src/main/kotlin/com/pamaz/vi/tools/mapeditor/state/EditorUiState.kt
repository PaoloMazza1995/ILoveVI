package com.pamaz.vi.tools.mapeditor.state

import com.pamaz.vi.core.mapview.MapContent
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.tools.mapeditor.data.RawMapConfigRow
import com.pamaz.vi.tools.mapeditor.model.DbPin
import com.pamaz.vi.tools.mapeditor.model.DraftEntry
import com.pamaz.vi.tools.mapeditor.model.DraftPin
import com.pamaz.vi.tools.mapeditor.model.EditMode
import com.pamaz.vi.tools.mapeditor.model.EntrySource
import com.pamaz.vi.tools.mapeditor.model.EntrySummary
import com.pamaz.vi.tools.mapeditor.model.MapLoadError
import com.pamaz.vi.tools.mapeditor.model.MapPublishSnapshot
import com.pamaz.vi.tools.mapeditor.model.Session

/**
 * One immutable snapshot of everything the UI needs — [EditorState] is the only thing that ever
 * produces a new one (via `copy`), the UI only ever reads it. No `androidx.compose.runtime` types
 * anywhere in this file or in [EditorState]: a plain `kotlin.test` function can construct one,
 * call an intent, and assert on the result with no Compose runtime/test rule involved at all.
 */
data class EditorUiState(
    val session: Session? = null,
    val isLoading: Boolean = true,
    val mapConfig: MapConfig? = null,
    val mapContent: MapContent? = null,
    val dbEntries: List<EntrySummary> = emptyList(),
    val dbPins: List<DbPin> = emptyList(),
    val showDbPins: Boolean = true,
    val mode: EditMode = EditMode.PLACE,
    val activeCategory: EntryCategory = EntryCategory.WEAPON,
    val activeSubType: String = WeaponType.PISTOL.name,
    /** "Place another pin of this entry" (EDT-32) — every click in Place mode adds a pin for this entry instead of an empty one. */
    val activeEntry: EntrySource? = null,
    val draftPins: List<DraftPin> = emptyList(),
    val draftEntries: List<DraftEntry> = emptyList(),
    val selectedPinIds: Set<String> = emptySet(),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    /** EDT-62: the Discard button needs a second click; this is "the first click already happened." */
    val discardArmed: Boolean = false,
    val autosaveStatus: String = "",
    val lastError: String? = null,
    /** Set instead of leaving [isLoading] stuck `true` forever when the map failed to load (PLT-08). */
    val mapLoadError: MapLoadError? = null,
    /** Set when `map_config`'s current row exists but failed validation (bad template, size, zoom range, or a blank source image URL) — the raw columns `ui/MapArea.kt`'s empty state shows alongside [mapLoadError]'s message. */
    val invalidMapConfigRow: RawMapConfigRow? = null,
    /** Non-null while the "Load map image..." dialog is open — see `state/MapPublisher.kt`. */
    val mapPublish: MapPublishSnapshot? = null,
) {
    val draftEntriesById: Map<String, DraftEntry> get() = draftEntries.associateBy { it.id }

    /** EDT-50/EDT-70: pins with no entry, or a draft entry with a still-blank name. */
    val incompleteDraftPinIds: List<String>
        get() = draftPins.filter { !it.isComplete(draftEntriesById) }.map { it.id }

    val hasDraftChanges: Boolean get() = draftPins.isNotEmpty() || draftEntries.isNotEmpty()

    /** EDT-70: enabled only when the draft has changes, every pin is complete, and the account is admin. */
    val isCommittable: Boolean
        get() = session?.isAdmin == true && hasDraftChanges && incompleteDraftPinIds.isEmpty()

    val selectedPins: List<DraftPin> get() = draftPins.filter { it.id in selectedPinIds }
}

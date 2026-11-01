package com.pamaz.vi.feature.map.map

import com.pamaz.vi.core.data.repository.MapPinWithEntryName
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.MapConfig

/** What the selected pin's summary card shows (MAP-04): just enough to render it and open Detail. */
data class SelectedPinSummary(
    val pinId: String,
    val entryId: String,
    val category: EntryCategory,
    val name: String,
    val zone: String?,
)

/** The Map tab's single UI state (MAP-01..11, MST-07). */
sealed interface MapUiState {
    data object Loading : MapUiState

    /** MST-07: the map config hasn't synced yet (or none was ever published) — a clear message, never a blank screen. */
    data object MapConfigMissing : MapUiState

    data class Error(val message: String) : MapUiState

    data class Loaded(
        val mapConfig: MapConfig,
        val pins: List<MapPinWithEntryName>,
        val activeCategories: Set<EntryCategory>,
        val selectedPinId: String?,
        val selectedSummary: SelectedPinSummary?,
        val showLabels: Boolean,
    ) : MapUiState {
        /** True once the active category/sub-type filters leave nothing to show (MAP-09's empty hint). */
        val isEmpty: Boolean get() = pins.isEmpty()
    }
}

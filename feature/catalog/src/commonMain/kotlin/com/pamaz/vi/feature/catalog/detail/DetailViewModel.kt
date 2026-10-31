package com.pamaz.vi.feature.catalog.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pamaz.vi.core.data.repository.CatalogRepository
import com.pamaz.vi.core.data.repository.MapRepository
import com.pamaz.vi.core.model.EntryCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Backs the Detail screen (DET-01..04). [category]/[entryId] come from the nav route (step 8's
 * contract: `detail/{category}/{entryId}`) as constructor parameters, not mutable state — a
 * different entry is a different screen instance (and a different back-stack entry), never a
 * state change on this one.
 *
 * [category] failing to parse as a real [EntryCategory] (a malformed/stale deep link; never
 * happens for a link this app itself generated, since it always writes `EntryCategory.name`) is
 * treated the same as the entry simply not existing — [DetailUiState.NotFound] — rather than
 * crashing.
 */
class DetailViewModel(
    catalogRepository: CatalogRepository,
    mapRepository: MapRepository,
    category: String,
    entryId: String,
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> = run {
        val parsedCategory = EntryCategory.entries.firstOrNull { it.name == category }
        if (parsedCategory == null) {
            MutableStateFlow(DetailUiState.NotFound)
        } else {
            combine(
                catalogRepository.entry(parsedCategory, entryId),
                mapRepository.pinByEntryId(parsedCategory, entryId),
                mapRepository.currentMapConfig(),
            ) { entry, pin, mapConfig ->
                if (entry == null) DetailUiState.NotFound else DetailUiState.Loaded(
                    entry,
                    pin,
                    mapConfig
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DetailUiState.Loading,
    )
}

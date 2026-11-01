package com.pamaz.vi.feature.map.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pamaz.vi.core.data.repository.MapPinWithEntryName
import com.pamaz.vi.core.data.repository.MapRepository
import com.pamaz.vi.core.data.repository.SettingsRepository
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.feature.map.filters.MapFilterState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Default for MAP's optional pin labels when [SettingsRepository] hasn't stored a real preference
 * yet ([com.pamaz.vi.core.data.repository.Preferences] is "null until the first `setPreferences`
 * call", per that interface's own KDoc) — an unconfirmed judgment call, nothing in the Functional
 * Analysis specifies a default, chosen because showing labels is the more helpful choice on a
 * first run with no saved preference yet.
 */
private const val DEFAULT_SHOW_LABELS = true

/**
 * Backs the Map tab (MAP-01..11, MST-04..07). Offline-first, same as `:feature:catalog`'s view
 * models: every read comes from [MapRepository]/[SettingsRepository], never from `:core:network`
 * directly.
 *
 * Clustering is deliberately **not** done here: it needs the live camera (zoom/center/viewport),
 * which only exists as `:core:mapview`'s `MapViewerState`, a Compose object `MapScreen` creates
 * via `rememberMapViewerState` — so `MapScreen` itself computes the clustered
 * `List<MapViewerItem>` (via `PinClusterer`, recomputed whenever this state's `pins` or the
 * camera change) and this class only ever deals in domain pins and selection, never in screen
 * coordinates.
 *
 * [filterState] is a Koin `single`, not scoped to this view model — shared with the Filters
 * secondary screen (a different route/back-stack entry); see [MapFilterState]'s own KDoc.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel(
    private val mapRepository: MapRepository,
    private val settingsRepository: SettingsRepository,
    private val filterState: MapFilterState,
) : ViewModel() {

    private val selectedPinId = MutableStateFlow<String?>(null)
    private val searchQueryFlow = MutableStateFlow("")

    private val pins: Flow<List<MapPinWithEntryName>> =
        filterState.filters.flatMapLatest { filters ->
            // MapRepository/MapDao treat an empty subTypes set as "no sub-type filter" (match every
            // sub-type) — the exact opposite of what turning every sub-type chip off is supposed to
            // mean here (MAP-09's "map shows an empty hint"), so that case is special-cased to an
            // empty result instead of forwarding an empty set down to the repository.
            if (filters.subTypes.isEmpty()) {
                flowOf(emptyList())
            } else {
                mapRepository.pins(filters.categories, filters.subTypes)
            }
        }

    val uiState: StateFlow<MapUiState> = combine(
        mapRepository.currentMapConfig(),
        pins,
        filterState.filters,
        selectedPinId,
        settingsRepository.preferences(),
    ) { mapConfig, pinList, filters, selected, preferences ->
        if (mapConfig == null) {
            MapUiState.MapConfigMissing
        } else {
            MapUiState.Loaded(
                mapConfig = mapConfig,
                pins = pinList,
                activeCategories = filters.categories,
                selectedPinId = selected,
                selectedSummary = selected?.let { id -> pinList.summaryFor(id) },
                showLabels = preferences?.showLabels ?: DEFAULT_SHOW_LABELS,
            )
        }
    }
        .catch { error -> emit(MapUiState.Error(error.message ?: "Unknown error")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MapUiState.Loading,
        )

    /** Could-priority search (name/zone), scoped to whatever's currently visible under the active filters. */
    val searchQuery: StateFlow<String> = searchQueryFlow

    val searchResults: StateFlow<List<MapPinWithEntryName>> =
        combine(uiState, searchQueryFlow) { state, query ->
            val loaded = state as? MapUiState.Loaded
            when {
                query.isBlank() || loaded == null -> emptyList()
                else -> loaded.pins.filter { pin ->
                    pin.entryName.contains(query, ignoreCase = true) ||
                            pin.pin.zone?.contains(query, ignoreCase = true) == true
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
    }

    /** A pin (or cluster's representative tap target) was tapped — exactly one selection at a time. */
    fun onPinSelected(pinId: String) {
        selectedPinId.value = pinId
    }

    /** Tapping the map's empty background clears the current selection. */
    fun onBackgroundTapped() {
        selectedPinId.value = null
    }

    fun onCategoryToggled(category: EntryCategory) {
        filterState.toggleCategory(category)
    }

    /** "Show on map" (MAP-06): selects whichever currently-visible pin marks [entryId], if any. */
    fun selectByEntryId(entryId: String) {
        val loaded = uiState.value as? MapUiState.Loaded ?: return
        val match = loaded.pins.firstOrNull { it.pin.entryRef.rawEntryId() == entryId }
        if (match != null) selectedPinId.value = match.pin.id.value
    }

    private fun List<MapPinWithEntryName>.summaryFor(pinId: String): SelectedPinSummary? {
        val match = firstOrNull { it.pin.id.value == pinId } ?: return null
        return SelectedPinSummary(
            pinId = pinId,
            entryId = match.pin.entryRef.rawEntryId(),
            category = match.pin.category,
            name = match.entryName,
            zone = match.pin.zone,
        )
    }
}

private fun EntryRef.rawEntryId(): String = when (this) {
    is EntryRef.ToWeapon -> id.value
    is EntryRef.ToVehicle -> id.value
    is EntryRef.ToItem -> id.value
}

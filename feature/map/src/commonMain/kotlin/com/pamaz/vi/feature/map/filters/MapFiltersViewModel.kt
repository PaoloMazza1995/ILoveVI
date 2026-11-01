package com.pamaz.vi.feature.map.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.WeaponType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Backs the Filters secondary screen (MAP-05/MAP-09). Reads/writes the same [MapFilterState] the
 * Map tab's [com.pamaz.vi.feature.map.map.MapViewModel] reads — toggling a chip here is visible
 * on the Map tab immediately, with no separate "apply" step; "Show on map" is simply a navigation
 * action (`onBack`) at the screen layer, not something this class needs to model.
 */
class MapFiltersViewModel(private val filterState: MapFilterState) : ViewModel() {

    val uiState: StateFlow<MapFiltersUiState> = filterState.filters
        .map { filters ->
            MapFiltersUiState(
                groups = listOf(
                    groupFor(
                        EntryCategory.WEAPON,
                        WeaponType.entries.map { it.name },
                        filters.subTypes
                    ),
                    groupFor(
                        EntryCategory.VEHICLE,
                        VehicleType.entries.map { it.name },
                        filters.subTypes
                    ),
                    groupFor(
                        EntryCategory.ITEM,
                        ItemType.entries.map { it.name },
                        filters.subTypes
                    ),
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MapFiltersUiState(),
        )

    fun onSubTypeToggled(subType: String) {
        filterState.toggleSubType(subType)
    }

    /** MAP-09: turns every category and sub-type back on. */
    fun onReset() {
        filterState.reset()
    }

    private fun groupFor(
        category: EntryCategory,
        subTypeNames: List<String>,
        activeSubTypes: Set<String>,
    ): CategoryFilterGroup = CategoryFilterGroup(
        category = category,
        subTypes = subTypeNames.map { name -> SubTypeChip(name, active = name in activeSubTypes) },
    )
}

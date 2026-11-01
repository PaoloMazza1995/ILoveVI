package com.pamaz.vi.feature.map.filters

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.WeaponType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Every sub-type name [com.pamaz.vi.core.data.repository.MapRepository.pins] understands, from
 * every `core:model` sub-type enum, combined into one set — [MapRepository.pins]'s own `subTypes`
 * parameter is a single flat `Set<String>` applied across whichever categories are requested
 * (see that method's KDoc), so the three enums are unioned here rather than kept as three separate
 * sets that would each need their own plumbing down to the repository call.
 */
private fun allSubTypeNames(): Set<String> =
    WeaponType.entries.map { it.name }.toSet() +
            VehicleType.entries.map { it.name }.toSet() +
            ItemType.entries.map { it.name }.toSet()

private fun allCategories(): Set<EntryCategory> = EntryCategory.entries.toSet()

/** The Map/Filters screens' shared selection (MAP-05/MAP-09): both "all on" at first launch. */
data class MapFilters(
    val categories: Set<EntryCategory> = allCategories(),
    val subTypes: Set<String> = allSubTypeNames(),
)

/**
 * Holds the one [MapFilters] both the Map tab and the Filters secondary screen read/mutate.
 * Registered as a Koin `single` (see `MapFeatureModule.kt`), not a `viewModel`, specifically so it
 * survives navigating *between* those two different routes: Navigation Compose's
 * `saveState`/`restoreState` back-stack trick — the one `CatalogViewModel`'s KDoc relies on to
 * survive a *tab switch* — only keeps a `ViewModelStore` alive for the *same* route being
 * revisited; Map and Filters are two different routes, so a plain per-route `ViewModel` would
 * reset every time either screen's back-stack entry is torn down. A Koin singleton has no such
 * lifecycle tie, so it's simply still there either way, for as long as the app process is.
 */
class MapFilterState {
    private val _filters = MutableStateFlow(MapFilters())
    val filters: StateFlow<MapFilters> = _filters.asStateFlow()

    fun toggleCategory(category: EntryCategory) {
        _filters.update { current -> current.copy(categories = current.categories.toggling(category)) }
    }

    fun toggleSubType(subType: String) {
        _filters.update { current -> current.copy(subTypes = current.subTypes.toggling(subType)) }
    }

    /** MAP-09: turns every category and sub-type back on. */
    fun reset() {
        _filters.value = MapFilters()
    }

    private fun <T> Set<T>.toggling(value: T): Set<T> =
        if (value in this) this - value else this + value
}

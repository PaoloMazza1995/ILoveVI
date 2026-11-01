package com.pamaz.vi.feature.map.filters

import com.pamaz.vi.core.model.EntryCategory

/** One sub-type chip the Filters screen renders (MAP-05): the raw enum name plus whether it's active. */
data class SubTypeChip(val name: String, val active: Boolean)

/** One category's group of [SubTypeChip]s — MAP-05's "groups of chips by category and sub-type". */
data class CategoryFilterGroup(
    val category: EntryCategory,
    val subTypes: List<SubTypeChip>,
)

/** The Filters screen's single UI state. */
data class MapFiltersUiState(
    val groups: List<CategoryFilterGroup> = emptyList(),
) {
    /** MAP-09: true once every sub-type in every group is off — the Map's own empty hint explains why. */
    val isEverythingOff: Boolean get() = groups.isNotEmpty() && groups.all { it.subTypes.none { chip -> chip.active } }
}

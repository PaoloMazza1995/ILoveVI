package com.pamaz.vi.feature.catalog.catalog

import com.pamaz.vi.core.model.Entry
import com.pamaz.vi.core.model.EntryCategory

/**
 * The Catalog tab's single UI state (CAT-01..05). [isLoading] is only ever `true` before the
 * *first* database read for the current session completes — switching category or typing a
 * search query afterwards never shows it again, since every subsequent read is of the same
 * already-open local database (see [CatalogViewModel]'s KDoc).
 */
data class CatalogUiState(
    val category: EntryCategory = EntryCategory.WEAPON,
    val query: String = "",
    val isLoading: Boolean = true,
    val entries: List<Entry> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && entries.isEmpty()
}

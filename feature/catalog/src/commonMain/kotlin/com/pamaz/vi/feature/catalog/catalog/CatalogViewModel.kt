package com.pamaz.vi.feature.catalog.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pamaz.vi.core.data.repository.CatalogRepository
import com.pamaz.vi.core.model.Entry
import com.pamaz.vi.core.model.EntryCategory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

private const val SEARCH_DEBOUNCE_MILLIS = 300L
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Backs the Catalog tab (CAT-01..05). Offline-first per this module's own README: every read
 * comes from [CatalogRepository], which is itself backed by `:core:database` — nothing here ever
 * touches the network.
 *
 * [category]/[query] are plain in-memory [MutableStateFlow]s, not persisted anywhere — "survive
 * tab switches" (the roadmap step's requirement) comes for free from how this `ViewModel` is
 * scoped: `AppNavHost` (step 8) switches tabs via `popUpTo(startDestination){saveState=true}` +
 * `restoreState=true`, Navigation Compose's documented recipe for keeping a destination's
 * `NavBackStackEntry` (and therefore its `ViewModelStore`) alive while a different tab is in
 * front — so the *same* `CatalogViewModel` instance, with its already-set category/query, is
 * still there when the user comes back, with no extra persistence code needed in this class.
 *
 * The search query is debounced (typing fires a new DB query at most once every
 * [SEARCH_DEBOUNCE_MILLIS]), but [onStart] re-emits the *current* query immediately when
 * [entries] starts collecting, so switching category never waits out the debounce window to show
 * that category's already-known, un-searched list.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CatalogViewModel(private val catalogRepository: CatalogRepository) : ViewModel() {

    private val category = MutableStateFlow(EntryCategory.WEAPON)
    private val query = MutableStateFlow("")
    private val hasLoadedOnce = MutableStateFlow(false)

    private val entries: Flow<List<Entry>> = category
        .flatMapLatest { selectedCategory ->
            query.debounce(SEARCH_DEBOUNCE_MILLIS)
                .onStart { emit(query.value) }
                .distinctUntilChanged()
                .flatMapLatest { currentQuery -> entriesFor(selectedCategory, currentQuery) }
        }
        .onEach { hasLoadedOnce.value = true }

    val uiState: StateFlow<CatalogUiState> = combine(
        category,
        query,
        entries,
        hasLoadedOnce,
    ) { selectedCategory, currentQuery, entryList, loaded ->
        CatalogUiState(
            category = selectedCategory,
            query = currentQuery,
            isLoading = !loaded,
            entries = entryList,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CatalogUiState(isLoading = true),
    )

    fun onCategorySelected(newCategory: EntryCategory) {
        category.value = newCategory
    }

    fun onQueryChanged(newQuery: String) {
        query.value = newQuery
    }

    private fun entriesFor(category: EntryCategory, query: String): Flow<List<Entry>> =
        when (category) {
            EntryCategory.WEAPON ->
                if (query.isBlank()) catalogRepository.weapons() else catalogRepository.searchWeapons(
                    query
                )

            EntryCategory.VEHICLE ->
                if (query.isBlank()) catalogRepository.vehicles() else catalogRepository.searchVehicles(
                    query
                )

            EntryCategory.ITEM ->
                if (query.isBlank()) catalogRepository.items() else catalogRepository.searchItems(
                    query
                )
        }
}

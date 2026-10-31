package com.pamaz.vi.feature.catalog.catalog

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.feature.catalog.fake.FakeCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-01-01T00:00:00Z")

private fun weapon(id: String, name: String, zone: String? = null) = Weapon(
    id = WeaponId(id),
    name = name,
    description = "d",
    subType = WeaponType.PISTOL,
    zone = zone,
    updatedAt = NOW,
)

private fun vehicle(id: String, name: String) = Vehicle(
    id = VehicleId(id),
    name = name,
    description = "d",
    subType = VehicleType.CAR,
    updatedAt = NOW,
)

/**
 * `CatalogViewModel.uiState` is built on `viewModelScope`, which resolves `Dispatchers.Main` —
 * JVM unit tests have no real one, so [kotlinx.coroutines.test]'s own test dispatcher must be
 * installed as Main *before* the view model is constructed, or its `combine`/`flatMapLatest`/
 * `debounce` pipeline runs on a dispatcher this test's [advanceUntilIdle] has no control over and
 * never progresses within the test's (virtual) lifetime.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun categorySwitch_showsThatCategorysOwnEntries() = runTest(mainDispatcher) {
        val repository = FakeCatalogRepository()
        repository.setWeapons(listOf(weapon("weapon-1", "Pistol")))
        repository.setVehicles(listOf(vehicle("vehicle-1", "Sedan")))
        val viewModel = CatalogViewModel(repository)
        val collector = launch { viewModel.uiState.collect {} }

        advanceUntilIdle()
        assertEquals(EntryCategory.WEAPON, viewModel.uiState.value.category)
        assertEquals(listOf("Pistol"), viewModel.uiState.value.entries.map { it.name })

        viewModel.onCategorySelected(EntryCategory.VEHICLE)
        advanceUntilIdle()
        assertEquals(EntryCategory.VEHICLE, viewModel.uiState.value.category)
        assertEquals(listOf("Sedan"), viewModel.uiState.value.entries.map { it.name })

        collector.cancel()
    }

    @Test
    fun search_isCaseAndAccentInsensitive() = runTest(mainDispatcher) {
        val repository = FakeCatalogRepository()
        repository.setWeapons(listOf(weapon("weapon-1", "Café Pistol", zone = "Vinewood")))
        val viewModel = CatalogViewModel(repository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onQueryChanged("CAFE")
        advanceUntilIdle()
        assertEquals(listOf("Café Pistol"), viewModel.uiState.value.entries.map { it.name })

        viewModel.onQueryChanged("vinewood")
        advanceUntilIdle()
        assertEquals(listOf("Café Pistol"), viewModel.uiState.value.entries.map { it.name })

        collector.cancel()
    }

    @Test
    fun noMatches_isEmptyButNotLoading() = runTest(mainDispatcher) {
        val repository = FakeCatalogRepository()
        repository.setWeapons(listOf(weapon("weapon-1", "Pistol")))
        val viewModel = CatalogViewModel(repository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onQueryChanged("nothing matches this")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.entries.isEmpty())
        assertTrue(state.isEmpty)
        assertEquals(false, state.isLoading)

        collector.cancel()
    }

    @Test
    fun beforeAnyCollection_stateIsLoading() {
        val viewModel = CatalogViewModel(FakeCatalogRepository())
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.entries.isEmpty())
    }
}

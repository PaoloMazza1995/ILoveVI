package com.pamaz.vi.feature.map.map

import com.pamaz.vi.core.data.repository.MapPinWithEntryName
import com.pamaz.vi.core.data.repository.Preferences
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.feature.map.fake.FakeMapRepository
import com.pamaz.vi.feature.map.fake.FakeSettingsRepository
import com.pamaz.vi.feature.map.filters.MapFilterState
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-01-01T00:00:00Z")

private fun mapConfig() = MapConfig(
    mapVersion = 1,
    sourceImageUrl = "https://example.com/map.png",
    widthPx = 1000,
    heightPx = 1000,
    tileUrlTemplate = "https://example.com/{z}/{x}/{y}.png",
    minZoom = 0,
    maxZoom = 5,
    updatedAt = NOW,
)

private fun pin(
    id: String,
    entryRef: EntryRef,
    category: EntryCategory = entryRef.category,
    zone: String? = "Vinewood",
) = MapPinWithEntryName(
    pin = MapPin(
        id = MapPinId(id),
        entryRef = entryRef,
        position = NormalizedPoint(0.5f, 0.5f),
        zone = zone,
        mapVersion = 1,
        updatedAt = NOW,
    ),
    entryName = "Entry $id",
)

/**
 * `MapViewModel.uiState` is built on `viewModelScope` — see `CatalogViewModelTest`'s KDoc for why
 * a `StandardTestDispatcher` must be installed as `Dispatchers.Main` before construction.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        mapRepository: FakeMapRepository = FakeMapRepository(),
        settingsRepository: FakeSettingsRepository = FakeSettingsRepository(),
        filterState: MapFilterState = MapFilterState(),
    ): MapViewModel = MapViewModel(mapRepository, settingsRepository, filterState)

    @Test
    fun noMapConfig_isMapConfigMissing() = runTest(mainDispatcher) {
        val viewModel = viewModel()
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertIs<MapUiState.MapConfigMissing>(viewModel.uiState.value)
        collector.cancel()
    }

    @Test
    fun mapConfigPresent_isLoadedWithAllCategoriesActiveByDefault() = runTest(mainDispatcher) {
        val repository = FakeMapRepository()
        repository.setMapConfig(mapConfig())
        repository.setPins(listOf(pin("p1", EntryRef.ToWeapon(WeaponId("w1")))))
        val viewModel = viewModel(mapRepository = repository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = assertIs<MapUiState.Loaded>(viewModel.uiState.value)
        assertEquals(EntryCategory.entries.toSet(), state.activeCategories)
        assertEquals(1, state.pins.size)
        collector.cancel()
    }

    @Test
    fun categoryToggleOff_hidesThatCategorysPins() = runTest(mainDispatcher) {
        val repository = FakeMapRepository()
        repository.setMapConfig(mapConfig())
        repository.setPins(
            listOf(
                pin("p1", EntryRef.ToWeapon(WeaponId("w1"))),
                pin("p2", EntryRef.ToItem(ItemId("i1"))),
            )
        )
        val filterState = MapFilterState()
        val viewModel = viewModel(mapRepository = repository, filterState = filterState)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onCategoryToggled(EntryCategory.WEAPON)
        advanceUntilIdle()

        val state = assertIs<MapUiState.Loaded>(viewModel.uiState.value)
        assertEquals(setOf(EntryCategory.VEHICLE, EntryCategory.ITEM), state.activeCategories)
        assertEquals(listOf("p2"), state.pins.map { it.pin.id.value })
        collector.cancel()
    }

    @Test
    fun everySubTypeOff_leavesPinsEmpty() = runTest(mainDispatcher) {
        val repository = FakeMapRepository()
        repository.setMapConfig(mapConfig())
        repository.setPins(listOf(pin("p1", EntryRef.ToWeapon(WeaponId("w1")))))
        val filterState = MapFilterState()
        val viewModel = viewModel(mapRepository = repository, filterState = filterState)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue((viewModel.uiState.value as MapUiState.Loaded).pins.isNotEmpty())

        val allSubTypes = WeaponType.entries.map { it.name } +
                VehicleType.entries.map { it.name } +
                ItemType.entries.map { it.name }
        allSubTypes.forEach { name -> filterState.toggleSubType(name) }
        advanceUntilIdle()

        val state = assertIs<MapUiState.Loaded>(viewModel.uiState.value)
        assertTrue(state.pins.isEmpty())
        assertTrue(state.isEmpty)
        collector.cancel()
    }

    @Test
    fun resettingFilters_turnsEverythingBackOn() = runTest(mainDispatcher) {
        val repository = FakeMapRepository()
        repository.setMapConfig(mapConfig())
        repository.setPins(listOf(pin("p1", EntryRef.ToWeapon(WeaponId("w1")))))
        val filterState = MapFilterState()
        val viewModel = viewModel(mapRepository = repository, filterState = filterState)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onCategoryToggled(EntryCategory.WEAPON)
        filterState.toggleSubType(WeaponType.PISTOL.name)
        advanceUntilIdle()
        assertTrue((viewModel.uiState.value as MapUiState.Loaded).pins.isEmpty())

        filterState.reset()
        advanceUntilIdle()

        val state = assertIs<MapUiState.Loaded>(viewModel.uiState.value)
        assertEquals(EntryCategory.entries.toSet(), state.activeCategories)
        assertEquals(1, state.pins.size)
        collector.cancel()
    }

    @Test
    fun selectingAndClearingAPin() = runTest(mainDispatcher) {
        val repository = FakeMapRepository()
        repository.setMapConfig(mapConfig())
        repository.setPins(listOf(pin("p1", EntryRef.ToWeapon(WeaponId("w1")), zone = "Vinewood")))
        val viewModel = viewModel(mapRepository = repository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onPinSelected("p1")
        advanceUntilIdle()
        var state = assertIs<MapUiState.Loaded>(viewModel.uiState.value)
        assertEquals("p1", state.selectedPinId)
        assertEquals("Entry p1", state.selectedSummary?.name)
        assertEquals("Vinewood", state.selectedSummary?.zone)
        assertEquals(EntryCategory.WEAPON, state.selectedSummary?.category)
        assertEquals("w1", state.selectedSummary?.entryId)

        viewModel.onBackgroundTapped()
        advanceUntilIdle()
        state = assertIs<MapUiState.Loaded>(viewModel.uiState.value)
        assertNull(state.selectedPinId)
        assertNull(state.selectedSummary)
        collector.cancel()
    }

    @Test
    fun selectByEntryId_selectsTheMatchingPin() = runTest(mainDispatcher) {
        val repository = FakeMapRepository()
        repository.setMapConfig(mapConfig())
        repository.setPins(
            listOf(
                pin("p1", EntryRef.ToWeapon(WeaponId("w1"))),
                pin("p2", EntryRef.ToWeapon(WeaponId("w2"))),
            )
        )
        val viewModel = viewModel(mapRepository = repository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectByEntryId("w2")
        advanceUntilIdle()

        val state = assertIs<MapUiState.Loaded>(viewModel.uiState.value)
        assertEquals("p2", state.selectedPinId)
        collector.cancel()
    }

    @Test
    fun selectByEntryId_missingEntryIsANoOp() = runTest(mainDispatcher) {
        val repository = FakeMapRepository()
        repository.setMapConfig(mapConfig())
        repository.setPins(listOf(pin("p1", EntryRef.ToWeapon(WeaponId("w1")))))
        val viewModel = viewModel(mapRepository = repository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.selectByEntryId("does-not-exist")
        advanceUntilIdle()

        val state = assertIs<MapUiState.Loaded>(viewModel.uiState.value)
        assertNull(state.selectedPinId)
        collector.cancel()
    }

    @Test
    fun showLabelsFollowsSettingsPreference() = runTest(mainDispatcher) {
        val repository = FakeMapRepository()
        repository.setMapConfig(mapConfig())
        val settings = FakeSettingsRepository()
        settings.setPreferences(
            Preferences(
                showLabels = false,
                language = "en",
                selectedNewsTopic = null,
                lastContentUpdate = null
            )
        )
        val viewModel = viewModel(mapRepository = repository, settingsRepository = settings)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = assertIs<MapUiState.Loaded>(viewModel.uiState.value)
        assertEquals(false, state.showLabels)
        collector.cancel()
    }

    @Test
    fun repositoryErrorSurfacesAsErrorState() = runTest(mainDispatcher) {
        val repository = FakeMapRepository()
        repository.setMapConfig(mapConfig())
        repository.throwOnPins = true
        val viewModel = viewModel(mapRepository = repository)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertIs<MapUiState.Error>(viewModel.uiState.value)
        collector.cancel()
    }

    @Test
    fun beforeAnyCollection_stateIsLoading() {
        val viewModel = viewModel()
        assertIs<MapUiState.Loading>(viewModel.uiState.value)
    }
}

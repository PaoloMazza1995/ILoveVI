package com.pamaz.vi.feature.map.filters

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.WeaponType
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MapFiltersViewModelTest {

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
    fun everythingStartsOn() = runTest(mainDispatcher) {
        val viewModel = MapFiltersViewModel(MapFilterState())
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.groups.isNotEmpty())
        assertTrue(state.groups.all { group -> group.subTypes.all { it.active } })
        assertFalse(state.isEverythingOff)
        collector.cancel()
    }

    @Test
    fun togglingASubTypeOff() = runTest(mainDispatcher) {
        val filterState = MapFilterState()
        val viewModel = MapFiltersViewModel(filterState)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSubTypeToggled(WeaponType.PISTOL.name)
        advanceUntilIdle()

        val weaponGroup =
            viewModel.uiState.value.groups.single { it.category == EntryCategory.WEAPON }
        val pistolChip = weaponGroup.subTypes.single { it.name == WeaponType.PISTOL.name }
        assertFalse(pistolChip.active)
        collector.cancel()
    }

    @Test
    fun turningEverySubTypeOffReportsEverythingOff() = runTest(mainDispatcher) {
        val filterState = MapFilterState()
        val viewModel = MapFiltersViewModel(filterState)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.uiState.value.groups.forEach { group ->
            group.subTypes.forEach { chip -> viewModel.onSubTypeToggled(chip.name) }
        }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEverythingOff)
        collector.cancel()
    }

    @Test
    fun resetTurnsEverythingBackOn() = runTest(mainDispatcher) {
        val filterState = MapFilterState()
        val viewModel = MapFiltersViewModel(filterState)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSubTypeToggled(WeaponType.PISTOL.name)
        filterState.toggleCategory(EntryCategory.VEHICLE)
        advanceUntilIdle()

        viewModel.onReset()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.groups.all { group -> group.subTypes.all { it.active } })
        assertFalse(state.isEverythingOff)
        collector.cancel()
    }

    @Test
    fun sharedStateIsVisibleAcrossBothViewModelsInstances() = runTest(mainDispatcher) {
        val filterState = MapFilterState()
        val filtersViewModel = MapFiltersViewModel(filterState)
        val collector = launch { filtersViewModel.uiState.collect {} }
        advanceUntilIdle()

        filterState.toggleSubType(WeaponType.RIFLE.name)
        advanceUntilIdle()

        val weaponGroup =
            filtersViewModel.uiState.value.groups.single { it.category == EntryCategory.WEAPON }
        assertFalse(weaponGroup.subTypes.single { it.name == WeaponType.RIFLE.name }.active)
        collector.cancel()
    }
}

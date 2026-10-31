package com.pamaz.vi.feature.catalog.detail

import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.feature.catalog.fake.FakeCatalogRepository
import com.pamaz.vi.feature.catalog.fake.FakeMapRepository
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
import kotlin.time.Instant

private val NOW = Instant.parse("2026-01-01T00:00:00Z")

private fun weapon(id: String = "weapon-1") = Weapon(
    id = WeaponId(id),
    name = "Pistol",
    description = "d",
    subType = WeaponType.PISTOL,
    updatedAt = NOW,
)

/** See `CatalogViewModelTest`'s KDoc for why `viewModelScope`-backed state needs the Main dispatcher installed in tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

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
    fun entryWithoutAPin_hasANullPinInLoadedState() = runTest(mainDispatcher) {
        val catalogRepository = FakeCatalogRepository().apply { setWeapons(listOf(weapon())) }
        val mapRepository = FakeMapRepository() // no pins set at all

        val viewModel = DetailViewModel(catalogRepository, mapRepository, "WEAPON", "weapon-1")
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = assertIs<DetailUiState.Loaded>(viewModel.uiState.value)
        assertNull(state.pin)

        collector.cancel()
    }

    @Test
    fun entryWithAPin_hasItInLoadedState() = runTest(mainDispatcher) {
        val catalogRepository = FakeCatalogRepository().apply { setWeapons(listOf(weapon())) }
        val pin = MapPin(
            id = MapPinId("pin-1"),
            entryRef = EntryRef.ToWeapon(WeaponId("weapon-1")),
            position = NormalizedPoint(0.5f, 0.5f),
            mapVersion = 1,
            updatedAt = NOW,
        )
        val mapRepository = FakeMapRepository().apply { setPins(listOf(pin)) }

        val viewModel = DetailViewModel(catalogRepository, mapRepository, "WEAPON", "weapon-1")
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val state = assertIs<DetailUiState.Loaded>(viewModel.uiState.value)
        assertEquals(pin, state.pin)

        collector.cancel()
    }

    @Test
    fun unknownEntry_isNotFound() = runTest(mainDispatcher) {
        val viewModel =
            DetailViewModel(FakeCatalogRepository(), FakeMapRepository(), "WEAPON", "missing")
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertIs<DetailUiState.NotFound>(viewModel.uiState.value)

        collector.cancel()
    }

    @Test
    fun malformedCategory_isNotFound() = runTest(mainDispatcher) {
        val viewModel = DetailViewModel(
            FakeCatalogRepository(),
            FakeMapRepository(),
            "NOT_A_CATEGORY",
            "weapon-1"
        )
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertIs<DetailUiState.NotFound>(viewModel.uiState.value)

        collector.cancel()
    }
}

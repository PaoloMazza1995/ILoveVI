package com.pamaz.vi.feature.map.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pamaz.vi.core.data.repository.CatalogRepository
import com.pamaz.vi.core.data.repository.MapPinWithEntryName
import com.pamaz.vi.core.model.Entry
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.design.IloveViTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.time.Instant

private val NOW = Instant.parse("2026-01-01T00:00:00Z")

private val testWeapon = Weapon(
    id = WeaponId("weapon-1"),
    name = "Test Pistol",
    description = "A test weapon description.",
    subType = WeaponType.PISTOL,
    updatedAt = NOW,
)

/** Only [entry] is exercised by this test — the bottom sheet's "all the infos" description fetch. */
private class FakeCatalogRepository : CatalogRepository {
    override fun weapons(): Flow<List<Weapon>> = error("not used")
    override fun vehicles(): Flow<List<Vehicle>> = error("not used")
    override fun items(): Flow<List<Item>> = error("not used")
    override fun weapon(id: WeaponId): Flow<Weapon?> = error("not used")
    override fun vehicle(id: VehicleId): Flow<Vehicle?> = error("not used")
    override fun item(id: ItemId): Flow<Item?> = error("not used")
    override fun searchWeapons(query: String): Flow<List<Weapon>> = error("not used")
    override fun searchVehicles(query: String): Flow<List<Vehicle>> = error("not used")
    override fun searchItems(query: String): Flow<List<Item>> = error("not used")
    override fun entry(category: EntryCategory, entryId: String): Flow<Entry?> = flowOf(testWeapon)
}

private val config = MapConfig(
    mapVersion = 1,
    sourceImageUrl = "https://example.com/map.png",
    widthPx = 1000,
    heightPx = 1000,
    tileUrlTemplate = "https://example.com/{z}/{x}/{y}.png",
    minZoom = 0,
    maxZoom = 5,
    updatedAt = NOW,
)

private val pinEntry = MapPinWithEntryName(
    pin = MapPin(
        id = MapPinId("pin-1"),
        entryRef = EntryRef.ToWeapon(WeaponId("weapon-1")),
        position = NormalizedPoint(0.5f, 0.5f),
        zone = "Test Zone",
        mapVersion = 1,
        updatedAt = NOW,
    ),
    entryName = "Test Pistol",
)

/**
 * The acceptance's explicit UI test: "pin tap -> summary card -> View details." Drives the real
 * `MapScreenContent` (the stateless half of `MapScreen`) — this test itself plays the role of the
 * app's navigation/state-holding layer, exactly the way `CatalogToDetailUiTest` (this repo's
 * precedent for Compose UI testing a tap-driven flow) stands in for `AppFeatureRegistrations`.
 *
 * The pin's semantics content description is plain Kotlin text (`PinLayer.kt`'s
 * `accessibleName()`), not a `stringResource` — so, unlike `CatalogToDetailUiTest`'s row/button
 * (which needed a `testTag` because resource-backed text wasn't reliably resolving in this JVM
 * test environment), it can be found by content description directly. The "View details" button
 * still gets a `testTag` for the same reason `DetailScreen.kt`'s "Show on map" button did.
 *
 * [setUpMainDispatcher]/[tearDownMainDispatcher]: kept defensively even though the tile-prefetch
 * path that originally required it (`TilePrefetch.kt`'s Coil3 `ImageLoader`, which lazily read
 * `Dispatchers.Main.immediate`) is gone — `:core:mapview`'s own pinned-levels preload replaced it
 * (see that module's README) and uses a plain `rememberCoroutineScope()`, which this Compose UI
 * test's own rule already services without a `Main` dispatcher. Left in place since removing it
 * isn't this step's concern and a redundant test-dispatcher install is harmless.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapPinToDetailUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUpMainDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun pinTap_showsSummaryCard_andViewDetailsFiresWithTheEntryRef() {
        var openedDetailFor: Pair<String, String>? = null

        composeRule.setContent {
            var selectedPinId by remember { mutableStateOf<String?>(null) }

            val state = MapUiState.Loaded(
                mapConfig = config,
                pins = listOf(pinEntry),
                activeCategories = EntryCategory.entries.toSet(),
                selectedPinId = selectedPinId,
                selectedSummary = selectedPinId?.let {
                    SelectedPinSummary(
                        pinId = "pin-1",
                        entryId = "weapon-1",
                        category = EntryCategory.WEAPON,
                        name = pinEntry.entryName,
                        zone = pinEntry.pin.zone,
                    )
                },
                showLabels = false,
            )

            // The bottom sheet's expanded ("all the infos") section fetches the full Entry via
            // Koin's koinInject<CatalogRepository>() — a real Koin application must be running for
            // that to resolve. Scoped with KoinApplication (not startKoin/stopKoin) so it's local
            // to this one composition and never leaks a global Koin instance across test runs.
            KoinApplication(application = { modules(module { single<CatalogRepository> { FakeCatalogRepository() } }) }) {
                IloveViTheme {
                    MapScreenContent(
                        state = state,
                        selectedEntryId = null,
                        searchQuery = "",
                        searchResults = emptyList(),
                        onSearchQueryChanged = {},
                        onCategoryToggled = {},
                        onPinSelected = { id -> selectedPinId = id },
                        onBackgroundTapped = { selectedPinId = null },
                        onSelectByEntryId = {},
                        onOpenFilters = {},
                        onOpenDetail = { category, entryId ->
                            openedDetailFor = category to entryId
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()

        // Taps a real pin through the real `onClick` semantics action [PinMarker] (`:core:mapview`)
        // attaches to it — the same action a screen reader would activate. A raw `performClick()`
        // (synthetic mouse injection on this Desktop test harness) would instead have to survive
        // `MapViewer`'s own custom low-level pointer-input gesture detector, which `:core:mapview`'s
        // own README already flags as unverified in this sandboxed, device-less environment; going
        // through the semantics action tests the same production callback without that dependency.
        val pinNode = composeRule.onNodeWithContentDescription("Weapon").fetchSemanticsNode()
        pinNode.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("map-view-details-button").performClick()
        composeRule.waitForIdle()

        assertEquals("WEAPON" to "weapon-1", openedDetailFor)
    }

    @Test
    fun pinTap_opensBottomSheetWithTheEntrysDescription() {
        composeRule.setContent {
            var selectedPinId by remember { mutableStateOf<String?>(null) }

            val state = MapUiState.Loaded(
                mapConfig = config,
                pins = listOf(pinEntry),
                activeCategories = EntryCategory.entries.toSet(),
                selectedPinId = selectedPinId,
                selectedSummary = selectedPinId?.let {
                    SelectedPinSummary(
                        pinId = "pin-1",
                        entryId = "weapon-1",
                        category = EntryCategory.WEAPON,
                        name = pinEntry.entryName,
                        zone = pinEntry.pin.zone,
                    )
                },
                showLabels = false,
            )

            KoinApplication(application = { modules(module { single<CatalogRepository> { FakeCatalogRepository() } }) }) {
                IloveViTheme {
                    MapScreenContent(
                        state = state,
                        selectedEntryId = null,
                        searchQuery = "",
                        searchResults = emptyList(),
                        onSearchQueryChanged = {},
                        onCategoryToggled = {},
                        onPinSelected = { id -> selectedPinId = id },
                        onBackgroundTapped = { selectedPinId = null },
                        onSelectByEntryId = {},
                        onOpenFilters = {},
                        onOpenDetail = { _, _ -> },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val pinNode = composeRule.onNodeWithContentDescription("Weapon").fetchSemanticsNode()
        pinNode.config.getOrNull(SemanticsActions.OnClick)?.action?.invoke()
        composeRule.waitForIdle()

        // The sheet is only peeked (SheetValue.PartiallyExpanded) at this point, not dragged open
        // — but BottomSheetScaffold composes the whole sheetContent column regardless of how much
        // of it is currently visible (only the *visual* offset changes as the user drags), so the
        // description further down that column already exists in the semantics tree without
        // needing to simulate an actual drag gesture here.
        composeRule.onNodeWithText(testWeapon.description).assertExists()
    }
}

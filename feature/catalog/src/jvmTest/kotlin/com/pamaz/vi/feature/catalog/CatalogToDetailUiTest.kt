package com.pamaz.vi.feature.catalog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.feature.catalog.catalog.CatalogScreenContent
import com.pamaz.vi.feature.catalog.catalog.CatalogUiState
import com.pamaz.vi.feature.catalog.detail.DetailScreenContent
import com.pamaz.vi.feature.catalog.detail.DetailUiState
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

private val NOW = Instant.parse("2026-01-01T00:00:00Z")

private val weapon = Weapon(
    id = WeaponId("weapon-1"),
    name = "Test Pistol",
    description = "A test weapon.",
    subType = WeaponType.PISTOL,
    zone = "Test Zone",
    updatedAt = NOW,
)

private val pin = MapPin(
    id = MapPinId("pin-1"),
    entryRef = EntryRef.ToWeapon(weapon.id),
    position = NormalizedPoint(0.5f, 0.5f),
    mapVersion = 1,
    updatedAt = NOW,
)

/**
 * The acceptance's explicit UI test: "row tap -> Detail -> 'Show on map' event." No other UI test
 * harness exists for this module (`:tools:map-editor`'s `MapEditorAppUiTest` is this repo's only
 * precedent for Compose UI testing, same `compose.desktop.uiTestJUnit4` dependency). This test
 * drives the real `CatalogScreenContent`/`DetailScreenContent` — the stateless halves of
 * `CatalogScreen`/`DetailScreen` — switching between them itself (standing in for Navigation
 * Compose, which neither screen depends on directly) exactly the way the app module's real
 * `FeatureRegistration`s do: `onOpenDetail` swaps to Detail, `onShowOnMap` is the event this test
 * asserts fires with the tapped row's own entry id.
 *
 * Interactive nodes are found by `testTag`, not by their (Compose-resources-backed, and in this
 * JVM test environment unreliably timed) visible text — `stringResource` was observed to still be
 * resolving to an empty string after `waitForIdle()`, for both this module's own and
 * `:core:design`'s string resources, in a plain `createComposeRule()` JVM test with no Android
 * instrumentation. The two production call sites (`CatalogScreen.kt`'s row, `DetailScreen.kt`'s
 * "Show on map" button) each got one small, harmless `Modifier.testTag(...)` addition for this.
 */
class CatalogToDetailUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rowTap_opensDetail_andShowOnMapFiresWithTheSameEntryId() {
        var shownDetailFor: Pair<String, String>? = null
        var showOnMapEntryId: String? = null

        composeRule.setContent {
            var detailArgs by remember { mutableStateOf<Pair<String, String>?>(null) }

            IloveViTheme {
                if (detailArgs == null) {
                    CatalogScreenContent(
                        state = CatalogUiState(
                            category = EntryCategory.WEAPON,
                            entries = listOf(weapon),
                            isLoading = false
                        ),
                        onCategorySelected = {},
                        onQueryChanged = {},
                        onOpenDetail = { category, entryId ->
                            shownDetailFor = category to entryId
                            detailArgs = category to entryId
                        },
                    )
                } else {
                    DetailScreenContent(
                        state = DetailUiState.Loaded(entry = weapon, pin = pin, mapConfig = null),
                        onBack = {},
                        onShowOnMap = { entryId -> showOnMapEntryId = entryId },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("catalog-row-weapon-1").performClick()
        composeRule.waitForIdle()

        assertEquals("WEAPON" to "weapon-1", shownDetailFor)

        composeRule.onNodeWithTag("show-on-map-button").performClick()
        composeRule.waitForIdle()

        assertEquals("weapon-1", showOnMapEntryId)
    }
}

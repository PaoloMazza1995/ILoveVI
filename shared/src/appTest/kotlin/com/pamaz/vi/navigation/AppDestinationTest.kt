package com.pamaz.vi.navigation

import com.pamaz.vi.design.components.IloveViNavDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Structural coverage for step 8's navigation contract — the parts testable without a real
 * Compose UI test harness/device (this project has none set up yet; see `shared/README.md`).
 * Real interactive behavior (actually tapping tabs, pressing back, watching the back stack) is
 * NOT exercised here — that needs a real device/emulator, unavailable in this environment.
 */
class AppDestinationTest {

    @Test
    fun exactlyFiveTabs_inTheRequiredOrder() {
        assertEquals(
            listOf(
                IloveViNavDestination.Map,
                IloveViNavDestination.Catalog,
                IloveViNavDestination.Music,
                IloveViNavDestination.News,
                IloveViNavDestination.Settings,
            ),
            AppDestination.Tab.entries.map { it.navDestination },
        )
    }

    @Test
    fun everyTab_hasAUniqueRoute() {
        val routes = AppDestination.Tab.entries.map { it.route }
        assertEquals(
            routes.toSet().size,
            routes.size,
            "expected no duplicate tab routes, got $routes"
        )
    }

    @Test
    fun forNavDestination_roundTripsEveryTab() {
        for (tab in AppDestination.Tab.entries) {
            assertEquals(tab, AppDestination.Tab.forNavDestination(tab.navDestination))
        }
    }

    @Test
    fun compilation_isNotATab() {
        val tabRoutes = AppDestination.Tab.entries.map { it.route }
        assertFalse(
            AppDestination.Secondary.Compilation.route in tabRoutes,
            "Compilation must only be reachable from the Music tab's card, never the bottom bar",
        )
    }

    @Test
    fun detailRoute_formatsCategoryAndEntryId() {
        assertEquals(
            "detail/WEAPON/abc-123",
            AppDestination.Secondary.Detail.route("WEAPON", "abc-123")
        )
    }

    @Test
    fun secondaryDestinations_areDisjointFromTabRoutes() {
        val tabRoutes = AppDestination.Tab.entries.map { it.route }.toSet()
        val secondaryRoutes = setOf(
            AppDestination.Secondary.MapFilters.route,
            AppDestination.Secondary.Detail.route,
            AppDestination.Secondary.Compilation.route,
        )
        assertTrue((tabRoutes intersect secondaryRoutes).isEmpty())
    }
}

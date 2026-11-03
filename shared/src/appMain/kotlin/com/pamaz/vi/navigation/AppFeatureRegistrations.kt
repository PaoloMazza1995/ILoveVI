package com.pamaz.vi.navigation

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.pamaz.vi.feature.catalog.catalog.CatalogScreen
import com.pamaz.vi.feature.catalog.detail.DetailScreen
import com.pamaz.vi.feature.map.filters.MapFiltersScreen
import com.pamaz.vi.feature.map.map.MapScreen
import com.pamaz.vi.feature.news.news.NewsScreen
import com.pamaz.vi.screens.CompilationScreen
import com.pamaz.vi.screens.MusicStubScreen
import com.pamaz.vi.screens.SettingsStubScreen

/**
 * This step's own (only, for now) registrant — see [FeatureRegistration]'s KDoc. Built exactly
 * the shape a real `feature:catalog`/`feature:music`/`feature:map`/... module will build later:
 * a flat `List<FeatureRegistration>`, each one a route plus the composable for it, with no
 * knowledge in [AppNavHost] of how any of these screens are implemented.
 */
internal fun appFeatureRegistrations(
    navController: NavHostController,
    selectedMapEntryId: String?,
    onSelectMapEntry: (String) -> Unit,
): List<FeatureRegistration> = listOf(
    FeatureRegistration(AppDestination.Tab.Map) {
        MapScreen(
            selectedEntryId = selectedMapEntryId,
            onOpenFilters = { navController.navigate(AppDestination.Secondary.MapFilters.route) },
            onOpenDetail = { category, entryId ->
                navController.navigate(AppDestination.Secondary.Detail.route(category, entryId))
            },
        )
    },
    FeatureRegistration(AppDestination.Tab.Catalog) {
        CatalogScreen(
            onOpenDetail = { category, entryId ->
                navController.navigate(AppDestination.Secondary.Detail.route(category, entryId))
            },
        )
    },
    FeatureRegistration(AppDestination.Tab.Music) {
        MusicStubScreen(
            onOpenCompilation = { navController.navigate(AppDestination.Secondary.Compilation.route) },
        )
    },
    FeatureRegistration(AppDestination.Tab.News) { NewsScreen() },
    FeatureRegistration(AppDestination.Tab.Settings) { SettingsStubScreen() },

    FeatureRegistration(AppDestination.Secondary.MapFilters) {
        MapFiltersScreen(onBack = { navController.popBackStack() })
    },
    FeatureRegistration(
        destination = AppDestination.Secondary.Detail,
        arguments = detailArguments,
    ) { entry ->
        DetailScreen(
            category = entry.arguments
                ?.read { getStringOrNull(AppDestination.Secondary.Detail.ARG_CATEGORY) }
                .orEmpty(),
            entryId = entry.arguments
                ?.read { getStringOrNull(AppDestination.Secondary.Detail.ARG_ENTRY_ID) }
                .orEmpty(),
            onBack = { navController.popBackStack() },
            // onSelectMapEntry switches to the Map tab via the same popUpTo(startDestination)
            // navigation the bottom bar uses, which already pops Detail (and anything else on
            // top of the tab roots) off the back stack — no separate popBackStack() needed here.
            onShowOnMap = onSelectMapEntry,
        )
    },
    FeatureRegistration(AppDestination.Secondary.Compilation) {
        CompilationScreen(onBack = { navController.popBackStack() })
    },
)

private val detailArguments: List<NamedNavArgument> = listOf(
    navArgument(AppDestination.Secondary.Detail.ARG_CATEGORY) { type = NavType.StringType },
    navArgument(AppDestination.Secondary.Detail.ARG_ENTRY_ID) { type = NavType.StringType },
)

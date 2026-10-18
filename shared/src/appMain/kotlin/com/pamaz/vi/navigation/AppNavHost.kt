package com.pamaz.vi.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pamaz.vi.design.components.IloveViNavBar

/**
 * The app shell (step 8): a fixed [IloveViNavBar] bottom bar plus one flat
 * [androidx.navigation.compose.NavHost] holding both the five tab roots and the three secondary
 * destinations (so a secondary destination pushes on top of whichever tab opened it, with the
 * bar still visible underneath — matching "opened on top of the tabs").
 *
 * **Tab switching / per-tab back stack**: selecting a bottom-bar item navigates with
 * `popUpTo(graph.findStartDestination) { saveState = true }` + `restoreState = true` — Navigation
 * Compose's own documented recipe for "each tab keeps its own back stack; switching tabs restores
 * it", rather than nesting a separate `NavHost` per tab (unnecessary extra ceremony for 5 flat
 * stub destinations today; revisit if a tab's real back stack ever needs to be independently
 * deep-linkable).
 *
 * **System back on Map exits the app**: not re-implemented here. [AppDestination.Tab.Map] is this
 * graph's `startDestination`; Navigation Compose's own `NavHost` only intercepts system back while
 * there's something to pop, and lets it fall through to the platform default (closing the
 * Activity, on Android) once you're on the start destination with an empty stack — exactly the
 * required behavior, for free, as long as nothing else here adds a competing back handler (nothing
 * does).
 *
 * **"Show on map"**: [registrations] already carries a `Detail` registration whose `onShowOnMap`
 * sets [selectedMapEntryId] and switches to the Map tab via the same popUpTo/restoreState
 * navigation used by the bottom bar — see `AppFeatureRegistrations.kt`. This is *not* a user
 * position (forbidden by the product rules): it only ever holds an entry id someone navigated
 * from, and the Map tab is free to ignore it entirely once real pin data exists.
 */
@Composable
fun AppNavHost(
    buildRegistrations: (
        navController: NavHostController,
        selectedMapEntryId: String?,
        onSelectMapEntry: (String) -> Unit,
    ) -> List<FeatureRegistration>,
) {
    val navController = rememberNavController()
    var selectedTab by rememberSaveable { mutableStateOf(AppDestination.Tab.Map) }
    var selectedMapEntryId by rememberSaveable { mutableStateOf<String?>(null) }

    fun switchToTab(tab: AppDestination.Tab) {
        selectedTab = tab
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val registrations = buildRegistrations(navController, selectedMapEntryId) { entryId ->
        selectedMapEntryId = entryId
        switchToTab(AppDestination.Tab.Map)
    }

    Scaffold(
        bottomBar = {
            IloveViNavBar(
                selected = selectedTab.navDestination,
                onSelect = { destination ->
                    switchToTab(
                        AppDestination.Tab.forNavDestination(
                            destination
                        )
                    )
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Tab.Map.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            registrations.forEach { registration ->
                composable(registration.destination.route, registration.arguments) { entry ->
                    registration.content(entry)
                }
            }
        }
    }
}

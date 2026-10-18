package com.pamaz.vi.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry

/**
 * What a feature module needs to plug one [AppDestination] into [com.pamaz.vi.AppNavHost] without
 * the app module ever importing that feature's internal types — the "small contract" step 8 asks
 * for. A future `feature:catalog` (etc.) exposes a `List<FeatureRegistration>` (one per screen it
 * owns); the app module only ever sees this shape, never the feature's view models, repositories
 * or screen composables directly.
 *
 * Today, with no feature modules yet, the app module is its own (only) registrant — see
 * `AppFeatureRegistrations.kt` — built the exact same way a real feature module will be later, so
 * swapping a stub registration for a real one is a one-line change, not a redesign.
 */
data class FeatureRegistration(
    val destination: AppDestination,
    val arguments: List<NamedNavArgument> = emptyList(),
    val content: @Composable (NavBackStackEntry) -> Unit,
)

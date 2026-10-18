package com.pamaz.vi

import androidx.compose.runtime.Composable
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.di.allAppKoinModules
import com.pamaz.vi.navigation.AppNavHost
import com.pamaz.vi.navigation.appFeatureRegistrations
import org.koin.core.context.startKoin
import org.koin.core.error.KoinApplicationAlreadyStartedException

/**
 * The real app shell (step 8): [IloveViTheme] wrapping the five-tab [AppNavHost]. Lives here,
 * not in `commonMain`, because its dependencies (`:core:design`, `:core:mapview`, Koin,
 * Navigation Compose) only target JVM/Android/iOS — see `shared/README.md`.
 *
 * Koin is started here — guarded by catching [KoinApplicationAlreadyStartedException] rather than in
 * each platform's own entry point, so `androidApp`'s `MainActivity`/`shared`'s own iOS
 * `MainViewController` stay exactly as they were (both already just call `App()`) — "start it at
 * launch" happens on this composable's first call, which for every platform here *is* launch.
 */
@Composable
actual fun App() {
    try {
        startKoin { modules(allAppKoinModules) }
    } catch (_: KoinApplicationAlreadyStartedException) {
        // Already started by a previous composition of this same App() (e.g. after a
        // configuration change) — nothing else to do.
    }
    IloveViTheme {
        AppNavHost { navController, selectedMapEntryId, onSelectMapEntry ->
            appFeatureRegistrations(navController, selectedMapEntryId, onSelectMapEntry)
        }
    }
}

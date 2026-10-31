package com.pamaz.vi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.pamaz.vi.core.data.seed.SeedImporter
import com.pamaz.vi.core.data.sync.ContentSyncer
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.di.allAppKoinModules
import com.pamaz.vi.di.rememberPlatformContextModule
import com.pamaz.vi.navigation.AppNavHost
import com.pamaz.vi.navigation.appFeatureRegistrations
import org.koin.compose.koinInject
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
 * [rememberPlatformContextModule] (step 9) has to be read here, inside composition, for the same
 * reason — see its own KDoc.
 *
 * **Step 9**: [SeedImporter.importIfNeeded]/[ContentSyncer.start] are called here too, in one
 * [LaunchedEffect] keyed `Unit` (so a recomposition — e.g. after a configuration change — never
 * re-triggers it) — `:core:data`'s own README had explicitly deferred this to "whichever step
 * assembles the real app shell against this data layer," since without it the local database
 * would stay empty on a fresh install and Catalog/Detail would have nothing to read, network or
 * not. Seed import runs first (`await`ed, since it's the one-time "app is never empty offline"
 * guarantee this app's very first screen depends on) and only then does the real sync start, so a
 * slow/failed network sync can never delay the bundled seed rows from being there immediately.
 */
@Composable
actual fun App() {
    val platformContextModule = rememberPlatformContextModule()
    try {
        startKoin { modules(allAppKoinModules(platformContextModule)) }
    } catch (_: KoinApplicationAlreadyStartedException) {
        // Already started by a previous composition of this same App() (e.g. after a
        // configuration change) — nothing else to do.
    }

    val seedImporter = koinInject<SeedImporter>()
    val contentSyncer = koinInject<ContentSyncer>()
    val appScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        seedImporter.importIfNeeded()
        contentSyncer.start(appScope)
    }

    IloveViTheme {
        AppNavHost { navController, selectedMapEntryId, onSelectMapEntry ->
            appFeatureRegistrations(navController, selectedMapEntryId, onSelectMapEntry)
        }
    }
}

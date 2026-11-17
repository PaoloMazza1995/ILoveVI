package com.pamaz.vi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pamaz.vi.core.data.repository.SettingsRepository
import com.pamaz.vi.core.data.seed.SeedImporter
import com.pamaz.vi.core.data.sync.ContentSyncer
import com.pamaz.vi.design.ApplyLanguagePreference
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.design.IloveViThemeMode
import com.pamaz.vi.di.allAppKoinModules
import com.pamaz.vi.di.rememberPlatformContextModule
import com.pamaz.vi.navigation.AppNavHost
import com.pamaz.vi.navigation.appFeatureRegistrations
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
 *
 * **Step 13B**: [IloveViTheme]'s `themeMode` and [ApplyLanguagePreference]'s `languageCode` both
 * come from [SettingsRepository] instead of defaulting silently — [initialPreferences] is read
 * with a blocking [runBlocking] call on first composition (fast: one row, local SQLite, same
 * database [SeedImporter]/[ContentSyncer] already open) so the very first frame already renders in
 * the right theme/language, never a flash of the wrong one followed by a jump; every composition
 * after that reads the same [SettingsRepository.preferences] [Flow] reactively via
 * [collectAsStateWithLifecycle]. Theme changes apply immediately on every platform. Language
 * changes apply immediately on Android (a quick [android.app.Activity.recreate]) but need the next
 * cold launch on iOS — a real Compose Multiplatform/iOS platform limitation, not a bug here; see
 * [ApplyLanguagePreference]'s own KDoc.
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

    val settingsRepository = koinInject<SettingsRepository>()
    val initialPreferences = remember { runBlocking { settingsRepository.preferences().first() } }
    val preferences by settingsRepository.preferences()
        .collectAsStateWithLifecycle(initialValue = initialPreferences)
    val themeMode = preferences?.themeMode?.toIloveViThemeModeOrNull() ?: IloveViThemeMode.System
    val languageCode = preferences?.language?.takeIf { it.isNotBlank() } ?: "en"

    ApplyLanguagePreference(languageCode)
    IloveViTheme(themeMode = themeMode) {
        AppNavHost { navController, selectedMapEntryId, onSelectMapEntry ->
            appFeatureRegistrations(navController, selectedMapEntryId, onSelectMapEntry)
        }
    }
}

/**
 * Case-insensitive on purpose: the stored value comes from `:feature:settings`'s own
 * `ThemeModeOption` enum (`SYSTEM`/`LIGHT`/`DARK`, all caps), not this one
 * (`System`/`Light`/`Dark`, mixed case) — a real bug once compared case-sensitively (the
 * stored "DARK" never matched "Dark", so the app silently always fell back to [IloveViThemeMode.System]
 * regardless of what was actually selected). Comparing case-insensitively here makes the match
 * correct regardless of either enum's declared casing, present or future.
 */
private fun String.toIloveViThemeModeOrNull(): IloveViThemeMode? =
    IloveViThemeMode.entries.firstOrNull { it.name.equals(this, ignoreCase = true) }

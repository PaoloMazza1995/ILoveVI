package com.pamaz.vi.di

import androidx.compose.runtime.Composable
import org.koin.core.module.Module

/**
 * A Koin [Module] providing a real `:core:database` `PlatformContext` single — the opaque
 * per-platform handle `DatabaseDriverFactory` (and `:core:data`'s `ConnectivityMonitor`) need a
 * real Android `Context` for, everywhere else just an unused marker. `@Composable` because the
 * Android `actual` needs `androidx.compose.ui.platform.LocalContext`, only readable inside
 * composition — there is no other place in this app (no custom `Application` subclass, no
 * `MainActivity` changes; `App()`'s own signature stays untouched per step 8) to get a real
 * `Context` from.
 *
 * Declared in *true* `commonMain` (not `src/appMain`, see `shared/README.md`) because `expect`/
 * `actual` must live in genuinely different compilations, not the same aliased file compiled
 * twice — the same reason `Platform.kt`'s pre-existing `expect fun getPlatform()` lives here too.
 */
@Composable
expect fun rememberPlatformContextModule(): Module

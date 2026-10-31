package com.pamaz.vi.di

import com.pamaz.vi.core.data.di.dataModule
import com.pamaz.vi.core.database.di.databaseModule
import com.pamaz.vi.core.network.di.networkModule
import com.pamaz.vi.feature.catalog.di.catalogFeatureModule
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * `:core:design`/`:core:mapview` have no DI needs (pure Compose components, no repositories or
 * view models) — step 8's original "one empty module per Gradle module" placeholders for them
 * were removed rather than kept as permanent no-ops.
 *
 * `databaseModule`/`networkModule`/`dataModule` (real, from their own modules) are wired in here
 * for the first time — each one's own README already said "not wired into shared's
 * AppKoinModules.kt... until a consuming step needs it"; step 9's `:feature:catalog` is that
 * step. `catalogFeatureModule` provides `CatalogViewModel`/`DetailViewModel`.
 */
internal val appModule: Module = module { }

/**
 * Every module's Koin module, aggregated in the one place (`App()`, in `com.pamaz.vi`) that
 * starts Koin. [platformContextModule] (the real `PlatformContext` single `databaseModule`
 * itself can't construct, see that module's README) is passed in rather than listed here, since
 * building it needs to be `@Composable` (see `di/PlatformContextModule.kt`'s KDoc).
 */
internal fun allAppKoinModules(platformContextModule: Module): List<Module> = listOf(
    platformContextModule,
    databaseModule,
    networkModule,
    dataModule,
    catalogFeatureModule,
    appModule,
)

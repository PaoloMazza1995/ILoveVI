package com.pamaz.vi.feature.catalog.di

import com.pamaz.vi.feature.catalog.catalog.CatalogViewModel
import com.pamaz.vi.feature.catalog.detail.DetailViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Plain Koin, same style as `:core:database`'s `databaseModule`/`:core:network`'s
 * `networkModule`/`:core:data`'s `dataModule`. Expects `dataModule` to already be included
 * wherever this one is (every binding here resolves one of its repositories).
 *
 * `viewModel { }`'s import triggers a deprecation warning in Koin 4.1.1 ("moved to
 * `org.koin.core.module.dsl`"), but that package — checked directly in the real `koin-core-jvm`
 * 4.1.1 jar — has no `viewModel` function at all (only `singleOf`/`factoryOf`/`scopedOf`); the
 * real replacement lives in a separate `koin-viewmodel` artifact this catalog doesn't declare.
 * Kept on the working, Compose-Multiplatform-specific DSL rather than adding a new dependency to
 * silence a warning for a function that isn't actually where the message says it is.
 */
val catalogFeatureModule = module {
    viewModel { CatalogViewModel(get()) }
    viewModel { (category: String, entryId: String) ->
        DetailViewModel(
            get(),
            get(),
            category,
            entryId
        )
    }
}

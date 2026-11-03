package com.pamaz.vi.feature.news.di

import com.pamaz.vi.feature.news.news.NewsViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/** Plain Koin, same style as `:feature:catalog`'s `catalogFeatureModule`. Expects `dataModule` to already be included wherever this one is. */
val newsFeatureModule = module {
    viewModel { NewsViewModel(get(), get()) }
}

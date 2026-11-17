package com.pamaz.vi.feature.settings.di

import com.pamaz.vi.core.data.appVersionName
import com.pamaz.vi.feature.settings.settings.SettingsViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/** Plain Koin, same style as `:feature:news`'s `newsFeatureModule`. Expects `dataModule` (and the `PlatformContext` single) to already be included wherever this one is. */
val settingsFeatureModule = module {
    viewModel { SettingsViewModel(get(), get(), get(), get(), appVersionName(get())) }
}

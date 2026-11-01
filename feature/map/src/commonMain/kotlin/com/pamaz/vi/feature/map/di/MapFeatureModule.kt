package com.pamaz.vi.feature.map.di

import com.pamaz.vi.feature.map.filters.MapFilterState
import com.pamaz.vi.feature.map.filters.MapFiltersViewModel
import com.pamaz.vi.feature.map.map.MapViewModel
import org.koin.compose.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Plain Koin, same style as `:feature:catalog`'s `catalogFeatureModule`. [MapFilterState] is a
 * `single`, not a `viewModel` — see its own KDoc for why the Map tab and Filters screen need to
 * share the exact same instance rather than each getting their own.
 */
val mapFeatureModule = module {
    single { MapFilterState() }
    viewModel { MapViewModel(get(), get(), get()) }
    viewModel { MapFiltersViewModel(get()) }
}

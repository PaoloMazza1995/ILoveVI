package com.pamaz.vi.core.network.di

import com.pamaz.vi.core.network.SupabaseClientProvider
import com.pamaz.vi.core.network.config.SupabaseConfig
import com.pamaz.vi.core.network.datasource.FeedSourceRemoteDataSource
import com.pamaz.vi.core.network.datasource.FeedSourceRemoteDataSourceImpl
import com.pamaz.vi.core.network.datasource.ItemRemoteDataSource
import com.pamaz.vi.core.network.datasource.ItemRemoteDataSourceImpl
import com.pamaz.vi.core.network.datasource.MapConfigRemoteDataSource
import com.pamaz.vi.core.network.datasource.MapConfigRemoteDataSourceImpl
import com.pamaz.vi.core.network.datasource.MapPinRemoteDataSource
import com.pamaz.vi.core.network.datasource.MapPinRemoteDataSourceImpl
import com.pamaz.vi.core.network.datasource.NewsFeedFetcher
import com.pamaz.vi.core.network.datasource.NewsFeedFetcherImpl
import com.pamaz.vi.core.network.datasource.SongRemoteDataSource
import com.pamaz.vi.core.network.datasource.SongRemoteDataSourceImpl
import com.pamaz.vi.core.network.datasource.StationRemoteDataSource
import com.pamaz.vi.core.network.datasource.StationRemoteDataSourceImpl
import com.pamaz.vi.core.network.datasource.VehicleRemoteDataSource
import com.pamaz.vi.core.network.datasource.VehicleRemoteDataSourceImpl
import com.pamaz.vi.core.network.datasource.WeaponRemoteDataSource
import com.pamaz.vi.core.network.datasource.WeaponRemoteDataSourceImpl
import io.github.jan.supabase.SupabaseClient
import org.koin.dsl.module

/**
 * Plain Koin, same style `:core:database`'s `databaseModule` / `:tools:map-editor`'s `appModule`
 * use — not the dormant koin-annotations/KSP convention plugin. Exposes the remote data sources
 * (one per backend table, sharing the one [SupabaseClient]) plus [NewsFeedFetcher] — not
 * table-shaped, since news content is fetched straight from each `feed_sources` URL on-device,
 * not read from a backend table (see `:core:model`'s `NewsItem` KDoc). Nothing here reads or
 * writes `:core:database`, since reconciling the two is a sync engine's job (`:core:data`, step
 * 7B), deliberately not this module's.
 */
val networkModule = module {
    single<SupabaseClient> { SupabaseClientProvider.create(SupabaseConfig.fromBuildConfig()) }
    single<WeaponRemoteDataSource> { WeaponRemoteDataSourceImpl(get()) }
    single<VehicleRemoteDataSource> { VehicleRemoteDataSourceImpl(get()) }
    single<ItemRemoteDataSource> { ItemRemoteDataSourceImpl(get()) }
    single<StationRemoteDataSource> { StationRemoteDataSourceImpl(get()) }
    single<SongRemoteDataSource> { SongRemoteDataSourceImpl(get()) }
    single<MapConfigRemoteDataSource> { MapConfigRemoteDataSourceImpl(get()) }
    single<MapPinRemoteDataSource> { MapPinRemoteDataSourceImpl(get()) }
    single<FeedSourceRemoteDataSource> { FeedSourceRemoteDataSourceImpl(get()) }
    single<NewsFeedFetcher> { NewsFeedFetcherImpl() }
}

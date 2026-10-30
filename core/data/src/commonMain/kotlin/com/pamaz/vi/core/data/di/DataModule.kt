package com.pamaz.vi.core.data.di

import com.pamaz.vi.core.data.connectivity.ConnectivityMonitor
import com.pamaz.vi.core.data.connectivity.PlatformConnectivityMonitor
import com.pamaz.vi.core.data.repository.CatalogRepository
import com.pamaz.vi.core.data.repository.CatalogRepositoryImpl
import com.pamaz.vi.core.data.repository.MapRepository
import com.pamaz.vi.core.data.repository.MapRepositoryImpl
import com.pamaz.vi.core.data.repository.MusicRepository
import com.pamaz.vi.core.data.repository.MusicRepositoryImpl
import com.pamaz.vi.core.data.repository.SettingsRepository
import com.pamaz.vi.core.data.repository.SettingsRepositoryImpl
import com.pamaz.vi.core.data.seed.SeedImporter
import com.pamaz.vi.core.data.seed.SeedImporterImpl
import com.pamaz.vi.core.data.sync.ContentSyncer
import com.pamaz.vi.core.data.sync.ContentSyncerImpl
import org.koin.dsl.module

/**
 * Plain Koin, same style as `:core:database`'s `databaseModule`/`:core:network`'s
 * `networkModule`. Expects `databaseModule` and `networkModule` to already be included wherever
 * this one is (every binding here depends on at least one of their singles), plus a
 * `PlatformContext` single (same requirement `databaseModule` already has — see its own README).
 */
val dataModule = module {
    single<CatalogRepository> { CatalogRepositoryImpl(get()) }
    single<MusicRepository> { MusicRepositoryImpl(get()) }
    single<MapRepository> { MapRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }

    single<ConnectivityMonitor> { PlatformConnectivityMonitor(get()) }

    single<SeedImporter> { SeedImporterImpl(get(), get(), get(), get(), get()) }

    single<ContentSyncer> {
        ContentSyncerImpl(
            database = get(),
            syncDao = get(),
            catalogDao = get(),
            musicDao = get(),
            mapDao = get(),
            mapConfigRemoteDataSource = get(),
            stationRemoteDataSource = get(),
            weaponRemoteDataSource = get(),
            vehicleRemoteDataSource = get(),
            itemRemoteDataSource = get(),
            songRemoteDataSource = get(),
            mapPinRemoteDataSource = get(),
            connectivityMonitor = get(),
        )
    }
}

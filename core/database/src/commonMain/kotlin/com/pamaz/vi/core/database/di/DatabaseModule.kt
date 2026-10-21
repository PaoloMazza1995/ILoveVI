package com.pamaz.vi.core.database.di

import com.pamaz.vi.core.database.DatabaseDriverFactory
import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.createDatabase
import com.pamaz.vi.core.database.dao.CatalogDao
import com.pamaz.vi.core.database.dao.CatalogDaoImpl
import com.pamaz.vi.core.database.dao.MapDao
import com.pamaz.vi.core.database.dao.MapDaoImpl
import com.pamaz.vi.core.database.dao.MusicDao
import com.pamaz.vi.core.database.dao.MusicDaoImpl
import com.pamaz.vi.core.database.dao.NewsDao
import com.pamaz.vi.core.database.dao.NewsDaoImpl
import com.pamaz.vi.core.database.dao.PreferencesDao
import com.pamaz.vi.core.database.dao.PreferencesDaoImpl
import com.pamaz.vi.core.database.dao.SyncDao
import com.pamaz.vi.core.database.dao.SyncDaoImpl
import org.koin.dsl.module

/**
 * Plain Koin, same style `:tools:map-editor`'s `appModule` uses — not the dormant
 * koin-annotations/KSP convention plugin. Expects a `PlatformContext` single to already be bound
 * by whichever app module includes this one (see this module's README's "Koin" section).
 */
val databaseModule = module {
    single { DatabaseDriverFactory(get()) }
    single<IloveViDatabase> { createDatabase(get()) }
    single<CatalogDao> { CatalogDaoImpl(get()) }
    single<MapDao> { MapDaoImpl(get()) }
    single<MusicDao> { MusicDaoImpl(get()) }
    single<NewsDao> { NewsDaoImpl(get()) }
    single<SyncDao> { SyncDaoImpl(get()) }
    single<PreferencesDao> { PreferencesDaoImpl(get()) }
}

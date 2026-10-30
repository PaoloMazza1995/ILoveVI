package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.IloveViDatabase

/**
 * Plain public factories for each DAO's `internal` `*Impl` class, for a caller that already holds
 * a real [IloveViDatabase] (e.g. `:core:data`'s own tests, wiring an in-memory database directly
 * without going through `databaseModule`/Koin/`PlatformContext` at all — see that module's test
 * README section) but can't construct `CatalogDaoImpl` etc. itself, since those stay `internal` to
 * this module. `databaseModule` could use these too but doesn't need to; both forms produce the
 * exact same instance.
 */
fun createCatalogDao(database: IloveViDatabase): CatalogDao = CatalogDaoImpl(database)
fun createMapDao(database: IloveViDatabase): MapDao = MapDaoImpl(database)
fun createMusicDao(database: IloveViDatabase): MusicDao = MusicDaoImpl(database)
fun createSyncDao(database: IloveViDatabase): SyncDao = SyncDaoImpl(database)
fun createPreferencesDao(database: IloveViDatabase): PreferencesDao = PreferencesDaoImpl(database)

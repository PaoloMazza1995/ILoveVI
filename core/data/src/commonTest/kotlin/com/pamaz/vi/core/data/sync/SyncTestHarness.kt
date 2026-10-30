package com.pamaz.vi.core.data.sync

import com.pamaz.vi.core.data.fake.FakeConnectivityMonitor
import com.pamaz.vi.core.data.fake.FakeItemRemoteDataSource
import com.pamaz.vi.core.data.fake.FakeMapConfigRemoteDataSource
import com.pamaz.vi.core.data.fake.FakeMapPinRemoteDataSource
import com.pamaz.vi.core.data.fake.FakeSongRemoteDataSource
import com.pamaz.vi.core.data.fake.FakeStationRemoteDataSource
import com.pamaz.vi.core.data.fake.FakeVehicleRemoteDataSource
import com.pamaz.vi.core.data.fake.FakeWeaponRemoteDataSource
import com.pamaz.vi.core.data.testDatabase
import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.dao.CatalogDao
import com.pamaz.vi.core.database.dao.MapDao
import com.pamaz.vi.core.database.dao.MusicDao
import com.pamaz.vi.core.database.dao.SyncDao
import com.pamaz.vi.core.database.dao.createCatalogDao
import com.pamaz.vi.core.database.dao.createMapDao
import com.pamaz.vi.core.database.dao.createMusicDao
import com.pamaz.vi.core.database.dao.createSyncDao

/** One real, fresh in-memory database + every fake remote data source, wired into a real [ContentSyncerImpl] — the fixture every `ContentSyncer` test in this package shares. */
internal class SyncTestHarness {
    val database: IloveViDatabase = testDatabase()
    val syncDao: SyncDao = createSyncDao(database)
    val catalogDao: CatalogDao = createCatalogDao(database)
    val musicDao: MusicDao = createMusicDao(database)
    val mapDao: MapDao = createMapDao(database)

    val weapons = FakeWeaponRemoteDataSource()
    val vehicles = FakeVehicleRemoteDataSource()
    val items = FakeItemRemoteDataSource()
    val stations = FakeStationRemoteDataSource()
    val songs = FakeSongRemoteDataSource()
    val mapConfig = FakeMapConfigRemoteDataSource()
    val mapPins = FakeMapPinRemoteDataSource()
    val connectivity = FakeConnectivityMonitor()

    val syncer: ContentSyncer = ContentSyncerImpl(
        database = database,
        syncDao = syncDao,
        catalogDao = catalogDao,
        musicDao = musicDao,
        mapDao = mapDao,
        mapConfigRemoteDataSource = mapConfig,
        stationRemoteDataSource = stations,
        weaponRemoteDataSource = weapons,
        vehicleRemoteDataSource = vehicles,
        itemRemoteDataSource = items,
        songRemoteDataSource = songs,
        mapPinRemoteDataSource = mapPins,
        connectivityMonitor = connectivity,
    )
}

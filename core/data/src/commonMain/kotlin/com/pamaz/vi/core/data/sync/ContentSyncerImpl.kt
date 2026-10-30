package com.pamaz.vi.core.data.sync

import com.pamaz.vi.core.data.connectivity.ConnectivityMonitor
import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.dao.CatalogDao
import com.pamaz.vi.core.database.dao.MapDao
import com.pamaz.vi.core.database.dao.MusicDao
import com.pamaz.vi.core.database.dao.SyncDao
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.Song
import com.pamaz.vi.core.model.SongId
import com.pamaz.vi.core.model.Station
import com.pamaz.vi.core.model.StationId
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.network.datasource.ItemRemoteDataSource
import com.pamaz.vi.core.network.datasource.MapConfigRemoteDataSource
import com.pamaz.vi.core.network.datasource.MapPinRemoteDataSource
import com.pamaz.vi.core.network.datasource.SongRemoteDataSource
import com.pamaz.vi.core.network.datasource.StationRemoteDataSource
import com.pamaz.vi.core.network.datasource.VehicleRemoteDataSource
import com.pamaz.vi.core.network.datasource.WeaponRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Clock

/** The order the roadmap step asks for: `map_config`, `stations`, entries, `songs`, `map_pins`. */
internal class ContentSyncerImpl(
    private val database: IloveViDatabase,
    private val syncDao: SyncDao,
    private val catalogDao: CatalogDao,
    private val musicDao: MusicDao,
    private val mapDao: MapDao,
    private val mapConfigRemoteDataSource: MapConfigRemoteDataSource,
    private val stationRemoteDataSource: StationRemoteDataSource,
    private val weaponRemoteDataSource: WeaponRemoteDataSource,
    private val vehicleRemoteDataSource: VehicleRemoteDataSource,
    private val itemRemoteDataSource: ItemRemoteDataSource,
    private val songRemoteDataSource: SongRemoteDataSource,
    private val mapPinRemoteDataSource: MapPinRemoteDataSource,
    private val connectivityMonitor: ConnectivityMonitor,
) : ContentSyncer {

    private val syncMutex = Mutex()
    private val _syncState = MutableStateFlow(SyncState.INITIAL)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    override fun start(scope: CoroutineScope) {
        scope.launch { sync() }
        scope.launch {
            connectivityMonitor.isOnline
                .drop(1)
                .distinctUntilChanged()
                .filter { online -> online }
                .collect { sync() }
        }
    }

    override suspend fun sync() {
        if (!syncMutex.tryLock()) return
        try {
            _syncState.update { it.copy(status = SyncStatus.RUNNING) }

            val errors = listOfNotNull(
                syncMapConfig(),
                syncStations(),
                syncWeapons(),
                syncVehicles(),
                syncItems(),
                syncSongs(),
                syncMapPins(),
            )

            _syncState.value = SyncState(
                status = SyncStatus.IDLE,
                lastUpdatedAt = Clock.System.now(),
                lastError = errors.takeIf { it.isNotEmpty() }?.joinToString("; "),
            )
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun syncMapConfig(): String? = syncTable(
        database = database,
        syncDao = syncDao,
        tableName = "map_config",
        fetchPage = mapConfigRemoteDataSource::fetchChanges,
        updatedAtOf = MapConfig::updatedAt,
        applyChange = { change ->
            val config = change.item
            val deletedAt = change.deletedAt
            if (config != null) {
                mapDao.upsertMapConfig(config)
            } else if (deletedAt != null) {
                mapDao.tombstoneMapConfig(change.id.toInt(), deletedAt, deletedAt)
            }
        },
    )

    private suspend fun syncStations(): String? = syncTable(
        database = database,
        syncDao = syncDao,
        tableName = "stations",
        fetchPage = stationRemoteDataSource::fetchChanges,
        updatedAtOf = Station::updatedAt,
        applyChange = { change ->
            val station = change.item
            val deletedAt = change.deletedAt
            if (station != null) {
                musicDao.upsertStation(station)
            } else if (deletedAt != null) {
                musicDao.tombstoneStation(StationId(change.id), deletedAt, deletedAt)
            }
        },
    )

    private suspend fun syncWeapons(): String? = syncTable(
        database = database,
        syncDao = syncDao,
        tableName = "weapons",
        fetchPage = weaponRemoteDataSource::fetchChanges,
        updatedAtOf = Weapon::updatedAt,
        applyChange = { change ->
            val weapon = change.item
            val deletedAt = change.deletedAt
            if (weapon != null) {
                catalogDao.upsertWeapon(weapon)
            } else if (deletedAt != null) {
                catalogDao.tombstoneWeapon(WeaponId(change.id), deletedAt, deletedAt)
            }
        },
    )

    private suspend fun syncVehicles(): String? = syncTable(
        database = database,
        syncDao = syncDao,
        tableName = "vehicles",
        fetchPage = vehicleRemoteDataSource::fetchChanges,
        updatedAtOf = Vehicle::updatedAt,
        applyChange = { change ->
            val vehicle = change.item
            val deletedAt = change.deletedAt
            if (vehicle != null) {
                catalogDao.upsertVehicle(vehicle)
            } else if (deletedAt != null) {
                catalogDao.tombstoneVehicle(VehicleId(change.id), deletedAt, deletedAt)
            }
        },
    )

    private suspend fun syncItems(): String? = syncTable(
        database = database,
        syncDao = syncDao,
        tableName = "items",
        fetchPage = itemRemoteDataSource::fetchChanges,
        updatedAtOf = Item::updatedAt,
        applyChange = { change ->
            val item = change.item
            val deletedAt = change.deletedAt
            if (item != null) {
                catalogDao.upsertItem(item)
            } else if (deletedAt != null) {
                catalogDao.tombstoneItem(ItemId(change.id), deletedAt, deletedAt)
            }
        },
    )

    private suspend fun syncSongs(): String? = syncTable(
        database = database,
        syncDao = syncDao,
        tableName = "songs",
        fetchPage = songRemoteDataSource::fetchChanges,
        updatedAtOf = Song::updatedAt,
        applyChange = { change ->
            val song = change.item
            val deletedAt = change.deletedAt
            if (song != null) {
                musicDao.upsertSong(song)
            } else if (deletedAt != null) {
                musicDao.tombstoneSong(SongId(change.id), deletedAt, deletedAt)
            }
        },
    )

    private suspend fun syncMapPins(): String? = syncTable(
        database = database,
        syncDao = syncDao,
        tableName = "map_pins",
        fetchPage = mapPinRemoteDataSource::fetchChanges,
        updatedAtOf = MapPin::updatedAt,
        applyChange = { change ->
            val pin = change.item
            val deletedAt = change.deletedAt
            if (pin != null) {
                mapDao.upsertPin(pin)
            } else if (deletedAt != null) {
                mapDao.tombstonePin(MapPinId(change.id), deletedAt, deletedAt)
            }
        },
    )
}

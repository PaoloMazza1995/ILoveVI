package com.pamaz.vi.core.data.sync

import com.pamaz.vi.core.data.connectivity.ConnectivityMonitor
import com.pamaz.vi.core.data.news.FeedParseException
import com.pamaz.vi.core.data.news.MAX_ITEMS_PER_TOPIC
import com.pamaz.vi.core.data.news.ParsedFeedItem
import com.pamaz.vi.core.data.news.mergeAndDedupe
import com.pamaz.vi.core.data.news.parseFeed
import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.dao.CatalogDao
import com.pamaz.vi.core.database.dao.MapDao
import com.pamaz.vi.core.database.dao.MusicDao
import com.pamaz.vi.core.database.dao.NewsDao
import com.pamaz.vi.core.database.dao.SyncDao
import com.pamaz.vi.core.database.inTransaction
import com.pamaz.vi.core.model.FeedSource
import com.pamaz.vi.core.model.FeedSourceId
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NewsTopic
import com.pamaz.vi.core.model.Song
import com.pamaz.vi.core.model.SongId
import com.pamaz.vi.core.model.Station
import com.pamaz.vi.core.model.StationId
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.network.NetworkResult
import com.pamaz.vi.core.network.datasource.FeedSourceRemoteDataSource
import com.pamaz.vi.core.network.datasource.ItemRemoteDataSource
import com.pamaz.vi.core.network.datasource.MapConfigRemoteDataSource
import com.pamaz.vi.core.network.datasource.MapPinRemoteDataSource
import com.pamaz.vi.core.network.datasource.NewsFeedFetcher
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
import kotlinx.coroutines.flow.first
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
    private val newsDao: NewsDao,
    private val mapConfigRemoteDataSource: MapConfigRemoteDataSource,
    private val stationRemoteDataSource: StationRemoteDataSource,
    private val weaponRemoteDataSource: WeaponRemoteDataSource,
    private val vehicleRemoteDataSource: VehicleRemoteDataSource,
    private val itemRemoteDataSource: ItemRemoteDataSource,
    private val songRemoteDataSource: SongRemoteDataSource,
    private val mapPinRemoteDataSource: MapPinRemoteDataSource,
    private val feedSourceRemoteDataSource: FeedSourceRemoteDataSource,
    private val newsFeedFetcher: NewsFeedFetcher,
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
                syncFeedSources(),
                syncNews(),
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

    private suspend fun syncFeedSources(): String? = syncTable(
        database = database,
        syncDao = syncDao,
        tableName = "feed_sources",
        fetchPage = feedSourceRemoteDataSource::fetchChanges,
        updatedAtOf = FeedSource::updatedAt,
        applyChange = { change ->
            val feedSource = change.item
            val deletedAt = change.deletedAt
            if (feedSource != null) {
                newsDao.upsertFeedSource(feedSource)
            } else if (deletedAt != null) {
                newsDao.tombstoneFeedSource(FeedSourceId(change.id), deletedAt, deletedAt)
            }
        },
    )

    /**
     * No backend `news_items` table to page through (see `:core:model`'s `NewsItem` KDoc) —
     * instead, for every enabled [FeedSource] already synced locally by [syncFeedSources], fetches
     * that feed's raw XML ([newsFeedFetcher]) and parses it ([parseFeed]) independently: one
     * feed's network failure or malformed XML is recorded as its own error string and never stops
     * any other feed's fetch, the same "one feed must not block the others" property this
     * project's earlier backend `fetch-news` job's `orchestrate.ts` had. Every feed that did
     * yield items is merged/de-duplicated in one pass ([mergeAndDedupe]), upserted, and each topic
     * trimmed back down to [MAX_ITEMS_PER_TOPIC] — all inside one transaction, so a reader never
     * sees a half-applied sync pass.
     */
    private suspend fun syncNews(): String? {
        val feeds = newsDao.feedSources().first().filter { it.enabled }
        if (feeds.isEmpty()) return null

        val feedErrors = mutableListOf<String>()
        val feedResults = mutableListOf<Pair<FeedSource, List<ParsedFeedItem>>>()

        for (feed in feeds) {
            val xml = when (val xmlResult = newsFeedFetcher.fetchXml(feed.url)) {
                is NetworkResult.Success -> xmlResult.data
                is NetworkResult.Offline -> {
                    feedErrors.add("${feed.id.value}: offline")
                    continue
                }

                is NetworkResult.Timeout -> {
                    feedErrors.add("${feed.id.value}: timed out")
                    continue
                }

                is NetworkResult.ServerError -> {
                    feedErrors.add("${feed.id.value}: server error ${xmlResult.code}")
                    continue
                }

                is NetworkResult.ParseError -> {
                    feedErrors.add("${feed.id.value}: could not parse response")
                    continue
                }
            }
            try {
                feedResults.add(feed to parseFeed(xml))
            } catch (e: FeedParseException) {
                feedErrors.add("${feed.id.value}: ${e.message}")
            }
        }

        val merged = mergeAndDedupe(feedResults)
        database.inTransaction {
            merged.forEach { newsDao.upsertNewsItem(it) }
            val now = Clock.System.now()
            NewsTopic.entries.forEach { topic ->
                newsDao.trimNewsItems(
                    topic,
                    MAX_ITEMS_PER_TOPIC,
                    now,
                    now
                )
            }
        }

        return feedErrors.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }
}

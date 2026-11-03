package com.pamaz.vi.core.data.fake

import com.pamaz.vi.core.model.FeedSource
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.Song
import com.pamaz.vi.core.model.Station
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.network.Change
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
import kotlin.time.Instant

/**
 * A scriptable `fetchChanges` queue shared by every `Fake*RemoteDataSource` below: each test
 * [enqueue]s the exact [NetworkResult] pages it wants returned, in order; once the queue is
 * empty, every further call returns an empty [NetworkResult.Success] (the "nothing changed"
 * steady state [syncTable][com.pamaz.vi.core.data.sync.syncTable] stops paging on). [sinceLog]
 * records every `since` a test was asked with, so a test can assert the *second* sync's `since`
 * was the first sync's newest row (the "second sync downloads only the changes" acceptance case).
 */
internal open class FakeRemoteDataSourceCore<T> {
    private val responses = ArrayDeque<NetworkResult<List<Change<T>>>>()
    val sinceLog = mutableListOf<Instant?>()

    fun enqueue(result: NetworkResult<List<Change<T>>>) {
        responses.addLast(result)
    }

    fun enqueueSuccess(changes: List<Change<T>>) = enqueue(NetworkResult.Success(changes))

    suspend fun fetchChanges(since: Instant?, pageSize: Int): NetworkResult<List<Change<T>>> {
        sinceLog.add(since)
        return if (responses.isNotEmpty()) responses.removeFirst() else NetworkResult.Success(
            emptyList()
        )
    }
}

internal class FakeWeaponRemoteDataSource : FakeRemoteDataSourceCore<Weapon>(),
    WeaponRemoteDataSource

internal class FakeVehicleRemoteDataSource : FakeRemoteDataSourceCore<Vehicle>(),
    VehicleRemoteDataSource

internal class FakeItemRemoteDataSource : FakeRemoteDataSourceCore<Item>(), ItemRemoteDataSource
internal class FakeStationRemoteDataSource : FakeRemoteDataSourceCore<Station>(),
    StationRemoteDataSource

internal class FakeSongRemoteDataSource : FakeRemoteDataSourceCore<Song>(), SongRemoteDataSource
internal class FakeMapConfigRemoteDataSource : FakeRemoteDataSourceCore<MapConfig>(),
    MapConfigRemoteDataSource

internal class FakeMapPinRemoteDataSource : FakeRemoteDataSourceCore<MapPin>(),
    MapPinRemoteDataSource

internal class FakeFeedSourceRemoteDataSource : FakeRemoteDataSourceCore<FeedSource>(),
    FeedSourceRemoteDataSource

/**
 * A scriptable per-URL `fetchXml` queue — unlike [FakeRemoteDataSourceCore] (one `since`-paged
 * queue per table), [NewsFeedFetcher] is called once per feed *url*, so each url gets its own
 * queue. A url with nothing enqueued falls back to an empty-but-valid RSS feed rather than an
 * error, so a test that doesn't care about a particular feed's content doesn't have to script one.
 */
internal class FakeNewsFeedFetcher : NewsFeedFetcher {
    private val responses = mutableMapOf<String, ArrayDeque<NetworkResult<String>>>()
    val requestedUrls = mutableListOf<String>()

    fun enqueueXml(url: String, xml: String) = enqueue(url, NetworkResult.Success(xml))

    fun enqueue(url: String, result: NetworkResult<String>) {
        responses.getOrPut(url) { ArrayDeque() }.addLast(result)
    }

    override suspend fun fetchXml(url: String): NetworkResult<String> {
        requestedUrls.add(url)
        val queue = responses[url]
        return if (queue != null && queue.isNotEmpty()) {
            queue.removeFirst()
        } else {
            NetworkResult.Success("<rss version=\"2.0\"><channel></channel></rss>")
        }
    }
}

package com.pamaz.vi.core.network.datasource

import com.pamaz.vi.core.model.FeedSource
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.NetworkResult
import com.pamaz.vi.core.network.dto.FeedSourceDto
import com.pamaz.vi.core.network.mapper.toChange
import com.pamaz.vi.core.network.safeNetworkCall
import io.github.jan.supabase.SupabaseClient
import kotlin.time.Instant

/** One page of `feed_sources` rows changed since [since] — see `DeltaPage.kt`'s KDoc for the convention this follows. */
interface FeedSourceRemoteDataSource {
    suspend fun fetchChanges(
        since: Instant?,
        pageSize: Int
    ): NetworkResult<List<Change<FeedSource>>>
}

internal class FeedSourceRemoteDataSourceImpl(private val client: SupabaseClient) :
    FeedSourceRemoteDataSource {
    override suspend fun fetchChanges(
        since: Instant?,
        pageSize: Int
    ): NetworkResult<List<Change<FeedSource>>> =
        safeNetworkCall {
            fetchDeltaPage<FeedSourceDto>(
                client,
                "feed_sources",
                since,
                pageSize
            ).map { it.toChange() }
        }
}

package com.pamaz.vi.core.network.datasource

import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.NetworkResult
import com.pamaz.vi.core.network.dto.ItemDto
import com.pamaz.vi.core.network.mapper.toChange
import com.pamaz.vi.core.network.safeNetworkCall
import io.github.jan.supabase.SupabaseClient
import kotlin.time.Instant

/** One page of `items` rows changed since [since] — see `DeltaPage.kt`'s KDoc for the convention this follows. */
interface ItemRemoteDataSource {
    suspend fun fetchChanges(since: Instant?, pageSize: Int): NetworkResult<List<Change<Item>>>
}

internal class ItemRemoteDataSourceImpl(private val client: SupabaseClient) : ItemRemoteDataSource {
    override suspend fun fetchChanges(
        since: Instant?,
        pageSize: Int
    ): NetworkResult<List<Change<Item>>> =
        safeNetworkCall {
            fetchDeltaPage<ItemDto>(client, "items", since, pageSize).map { it.toChange() }
        }
}

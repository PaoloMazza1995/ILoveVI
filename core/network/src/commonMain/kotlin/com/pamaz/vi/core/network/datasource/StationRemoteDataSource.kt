package com.pamaz.vi.core.network.datasource

import com.pamaz.vi.core.model.Station
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.NetworkResult
import com.pamaz.vi.core.network.dto.StationDto
import com.pamaz.vi.core.network.mapper.toChange
import com.pamaz.vi.core.network.safeNetworkCall
import io.github.jan.supabase.SupabaseClient
import kotlin.time.Instant

/** One page of `stations` rows changed since [since] — see `DeltaPage.kt`'s KDoc for the convention this follows. */
interface StationRemoteDataSource {
    suspend fun fetchChanges(since: Instant?, pageSize: Int): NetworkResult<List<Change<Station>>>
}

internal class StationRemoteDataSourceImpl(private val client: SupabaseClient) :
    StationRemoteDataSource {
    override suspend fun fetchChanges(
        since: Instant?,
        pageSize: Int
    ): NetworkResult<List<Change<Station>>> =
        safeNetworkCall {
            fetchDeltaPage<StationDto>(client, "stations", since, pageSize).map { it.toChange() }
        }
}

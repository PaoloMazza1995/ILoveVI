package com.pamaz.vi.core.network.datasource

import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.NetworkResult
import com.pamaz.vi.core.network.dto.MapPinDto
import com.pamaz.vi.core.network.mapper.toChange
import com.pamaz.vi.core.network.safeNetworkCall
import io.github.jan.supabase.SupabaseClient
import kotlin.time.Instant

/** One page of `map_pins` rows changed since [since] — see `DeltaPage.kt`'s KDoc for the convention this follows. */
interface MapPinRemoteDataSource {
    suspend fun fetchChanges(since: Instant?, pageSize: Int): NetworkResult<List<Change<MapPin>>>
}

internal class MapPinRemoteDataSourceImpl(private val client: SupabaseClient) :
    MapPinRemoteDataSource {
    override suspend fun fetchChanges(
        since: Instant?,
        pageSize: Int
    ): NetworkResult<List<Change<MapPin>>> =
        safeNetworkCall {
            fetchDeltaPage<MapPinDto>(client, "map_pins", since, pageSize).map { it.toChange() }
        }
}

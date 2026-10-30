package com.pamaz.vi.core.network.datasource

import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.NetworkResult
import com.pamaz.vi.core.network.dto.VehicleDto
import com.pamaz.vi.core.network.mapper.toChange
import com.pamaz.vi.core.network.safeNetworkCall
import io.github.jan.supabase.SupabaseClient
import kotlin.time.Instant

/** One page of `vehicles` rows changed since [since] — see `DeltaPage.kt`'s KDoc for the convention this follows. */
interface VehicleRemoteDataSource {
    suspend fun fetchChanges(since: Instant?, pageSize: Int): NetworkResult<List<Change<Vehicle>>>
}

internal class VehicleRemoteDataSourceImpl(private val client: SupabaseClient) :
    VehicleRemoteDataSource {
    override suspend fun fetchChanges(
        since: Instant?,
        pageSize: Int
    ): NetworkResult<List<Change<Vehicle>>> =
        safeNetworkCall {
            fetchDeltaPage<VehicleDto>(client, "vehicles", since, pageSize).map { it.toChange() }
        }
}

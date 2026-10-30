package com.pamaz.vi.core.network.datasource

import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.NetworkResult
import com.pamaz.vi.core.network.dto.MapConfigDto
import com.pamaz.vi.core.network.mapper.toChange
import com.pamaz.vi.core.network.safeNetworkCall
import io.github.jan.supabase.SupabaseClient
import kotlin.time.Instant

/** One page of `map_config` rows changed since [since] — see `DeltaPage.kt`'s KDoc for the convention this follows. Ordered by `updated_at` then `map_version` (this table's primary key, not a text `id`). */
interface MapConfigRemoteDataSource {
    suspend fun fetchChanges(since: Instant?, pageSize: Int): NetworkResult<List<Change<MapConfig>>>
}

internal class MapConfigRemoteDataSourceImpl(private val client: SupabaseClient) :
    MapConfigRemoteDataSource {
    override suspend fun fetchChanges(
        since: Instant?,
        pageSize: Int
    ): NetworkResult<List<Change<MapConfig>>> =
        safeNetworkCall {
            fetchDeltaPage<MapConfigDto>(
                client,
                "map_config",
                since,
                pageSize,
                idColumn = "map_version"
            )
                .map { it.toChange() }
        }
}

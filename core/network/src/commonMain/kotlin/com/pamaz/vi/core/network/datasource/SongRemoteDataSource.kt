package com.pamaz.vi.core.network.datasource

import com.pamaz.vi.core.model.Song
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.NetworkResult
import com.pamaz.vi.core.network.dto.SongDto
import com.pamaz.vi.core.network.mapper.toChange
import com.pamaz.vi.core.network.safeNetworkCall
import io.github.jan.supabase.SupabaseClient
import kotlin.time.Instant

/** One page of `songs` rows changed since [since] — see `DeltaPage.kt`'s KDoc for the convention this follows. */
interface SongRemoteDataSource {
    suspend fun fetchChanges(since: Instant?, pageSize: Int): NetworkResult<List<Change<Song>>>
}

internal class SongRemoteDataSourceImpl(private val client: SupabaseClient) : SongRemoteDataSource {
    override suspend fun fetchChanges(
        since: Instant?,
        pageSize: Int
    ): NetworkResult<List<Change<Song>>> =
        safeNetworkCall {
            fetchDeltaPage<SongDto>(client, "songs", since, pageSize).map { it.toChange() }
        }
}

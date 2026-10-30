package com.pamaz.vi.core.network.datasource

import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.NetworkResult
import com.pamaz.vi.core.network.dto.WeaponDto
import com.pamaz.vi.core.network.mapper.toChange
import com.pamaz.vi.core.network.safeNetworkCall
import io.github.jan.supabase.SupabaseClient
import kotlin.time.Instant

/** One page of `weapons` rows changed since [since] — see `DeltaPage.kt`'s KDoc for the convention this follows. */
interface WeaponRemoteDataSource {
    suspend fun fetchChanges(since: Instant?, pageSize: Int): NetworkResult<List<Change<Weapon>>>
}

internal class WeaponRemoteDataSourceImpl(private val client: SupabaseClient) :
    WeaponRemoteDataSource {
    override suspend fun fetchChanges(
        since: Instant?,
        pageSize: Int
    ): NetworkResult<List<Change<Weapon>>> =
        safeNetworkCall {
            fetchDeltaPage<WeaponDto>(client, "weapons", since, pageSize).map { it.toChange() }
        }
}

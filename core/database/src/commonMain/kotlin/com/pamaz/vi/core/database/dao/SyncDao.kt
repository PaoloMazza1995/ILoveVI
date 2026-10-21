package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.asOneOrNullFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/**
 * Delta-sync bookkeeping: when each backend table was last fully synced. Upserting/tombstoning
 * the content tables themselves is each feature DAO's job ([CatalogDao], [MapDao], [MusicDao],
 * [NewsDao]) since they already own those tables' queries.
 */
interface SyncDao {
    fun lastSyncedAt(tableName: String): Flow<Instant?>
    suspend fun setLastSyncedAt(tableName: String, at: Instant)
}

internal class SyncDaoImpl(database: IloveViDatabase) : SyncDao {
    private val syncStateQueries = database.syncStateQueries

    override fun lastSyncedAt(tableName: String): Flow<Instant?> =
        syncStateQueries.selectByTable(tableName) { _, lastSyncedAt -> lastSyncedAt }
            .asOneOrNullFlow()
            .map { it?.let(Instant::fromEpochMilliseconds) }

    override suspend fun setLastSyncedAt(tableName: String, at: Instant) {
        syncStateQueries.upsert(tableName, at.toEpochMilliseconds())
    }
}

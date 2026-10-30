package com.pamaz.vi.core.data.sync

import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.dao.SyncDao
import com.pamaz.vi.core.database.inTransaction
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.NetworkResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlin.time.Instant

/** Rows fetched per page — matches `:core:network`'s README's "Delta sync convention" discussion of what a reasonable page size is. */
internal const val SYNC_PAGE_SIZE = 200

/**
 * Pages one table's changes to completion (or until something fails), following the convention
 * `:core:network`'s `fetchDeltaPage` already implements: `since = null` is a full first sync;
 * otherwise every page's rows are applied and the table's cursor (`SyncDao`) advanced *together*,
 * in one transaction, so a crash between pages leaves the database and the cursor agreeing with
 * each other — the next [syncTable] call resumes exactly where the last committed page left off,
 * never re-applying it and never skipping past it.
 *
 * Returns `null` on full success, or a short human-readable error otherwise (the caller folds
 * this into [SyncState.lastError] without failing any other table's own [syncTable] call).
 */
internal suspend fun <T> syncTable(
    database: IloveViDatabase,
    syncDao: SyncDao,
    tableName: String,
    fetchPage: suspend (since: Instant?, pageSize: Int) -> NetworkResult<List<Change<T>>>,
    updatedAtOf: (T) -> Instant,
    applyChange: suspend (Change<T>) -> Unit,
): String? {
    var since = syncDao.lastSyncedAt(tableName).first()
    try {
        while (true) {
            val page = when (val result = fetchPage(since, SYNC_PAGE_SIZE)) {
                is NetworkResult.Success -> result.data
                is NetworkResult.Offline -> return "$tableName: offline"
                is NetworkResult.Timeout -> return "$tableName: timed out"
                is NetworkResult.ServerError -> return "$tableName: server error ${result.code}"
                is NetworkResult.ParseError -> return "$tableName: could not parse response"
            }
            if (page.isEmpty()) return null

            val pageSince =
                page.maxOf { change -> change.item?.let(updatedAtOf) ?: change.deletedAt!! }
            database.inTransaction {
                page.forEach { applyChange(it) }
                syncDao.setLastSyncedAt(tableName, pageSince)
            }
            since = pageSince

            if (page.size < SYNC_PAGE_SIZE) return null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return "$tableName: ${e.message ?: e::class.simpleName}"
    }
}

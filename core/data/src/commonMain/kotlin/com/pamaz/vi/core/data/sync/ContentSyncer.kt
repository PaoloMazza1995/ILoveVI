package com.pamaz.vi.core.data.sync

import com.pamaz.vi.core.data.connectivity.ConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Keeps the local database (`:core:database`) up to date with the backend (`:core:network`),
 * table by table, in the order `map_config`, `stations`, `weapons`/`vehicles`/`items`, `songs`,
 * `map_pins` — `songs` references `stations` by id, so stations must land first; the others have
 * no real ordering requirement but follow the same sequence for consistency. `feed_sources` is
 * deliberately not synced here: it belongs to the news feature (step 12A/12B), not this one.
 *
 * Each table is synced independently: [sync] fetches that table's changes page by page (via its
 * `:core:network` remote data source), applying every page — upserts and tombstones alike — in
 * one local transaction that also advances that table's own delta-sync cursor
 * (`:core:database`'s `SyncDao`), so an interruption between pages resumes from the last
 * successfully committed page rather than redownloading everything or losing the already-applied
 * pages. **A table whose fetch or apply fails does not affect any other table** — each table's
 * loop is wrapped independently, and a failure simply stops paging *that* table while every other
 * table's loop proceeds on its own connection/transaction.
 */
interface ContentSyncer {
    /** The current/most recent sync pass's state — see [SyncState]'s KDoc. */
    val syncState: StateFlow<SyncState>

    /**
     * Runs one sync pass over every table, now. Safe to call concurrently — a call made while
     * another is already running is a no-op (returns immediately without starting a second pass).
     * Never throws: any per-table failure is captured into [syncState]'s `lastError`, not
     * propagated to the caller.
     */
    suspend fun sync()

    /**
     * Wires this syncer's two automatic triggers into [scope]: one immediate, non-blocking [sync]
     * call (the "sync at app start" trigger — [scope]'s own launch returns immediately, [start]
     * itself never suspends), and a second coroutine that calls [sync] again every time
     * [ConnectivityMonitor.isOnline] transitions from offline to online. The third trigger, manual
     * refresh, is just calling [sync] directly from wherever the "refresh" action lives — nothing
     * about it needs wiring here.
     */
    fun start(scope: CoroutineScope)
}

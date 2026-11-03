package com.pamaz.vi.core.data.sync

import com.pamaz.vi.core.data.connectivity.ConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * Keeps the local database (`:core:database`) up to date, table by table, in the order
 * `map_config`, `stations`, `weapons`/`vehicles`/`items`, `songs`, `map_pins`, `feed_sources`,
 * news — `songs` references `stations` by id and news fetching reads `feed_sources` (so the feed
 * list must already be current), so each must land before its dependent; the others have no real
 * ordering requirement but follow the same sequence for consistency.
 *
 * Every table except news is synced from the backend (`:core:network`): [sync] fetches that
 * table's changes page by page, applying every page — upserts and tombstones alike — in one local
 * transaction that also advances that table's own delta-sync cursor (`:core:database`'s
 * `SyncDao`), so an interruption between pages resumes from the last successfully committed page
 * rather than redownloading everything or losing the already-applied pages. News has no backend
 * table at all (see `:core:model`'s `NewsItem` KDoc): instead, every enabled `feed_sources` row's
 * RSS/Atom URL is fetched and parsed directly, on-device, independently per feed. **A table (or
 * feed) whose fetch or apply fails does not affect any other table (or feed)** — each loop is
 * wrapped independently, and a failure simply stops that one while everything else proceeds on
 * its own connection/transaction.
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

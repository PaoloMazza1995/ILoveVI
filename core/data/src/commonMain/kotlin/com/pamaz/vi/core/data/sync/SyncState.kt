package com.pamaz.vi.core.data.sync

import kotlin.time.Instant

/** Whether a sync pass is currently running — the Settings screen's "last updated"/spinner state. */
enum class SyncStatus { IDLE, RUNNING }

/**
 * The whole app's sync state, as the Settings screen (and anything else) observes it:
 * [status] (idle/running), [lastUpdatedAt] (when the most recent sync pass *finished*, successfully
 * or not — `null` before the very first pass), and [lastError] (a human-readable summary of the
 * most recent pass's failures, `null` if every table synced cleanly). A partial failure (one table
 * failing, see [ContentSyncer]'s KDoc) still updates [lastUpdatedAt] and sets [lastError], since the
 * other tables really did get newer data.
 */
data class SyncState(
    val status: SyncStatus,
    val lastUpdatedAt: Instant?,
    val lastError: String?,
) {
    companion object {
        val INITIAL = SyncState(status = SyncStatus.IDLE, lastUpdatedAt = null, lastError = null)
    }
}

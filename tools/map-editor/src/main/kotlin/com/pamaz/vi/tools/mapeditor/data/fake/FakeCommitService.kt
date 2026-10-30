package com.pamaz.vi.tools.mapeditor.data.fake

import com.pamaz.vi.tools.mapeditor.data.CommitPayload
import com.pamaz.vi.tools.mapeditor.data.CommitResult
import com.pamaz.vi.tools.mapeditor.data.CommitService
import kotlinx.coroutines.delay
import java.io.File

/**
 * Succeeds after [delayMs] (~1.5s, matching a real network round trip) with counts derived from
 * [payload] itself, or throws [RuntimeException] when [forceFailure] is set — the "fake
 * CommitService that succeeds after about 1.5s and can be set to fail from a debug flag" the
 * acceptance asks for, so the Commit dialog's saving/success/error states (EDT-73..75) can all be
 * exercised with no backend at all. `var forceFailure` is deliberately mutable (not a constructor-
 * only flag) so the same instance can be toggled between a success and a Retry-then-succeed demo
 * without re-wiring DI.
 */
class FakeCommitService(
    private val delayMs: Long = 1500,
    var forceFailure: Boolean = false,
) : CommitService {
    override suspend fun commit(
        payload: CommitPayload,
        pendingImages: Map<String, File>
    ): CommitResult {
        delay(delayMs)
        if (forceFailure) {
            throw RuntimeException("Could not reach the backend. Check your connection and try again.")
        }
        val existingEntryIds = setOf(
            "weapon-1",
            "weapon-2",
            "weapon-3",
            "vehicle-1",
            "vehicle-2",
            "vehicle-3",
            "item-1",
            "item-2"
        )
        return CommitResult(
            entriesCreated = payload.entries.count { it.id !in existingEntryIds },
            entriesUpdated = payload.entries.count { it.id in existingEntryIds },
            pinsCreated = payload.pins.size,
            pinsMoved = 0,
            imagesUploaded = pendingImages.size,
        )
    }
}

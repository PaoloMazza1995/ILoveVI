package com.pamaz.vi.core.network

import kotlin.time.Instant

/**
 * One row from a `fetchChanges` page, already mapped (or deliberately not) to a `:core:model`
 * type `T`, so the data layer (step 7B) can apply it without knowing anything about the wire
 * shape underneath.
 *
 * [item] is `null` in exactly two cases, both left to the caller to tell apart via [deletedAt]:
 * - **Tombstone**: [deletedAt] is non-null — the row was soft-deleted server-side. The caller
 *   should delete [id] locally; there is no point mapping the rest of a row that's going away.
 * - **Unmappable row**: [deletedAt] is `null` but the row's data couldn't be turned into a valid
 *   `T` (e.g. an unrecognized enum constant, or — for `map_pins` — a category that doesn't name
 *   one of the three known entry tables). The caller should skip [id] (and may want to log it),
 *   never delete it, since the row is still live server-side.
 *
 * A successfully mapped, non-deleted row has both [item] non-null and [deletedAt] `null`.
 */
data class Change<T>(
    val item: T?,
    val id: String,
    val deletedAt: Instant?,
)

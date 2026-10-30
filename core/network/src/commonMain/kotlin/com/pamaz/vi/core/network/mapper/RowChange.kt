package com.pamaz.vi.core.network.mapper

import com.pamaz.vi.core.network.Change
import kotlin.time.Instant

/**
 * Builds a [Change] from a row's raw `id`/`deleted_at`, sharing the tombstone-vs-unmappable logic
 * every `*Dto.toChange()` in this package needs (see [Change]'s KDoc for what the two `null`-item
 * cases mean):
 * - [deletedAtRaw] non-null (and parseable) → tombstone: [map] is never even called.
 * - Otherwise, [map] runs; any exception it throws (an unrecognized enum constant, an invalid
 *   `NormalizedPoint`/`MapConfig` value, a malformed `updated_at`, ...) is swallowed into a `null`
 *   item rather than propagated — one bad row must not fail an entire page.
 */
internal inline fun <T> rowChange(id: String, deletedAtRaw: String?, map: () -> T): Change<T> {
    if (deletedAtRaw != null) {
        val deletedAt = runCatching { Instant.parse(deletedAtRaw) }.getOrNull()
        return Change(item = null, id = id, deletedAt = deletedAt)
    }
    val item = runCatching(map).getOrNull()
    return Change(item = item, id = id, deletedAt = null)
}

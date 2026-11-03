package com.pamaz.vi.core.mapview.tile

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Persistent cache of downloaded tile bytes, keyed by the tile's full URL (see
 * [tileCacheFileName]) — versioned URLs are immutable (a new map version always gets a new
 * `tile_url_template`, per [com.pamaz.vi.core.model.MapConfig]'s own convention), so a cache hit
 * here never needs revalidation: once written, bytes for a given URL are valid forever, until
 * evicted purely for space. Size-capped via [maxBytes]; LRU by file modification time (the
 * cheapest per-platform "last used" signal without maintaining a separate index file — a plain
 * [read] doesn't bump it on most filesystems, but every [read] that matters for recency also
 * implies the tile is still in [TileMemoryCache], which *does* track real LRU order precisely —
 * this layer only needs to be approximately right, since a wrongly-evicted disk entry just means
 * one extra network fetch, not a correctness problem).
 */
internal class TileDiskCache(
    private val files: TileDiskFiles,
    private val maxBytes: Long,
) {
    private val mutex = Mutex()

    suspend fun read(url: String): ByteArray? =
        mutex.withLock { files.read(tileCacheFileName(url)) }

    suspend fun write(url: String, bytes: ByteArray) = mutex.withLock {
        files.write(tileCacheFileName(url), bytes)
        evictIfNeeded()
    }

    private fun evictIfNeeded() {
        val entries = files.list().sortedBy { it.lastModifiedMillis }
        var total = entries.sumOf { it.sizeBytes }
        var i = 0
        while (total > maxBytes && i < entries.size) {
            val victim = entries[i]
            files.delete(victim.fileName)
            total -= victim.sizeBytes
            i++
        }
    }
}

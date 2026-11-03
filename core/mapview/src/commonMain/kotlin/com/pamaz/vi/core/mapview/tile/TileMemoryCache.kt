package com.pamaz.vi.core.mapview.tile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * LRU cache of decoded tile bitmaps, bounded by total byte size rather than entry count (tiles are
 * all the same nominal size, but byte-budgeting is what actually protects memory, and matches how
 * [TileCacheConfig]'s limits are specified). [pinnedKeys] are exempt from eviction entirely — see
 * [TileCacheConfig.PINNED_MAX_ZOOM]'s KDoc for why.
 *
 * Backed by a single [MutableStateFlow] holding an immutable [Snapshot] rather than a mutable
 * `LinkedHashMap` behind a lock: [peek] (the hot path — called once per visible tile, every frame,
 * from the *draw* phase, which cannot suspend and must never block) is then just a synchronous,
 * lock-free read of `state.value`, and `MutableStateFlow.update` already makes every mutation
 * atomic without this class needing its own platform-specific lock (Kotlin Multiplatform has no
 * `@Synchronized` outside the JVM target).
 *
 * [generation] is the fix for a real, reported bug: a tile that finishes decoding *after* the
 * first frame used to never actually appear until some unrelated gesture (a drag/pinch) happened
 * to change [androidx.compose.ui.graphics.drawscope.DrawScope]-observed state and force a redraw —
 * [peek] itself is a plain [MutableStateFlow] read, not a Compose [androidx.compose.runtime.State]
 * read, so nothing told the canvas a new tile had actually arrived. [generation] *is* real Compose
 * state (backed by [mutableIntStateOf], safe to bump from any thread — the global snapshot handles
 * cross-thread writes); [TileLayer]'s draw lambda reads it once per frame purely to register a
 * draw-phase dependency, so every [put] (a tile's bytes actually becoming visible-content) now
 * correctly triggers a redraw on its own, with no gesture required.
 */
internal class TileMemoryCache(private val maxBytes: Long) {

    data class Entry(val bitmap: ImageBitmap, val bytes: Long)

    private data class Snapshot(
        val entries: Map<TileKey, Entry> = emptyMap(),
        /** Least-recently-used first; a `get`/`put` moves the key to the end. */
        val lruOrder: List<TileKey> = emptyList(),
        val totalBytes: Long = 0L,
    )

    private val state = MutableStateFlow(Snapshot())
    private var pinnedKeys: Set<TileKey> = emptySet()

    /** See this class's own KDoc. Bumped whenever [Snapshot.entries] actually changes ([put], [setPinnedKeys], [clearUnpinned]) — not on [touch] (LRU reordering alone never changes what's drawn). */
    var generation: Int by mutableIntStateOf(0)
        private set

    /** Synchronous, lock-free — safe to call from the draw phase. Does *not* update LRU order (a draw-phase read must never mutate shared state); see [touch] for that. */
    fun peek(key: TileKey): ImageBitmap? = state.value.entries[key]?.bitmap

    fun contains(key: TileKey): Boolean = key in state.value.entries

    /** Marks [key] as recently used, if present — called from the loader (not the draw phase) once a tile is confirmed still wanted. */
    fun touch(key: TileKey) {
        state.update { snapshot ->
            if (key !in snapshot.entries || key in pinnedKeys) return@update snapshot
            snapshot.copy(lruOrder = (snapshot.lruOrder - key) + key)
        }
    }

    /** No-ops if [key] is already cached (the loader is expected to check [contains] first; this is a defensive second check). */
    fun put(key: TileKey, bitmap: ImageBitmap, bytes: Long) {
        state.update { snapshot ->
            if (key in snapshot.entries) return@update snapshot
            var entries = snapshot.entries + (key to Entry(bitmap, bytes))
            var order = snapshot.lruOrder + key
            var total = snapshot.totalBytes + bytes
            while (total > maxBytes) {
                val victim = order.firstOrNull { it !in pinnedKeys } ?: break
                val victimEntry = entries[victim] ?: break
                entries = entries - victim
                order = order - victim
                total -= victimEntry.bytes
            }
            Snapshot(entries, order, total)
        }
        generation++
    }

    /** Replaces the pinned set — e.g. once the lowest zoom levels finish their initial load. Re-applies eviction immediately in case the *previous* pinned set was holding bytes the new budget can no longer spare. */
    fun setPinnedKeys(keys: Set<TileKey>) {
        pinnedKeys = keys
        state.update { snapshot ->
            var entries = snapshot.entries
            var order = snapshot.lruOrder
            var total = snapshot.totalBytes
            while (total > maxBytes) {
                val victim = order.firstOrNull { it !in pinnedKeys } ?: break
                val victimEntry = entries[victim] ?: break
                entries = entries - victim
                order = order - victim
                total -= victimEntry.bytes
            }
            snapshot.copy(entries = entries, lruOrder = order, totalBytes = total)
        }
        generation++
    }

    /** Responds to a low-memory signal (MAP-10-style mobile memory safety): drops everything *except* the pinned overview levels. */
    fun clearUnpinned() {
        state.update { snapshot ->
            val keptEntries = snapshot.entries.filterKeys { it in pinnedKeys }
            Snapshot(
                entries = keptEntries,
                lruOrder = snapshot.lruOrder.filter { it in pinnedKeys },
                totalBytes = keptEntries.values.sumOf { it.bytes },
            )
        }
        generation++
    }

    val entryCount: Int get() = state.value.entries.size
    val totalBytes: Long get() = state.value.totalBytes
    val pinnedCount: Int get() = pinnedKeys.size
}

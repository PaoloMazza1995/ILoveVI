package com.pamaz.vi.core.mapview.tile

import com.pamaz.vi.core.model.map.TileUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlin.time.Clock

/**
 * Why a tile is being asked for — decides which of three FIFO queues [TileLoadCoordinator.request]
 * enqueues it into, and therefore which tiles the [TileLoadCoordinator.maxParallelRequests] limited
 * workers pick up first: [VIEWPORT] tiles always drain before [PREFETCH_RING], which always drains
 * before [NEXT_ZOOM] — see the module README's "Loading policy" section. A caller that only ever
 * requests one zoom level's tiles at [VIEWPORT] priority per gesture (as [TileLayer] does) is what
 * makes "only the most recent target zoom level is requested during an active gesture" true in
 * practice — this class has no separate concept of "the gesture's current level" itself.
 */
internal enum class TileRequestPriority { VIEWPORT, PREFETCH_RING, NEXT_ZOOM }

/**
 * Owns every in-flight/queued tile fetch for one map version: priority queueing, bounded
 * parallelism, in-flight de-duplication (two [request] calls for the same key while it's already
 * queued/in-flight do nothing — the single underlying fetch is shared), retry with backoff, and
 * debounced cancellation of tiles that fall out of view. [request] is synchronous (never suspends)
 * so the draw phase can call it directly, once per visible/prefetch-ring/next-zoom tile, every
 * frame — seeing the *same* key again before it resolves is the normal, expected case (the whole
 * reason de-duplication exists), not a bug to guard against upstream.
 *
 * **Thread confinement.** Every queue/map below (`viewportQueue`/`ringQueue`/`nextZoomQueue`,
 * `queued`, `inFlight`, `failed`, `wantedAtMillis`) is touched *only* from inside [processEvents],
 * one coroutine started once in [init] that owns them exclusively — never directly from [request]
 * or from a fetch job's own completion. This matters because with [maxParallelRequests] fetches
 * genuinely running at once, their completions resume on whatever thread the HTTP engine happens
 * to use; several of those resuming at once used to mutate a plain `HashMap`/`ArrayDeque` straight
 * from [drainQueues]'s own `finally` block, with no synchronization at all — a real,
 * `MapViewerCameraStabilityUiTest`-caught `ConcurrentModificationException`, not a hypothetical
 * one (see the module README's "Tile pipeline" diagnosis). Routing every mutation through
 * [events] (`request`/a fetch job's completion both just [Channel.trySend] into it — lock-free and
 * safe from any thread by construction) and a single consumer reuses the exact same
 * confined-ownership pattern `MapGestures.kt`'s own `pendingSnaps` channel already established for
 * the camera, rather than inventing a second mechanism (a lock) for the same problem. [fetchOnce]
 * itself (the actual network/disk work, including its retry/backoff loop) deliberately stays
 * *outside* that confinement — it touches no shared state, so letting several of them run truly in
 * parallel across real threads is exactly the point of [maxParallelRequests].
 *
 * One instance is scoped to a single `(mapVersion, tileUrlTemplate)` pair — a new map version gets
 * a fresh coordinator (and therefore a fresh queue/in-flight set), never a reused one with stale
 * URLs.
 */
internal class TileLoadCoordinator(
    private val scope: CoroutineScope,
    private val memoryCache: TileMemoryCache,
    private val fetcher: TileFetcher,
    private val metrics: TileCacheMetrics,
    private val tileUrlTemplate: String,
    private val mapVersion: Int,
    private val tileSizePx: Int,
    private val maxParallelRequests: Int = TileCacheConfig.MAX_PARALLEL_REQUESTS,
    private val maxRetryAttempts: Int = TileCacheConfig.RETRY_MAX_ATTEMPTS,
    private val retryBaseDelayMs: Long = TileCacheConfig.RETRY_BASE_DELAY_MS,
    private val viewportStabilityDelayMs: Long = TileCacheConfig.VIEWPORT_STABILITY_DELAY_MS,
) {
    private sealed interface Event {
        data class Requested(
            val key: TileKey,
            val priority: TileRequestPriority,
            val nowMillis: Long
        ) : Event

        /** [result] is `null` for a cancelled (swept-as-stale) fetch — there's no result to apply, only a slot to free. */
        data class FetchDone(val key: TileKey, val result: TileFetchResult?) : Event
        data class Sweep(val sweptAtMillis: Long) : Event
    }

    private val events = Channel<Event>(capacity = Channel.UNLIMITED)

    // --- Confined state: see this class's own KDoc. Touched only from processEvents' coroutine. ---
    private val semaphore = Semaphore(maxParallelRequests)
    private val viewportQueue = ArrayDeque<TileKey>()
    private val ringQueue = ArrayDeque<TileKey>()
    private val nextZoomQueue = ArrayDeque<TileKey>()
    private val queued = HashSet<TileKey>()
    private val inFlight = HashMap<TileKey, Job>()
    private val failed = HashSet<TileKey>()
    private val wantedAtMillis = HashMap<TileKey, Long>()
    private var sweepJob: Job? = null
    // --- End confined state. ---

    private val _loadingKeys = MutableStateFlow<Set<TileKey>>(emptySet())
    val loadingKeys = _loadingKeys.asStateFlow()
    private val _failedKeys = MutableStateFlow<Set<TileKey>>(emptySet())
    val failedKeys = _failedKeys.asStateFlow()

    /** When each tile's bitmap actually finished loading — real arrival time, independent of when it first happens to be drawn. [TileLayer]'s crossfade reads this so a tile preloaded off-screen (pinned levels, prefetch ring) never fades in once it scrolls into view: it was already "there". A [MutableStateFlow] (not a plain map) so [arrivedAtMillis] can be read safely from the draw-phase thread while [processEvents] writes it — the same reasoning [TileMemoryCache]'s own KDoc gives for why it needs no separate lock. */
    private val _arrivedAtMillis = MutableStateFlow<Map<TileKey, Long>>(emptyMap())
    fun arrivedAtMillis(key: TileKey): Long? = _arrivedAtMillis.value[key]

    init {
        scope.launch {
            for (event in events) processEvent(event)
        }
    }

    /**
     * Asks for ([zoom], [tileX], [tileY]) at [priority]. A no-op if the tile is already decoded
     * (just [TileMemoryCache.touch]es it — checked right here, not deferred through [events],
     * since it's by far the hottest case: called once per visible tile, every frame, and
     * [TileMemoryCache] is already safe to touch from any thread without confinement). Otherwise
     * the actual dedup/priority/retry decision is handled by [processEvents], reached via
     * [events] — see this class's own KDoc for why.
     */
    fun request(zoom: Int, tileX: Int, tileY: Int, priority: TileRequestPriority, nowMillis: Long) {
        val key = TileKey(mapVersion, zoom, tileX, tileY)
        if (memoryCache.contains(key)) {
            memoryCache.touch(key)
            return
        }
        events.trySend(Event.Requested(key, priority, nowMillis))
    }

    private suspend fun processEvent(event: Event) {
        when (event) {
            is Event.Requested -> handleRequested(event.key, event.priority, event.nowMillis)
            is Event.FetchDone -> handleFetchDone(event.key, event.result)
            is Event.Sweep -> sweepStale(event.sweptAtMillis)
        }
    }

    private fun handleRequested(key: TileKey, priority: TileRequestPriority, nowMillis: Long) {
        wantedAtMillis[key] = nowMillis
        // Re-checked here (not just in `request`): the tile may have finished downloading between
        // the call to `request` and this event actually being processed.
        if (memoryCache.contains(key)) {
            memoryCache.touch(key)
            return
        }
        if (key in inFlight || key in queued) {
            scheduleSweep(nowMillis)
            return
        }
        if (priority != TileRequestPriority.VIEWPORT && key in failed) return

        queued += key
        when (priority) {
            TileRequestPriority.VIEWPORT -> viewportQueue.addLast(key)
            TileRequestPriority.PREFETCH_RING -> ringQueue.addLast(key)
            TileRequestPriority.NEXT_ZOOM -> nextZoomQueue.addLast(key)
        }
        drainQueues()
        scheduleSweep(nowMillis)
    }

    private fun drainQueues() {
        while (true) {
            if (!semaphore.tryAcquire()) return
            val key = pickNext()
            if (key == null) {
                semaphore.release()
                return
            }
            queued -= key
            inFlight[key] = scope.launch {
                var result: TileFetchResult? = null
                try {
                    result = fetchOnce(key)
                } finally {
                    // Thread-safe by construction (Semaphore/Channel.trySend) — safe to call here
                    // even though this `finally` may run on whatever thread `fetchOnce` happened to
                    // resume on, or during cancellation. See this class's own KDoc.
                    semaphore.release()
                    events.trySend(Event.FetchDone(key, result))
                }
            }
            _loadingKeys.value = inFlight.keys.toSet()
        }
    }

    private fun pickNext(): TileKey? = when {
        viewportQueue.isNotEmpty() -> viewportQueue.removeFirst()
        ringQueue.isNotEmpty() -> ringQueue.removeFirst()
        nextZoomQueue.isNotEmpty() -> nextZoomQueue.removeFirst()
        else -> null
    }

    /** No shared state touched here at all — see this class's own KDoc for why that's exactly what lets several of these run truly in parallel. */
    private suspend fun fetchOnce(key: TileKey): TileFetchResult {
        val url = TileUrl.build(tileUrlTemplate, key.zoom, key.tileX, key.tileY)
        var attempt = 0
        while (true) {
            when (val result = fetcher.fetch(url, tileSizePx)) {
                is TileFetchResult.Success -> return result
                is TileFetchResult.Failure -> {
                    attempt++
                    if (attempt >= maxRetryAttempts) return result
                    delay(retryBaseDelayMs * (1L shl (attempt - 1)))
                }
            }
        }
    }

    private fun handleFetchDone(key: TileKey, result: TileFetchResult?) {
        inFlight.remove(key)
        _loadingKeys.value = inFlight.keys.toSet()
        if (result != null) applyResult(key, result)
        drainQueues()
    }

    private fun applyResult(key: TileKey, result: TileFetchResult) {
        when (result) {
            is TileFetchResult.Success -> {
                memoryCache.put(key, result.bitmap, result.byteSize)
                _arrivedAtMillis.update { it + (key to Clock.System.now().toEpochMilliseconds()) }
                if (failed.remove(key)) _failedKeys.value = failed.toSet()
                when (result.source) {
                    TileDataSource.DISK -> metrics.recordDiskHit()
                    TileDataSource.NETWORK -> metrics.recordNetworkFetch()
                }
            }

            is TileFetchResult.Failure -> {
                failed += key
                _failedKeys.value = failed.toSet()
                metrics.recordFailure()
            }
        }
    }

    /**
     * Debounced: every [handleRequested] call cancels and re-arms this same delay, so it only
     * actually fires [viewportStabilityDelayMs] after the *last* tile was asked for in a burst (a
     * whole frame's worth of visible tiles, or a whole gesture's worth of frames) — never
     * mid-burst. This is what makes "cancel only after the viewport has been stable" true: a tile
     * that's still being requested every frame never reaches its cutoff. The scheduled coroutine
     * itself touches no shared state (just `delay` + [Channel.trySend]); [sweepStale] runs back on
     * [processEvents]' confined coroutine via [Event.Sweep], never directly from here.
     */
    private fun scheduleSweep(nowMillis: Long) {
        sweepJob?.cancel()
        sweepJob = scope.launch {
            delay(viewportStabilityDelayMs)
            events.trySend(Event.Sweep(nowMillis))
        }
    }

    private fun sweepStale(sweptAtMillis: Long) {
        val staleInFlight = inFlight.keys.filter { (wantedAtMillis[it] ?: 0L) <= sweptAtMillis }
        for (key in staleInFlight) {
            inFlight.remove(key)?.cancel()
            wantedAtMillis.remove(key)
        }
        if (staleInFlight.isNotEmpty()) _loadingKeys.value = inFlight.keys.toSet()

        val staleQueued = buildList {
            addAll(viewportQueue.filter { (wantedAtMillis[it] ?: 0L) <= sweptAtMillis })
            addAll(ringQueue.filter { (wantedAtMillis[it] ?: 0L) <= sweptAtMillis })
            addAll(nextZoomQueue.filter { (wantedAtMillis[it] ?: 0L) <= sweptAtMillis })
        }
        for (key in staleQueued) {
            viewportQueue.remove(key)
            ringQueue.remove(key)
            nextZoomQueue.remove(key)
            queued -= key
            wantedAtMillis.remove(key)
        }
    }
}

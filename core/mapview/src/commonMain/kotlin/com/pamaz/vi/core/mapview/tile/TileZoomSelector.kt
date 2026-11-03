package com.pamaz.vi.core.mapview.tile

import kotlin.math.round

/**
 * Picks the stable integer tile zoom level for a continuous camera zoom, with hysteresis so the
 * level doesn't flip back and forth when the camera sits near an integer boundary — a confirmed
 * cause of visible tile reload flicker (see the module README's "Tile pipeline" diagnosis): every
 * flip changes every visible tile's identity at once, tearing down and rebuilding the whole set.
 *
 * Stateful (remembers the last level it returned) on purpose: hysteresis is meaningless without
 * memory of where you were — "has the continuous zoom moved far enough past *this* level's own
 * boundary" only makes sense relative to a specific previous answer, not a stateless recomputation.
 */
internal class TileZoomSelector(private val hysteresis: Float = TileCacheConfig.ZOOM_HYSTERESIS) {
    private var currentLevel: Int? = null

    /**
     * [continuousZoom] clamped to `minZoom..maxZoom` first — overzoom beyond [maxZoom] still
     * resolves to [maxZoom]'s tiles (the sharpest available); enlarging them past that point is
     * [TileLayer]'s own concern, not this class's.
     */
    fun select(continuousZoom: Float, minZoom: Int, maxZoom: Int): Int {
        val clamped = continuousZoom.coerceIn(minZoom.toFloat(), maxZoom.toFloat())
        val rounded = round(clamped).toInt().coerceIn(minZoom, maxZoom)
        val previous = currentLevel
        val next = when {
            previous == null -> rounded
            previous == rounded -> previous
            else -> {
                // Only actually switches once the continuous zoom has moved `hysteresis` *past*
                // the boundary toward `rounded`, not merely crossed it.
                val movingUp = rounded > previous
                val threshold =
                    if (movingUp) previous + 0.5f + hysteresis else previous - 0.5f - hysteresis
                val crossed = if (movingUp) clamped >= threshold else clamped <= threshold
                if (crossed) rounded else previous
            }
        }
        currentLevel = next
        return next
    }

    /** Forgets the remembered level — e.g. when the map's own geometry changes and any previous level is no longer meaningful. */
    fun reset() {
        currentLevel = null
    }
}

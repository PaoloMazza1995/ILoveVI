package com.pamaz.vi.core.mapview.tile

/** Identifies one tile uniquely across map versions — a stale version's tiles never collide with a newer one's. */
data class TileKey(
    val mapVersion: Int,
    val zoom: Int,
    val tileX: Int,
    val tileY: Int,
)

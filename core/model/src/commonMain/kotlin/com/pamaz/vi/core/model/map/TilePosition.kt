package com.pamaz.vi.core.model.map

import com.pamaz.vi.core.model.NormalizedPoint

/**
 * Where a [NormalizedPoint] falls at a given integer zoom level: which tile ([tileX], [tileY])
 * and the pixel offset within that tile ([offsetX], [offsetY], each in `0..tileSize`).
 *
 * This is the pixel-space counterpart to [NormalizedPoint]: it depends on the [TileScheme] and
 * [zoom] level used to compute it, which is why it isn't itself a [NormalizedPoint] replacement —
 * converting back to one requires [TileScheme.normalizedPoint].
 */
data class TilePosition(
    val zoom: Int,
    val tileX: Int,
    val tileY: Int,
    val offsetX: Float,
    val offsetY: Float,
)

/** Converts [point] into the tile and in-tile pixel offset it falls into at integer [zoom]. */
fun TileScheme.tilePosition(point: NormalizedPoint, zoom: Int): TilePosition {
    val scale = scaleAt(zoom)
    val scaledWidth = widthPx * scale
    val scaledHeight = heightPx * scale

    val pixelX = point.x * scaledWidth
    val pixelY = point.y * scaledHeight

    val tileX = tileIndexFor(pixelX.toFloat(), tilesWide(zoom))
    val tileY = tileIndexFor(pixelY.toFloat(), tilesHigh(zoom))

    return TilePosition(
        zoom = zoom,
        tileX = tileX,
        tileY = tileY,
        offsetX = (pixelX - tileX * tileSize).toFloat(),
        offsetY = (pixelY - tileY * tileSize).toFloat(),
    )
}

/** Inverse of [TileScheme.tilePosition]: rebuilds the [NormalizedPoint] a [TilePosition] refers to. */
fun TileScheme.normalizedPoint(tile: TilePosition): NormalizedPoint {
    val scale = scaleAt(tile.zoom)
    val scaledWidth = widthPx * scale
    val scaledHeight = heightPx * scale

    val pixelX = tile.tileX * tileSize + tile.offsetX
    val pixelY = tile.tileY * tileSize + tile.offsetY

    return NormalizedPoint(
        x = (pixelX / scaledWidth).toFloat().coerceIn(0f, 1f),
        y = (pixelY / scaledHeight).toFloat().coerceIn(0f, 1f),
    )
}

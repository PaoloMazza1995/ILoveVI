package com.pamaz.vi.tools.tilegen.placeholder

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage

/**
 * Generates a clearly-fake stand-in map image for local development: a grid with coordinate
 * labels and an unmistakable watermark, never anything resembling a real or fan-made game map.
 *
 * This exists because the project must never download or embed an official GTA VI map (or any
 * fan-made recreation of one) — see `README.md`'s rights note. Real map art can only be sourced
 * once the project has the rights to use it; until then, every tilegen run in development should
 * point `--source` at an image produced by [generate].
 */
object PlaceholderMapImage {
    private val BACKGROUND = Color(0x1A, 0x1D, 0x22)
    private val GRID_LINE = Color(0x3A, 0x3F, 0x47)
    private val LABEL_COLOR = Color(0xC0, 0xC4, 0xCC)
    private val WATERMARK_COLOR = Color(0xFF, 0x3D, 0x6B, 70)

    const val DEFAULT_WIDTH_PX = 8192
    const val DEFAULT_HEIGHT_PX = 6144
    const val DEFAULT_GRID_SPACING_PX = 256

    fun generate(
        widthPx: Int = DEFAULT_WIDTH_PX,
        heightPx: Int = DEFAULT_HEIGHT_PX,
        gridSpacingPx: Int = DEFAULT_GRID_SPACING_PX,
    ): BufferedImage {
        require(widthPx > 0 && heightPx > 0) { "widthPx/heightPx must be positive, was ${widthPx}x$heightPx" }
        require(gridSpacingPx > 0) { "gridSpacingPx must be positive, was $gridSpacingPx" }

        val image = BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g.color = BACKGROUND
        g.fillRect(0, 0, widthPx, heightPx)

        g.color = GRID_LINE
        var x = 0
        while (x <= widthPx) {
            g.drawLine(x, 0, x, heightPx)
            x += gridSpacingPx
        }
        var y = 0
        while (y <= heightPx) {
            g.drawLine(0, y, widthPx, y)
            y += gridSpacingPx
        }

        g.color = LABEL_COLOR
        g.font = Font(Font.MONOSPACED, Font.PLAIN, 18)
        y = 0
        while (y <= heightPx) {
            x = 0
            while (x <= widthPx) {
                g.drawString("$x,$y", x + 6, y + 20)
                x += gridSpacingPx
            }
            y += gridSpacingPx
        }

        drawWatermark(g, widthPx, heightPx)

        g.dispose()
        return image
    }

    private fun drawWatermark(g: java.awt.Graphics2D, widthPx: Int, heightPx: Int) {
        val watermark = "PLACEHOLDER MAP - NOT GTA VI - DEV USE ONLY"
        g.color = WATERMARK_COLOR
        g.font = Font(Font.SANS_SERIF, Font.BOLD, (widthPx / 14).coerceAtLeast(12))

        val originalTransform = g.transform
        g.translate(widthPx / 2, heightPx / 2)
        g.rotate(-Math.atan2(heightPx.toDouble(), widthPx.toDouble()))
        val metrics = g.fontMetrics
        g.drawString(watermark, -metrics.stringWidth(watermark) / 2, 0)
        g.transform = originalTransform
    }
}

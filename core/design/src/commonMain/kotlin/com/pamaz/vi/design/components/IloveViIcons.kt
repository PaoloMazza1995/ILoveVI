package com.pamaz.vi.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors

/**
 * "Stroke" icons (outline only, never filled) used in the design system's components, transcribed
 * 1:1 from the SVG paths of the various preview.html files (no icon here is invented: every
 * coordinate matches the reference markup, reported in a comment above each function).
 * They all share the same native 24x24 viewBox and scale proportionally to the [size] parameter.
 */
private const val NATIVE_BOX = 24f

private fun DrawScope.iconStroke(strokeWidthPx: Float) =
    Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** preview.html ListRow: "M9 5l7 7-7 7" */
@Composable
fun IconChevronRight(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.inkFaint,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val path = Path().apply {
            moveTo(9f * s, 5f * s)
            lineTo(16f * s, 12f * s)
            lineTo(9f * s, 19f * s)
        }
        drawPath(path, color = tint, style = iconStroke(strokeWidth.toPx()))
    }
}

/** preview.html IconButton "Back": "M15 5l-7 7 7 7" */
@Composable
fun IconArrowBack(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val path = Path().apply {
            moveTo(15f * s, 5f * s)
            lineTo(8f * s, 12f * s)
            lineTo(15f * s, 19f * s)
        }
        drawPath(path, color = tint, style = iconStroke(strokeWidth.toPx()))
    }
}

/** preview.html IconButton "Zoom in": "M12 5v14M5 12h14" */
@Composable
fun IconZoomIn(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 22.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val path = Path().apply {
            moveTo(12f * s, 5f * s)
            lineTo(12f * s, 19f * s)
            moveTo(5f * s, 12f * s)
            lineTo(19f * s, 12f * s)
        }
        drawPath(path, color = tint, style = iconStroke(strokeWidth.toPx()))
    }
}

/** preview.html IconButton "Zoom out": "M5 12h14" (same glyph as Zoom in, minus the vertical stroke) */
@Composable
fun IconZoomOut(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 22.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val path = Path().apply {
            moveTo(5f * s, 12f * s)
            lineTo(19f * s, 12f * s)
        }
        drawPath(path, color = tint, style = iconStroke(strokeWidth.toPx()))
    }
}

/**
 * RSS glyph, NavBar's "News" icon: two quarter-circle arcs sharing a center 8dp up/left of the
 * dot (radius 9 and 16), plus a small filled dot — "M4 11a9 9 0 019 9" and "M4 4a16 16 0 0116 16"
 * with a dot circle at (5,19) r=1.2, viewBox 24x24, per the News destination spec.
 */
@Composable
fun IconRss(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        val center = Offset(4f * s, 20f * s)
        val smallR = 9f * s
        val bigR = 16f * s
        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - smallR, center.y - smallR),
            size = Size(smallR * 2, smallR * 2),
            style = stroke,
        )
        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - bigR, center.y - bigR),
            size = Size(bigR * 2, bigR * 2),
            style = stroke,
        )
        drawCircle(tint, radius = 1.2f * s, center = Offset(5f * s, 19f * s))
    }
}

/**
 * Two circular arrows ("refresh"), used by NewsScreen's refresh IconButton. News has no
 * preview.html mockup (new destination in this version): built from two opposing half-circle
 * arcs with small arrowhead chevrons, at the system's standard 24x24 viewBox and 2px stroke.
 */
@Composable
fun IconRefresh(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        val center = Offset(12f * s, 12f * s)
        val r = 7f * s
        val topLeft = Offset(center.x - r, center.y - r)
        val arcSize = Size(r * 2, r * 2)
        drawArc(tint, startAngle = 225f, sweepAngle = 180f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        drawArc(tint, startAngle = 45f, sweepAngle = 180f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        val arrow1 = Path().apply {
            moveTo(19.9f * s, 14.5f * s)
            lineTo(16.9f * s, 16.9f * s)
            lineTo(14.5f * s, 13.9f * s)
        }
        val arrow2 = Path().apply {
            moveTo(4.1f * s, 9.5f * s)
            lineTo(7.1f * s, 7.1f * s)
            lineTo(9.5f * s, 10.1f * s)
        }
        drawPath(arrow1, color = tint, style = stroke)
        drawPath(arrow2, color = tint, style = stroke)
    }
}

/**
 * Placeholder "image" glyph for NewsRow's thumbnail: a rounded frame, a small sun circle and a
 * mountain silhouette, shown in `ink-faint` while there is no image or it hasn't loaded yet.
 * Not from a preview.html mockup (News is new in this version): a conventional image-placeholder
 * glyph at the system's standard 24x24 viewBox and 2px stroke.
 */
@Composable
fun IconImagePlaceholder(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.inkFaint,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        drawRoundRect(
            color = tint,
            topLeft = Offset(3f * s, 4f * s),
            size = Size(18f * s, 16f * s),
            cornerRadius = CornerRadius(2f * s, 2f * s),
            style = stroke,
        )
        drawCircle(tint, radius = 1.5f * s, center = Offset(8.5f * s, 9.5f * s), style = stroke)
        val mountains = Path().apply {
            moveTo(4.5f * s, 16.5f * s)
            lineTo(9f * s, 12f * s)
            lineTo(12.5f * s, 15.5f * s)
            lineTo(16f * s, 11f * s)
            lineTo(19.5f * s, 15.5f * s)
        }
        drawPath(mountains, color = tint, style = stroke)
    }
}

/**
 * Weapons category crosshair: centered r6 circle + 4 ticks, used both in the
 * ListRow/Card thumbnail ("M12 2v5M12 17v5M2 12h5M17 12h5", viewBox 24x24) and as the
 * MapPin's inner icon (same path, expressed in coordinates relative to the center in the markup:
 * it is geometrically identical).
 */
@Composable
fun IconWeaponCrosshair(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        drawCircle(tint, radius = 6f * s, center = Offset(12f * s, 12f * s), style = stroke)
        val ticks = Path().apply {
            moveTo(12f * s, 2f * s); lineTo(12f * s, 7f * s)
            moveTo(12f * s, 17f * s); lineTo(12f * s, 22f * s)
            moveTo(2f * s, 12f * s); lineTo(7f * s, 12f * s)
            moveTo(17f * s, 12f * s); lineTo(22f * s, 12f * s)
        }
        drawPath(ticks, color = tint, style = stroke)
    }
}

/** preview.html ListRow (action "Add to playlist"): "M5 12.5l4.5 4.5L19 7.5" */
@Composable
fun IconCheck(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.inkOnBrand,
    size: Dp = 18.dp,
    strokeWidth: Dp = 3.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val path = Path().apply {
            moveTo(5f * s, 12.5f * s)
            lineTo(9.5f * s, 17f * s)
            lineTo(19f * s, 7.5f * s)
        }
        drawPath(path, color = tint, style = iconStroke(strokeWidth.toPx()))
    }
}

/**
 * Music note, used for the track thumbnail in ListRow and for NavBar's "Music" entry:
 * "M9 18V6l10-2v12" + two circles (6.5,18,r2.5) and (16.5,16,r2.5).
 */
@Composable
fun IconMusicNote(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.inkFaint,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        val stem = Path().apply {
            moveTo(9f * s, 18f * s)
            lineTo(9f * s, 6f * s)
            lineTo(19f * s, 4f * s)
            lineTo(19f * s, 16f * s)
        }
        drawPath(stem, color = tint, style = stroke)
        drawCircle(tint, radius = 2.5f * s, center = Offset(6.5f * s, 18f * s), style = stroke)
        drawCircle(tint, radius = 2.5f * s, center = Offset(16.5f * s, 16f * s), style = stroke)
    }
}

/**
 * Vehicle seen from the side, MapPin icon for the Vehicles category: "M-8 3v-3l2-4h12l2 4v3M-8 3h16M-6 3v3M6 3v3"
 * (coordinates relative to the center in the original markup), translated here into a 24x24 viewBox.
 */
@Composable
fun IconVehicle(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.inkOnBrand,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val body = Path().apply {
            moveTo(4f * s, 15f * s)
            lineTo(4f * s, 12f * s)
            lineTo(6f * s, 8f * s)
            lineTo(18f * s, 8f * s)
            lineTo(20f * s, 12f * s)
            lineTo(20f * s, 15f * s)
            moveTo(4f * s, 15f * s)
            lineTo(20f * s, 15f * s)
            moveTo(6f * s, 15f * s)
            lineTo(6f * s, 18f * s)
            moveTo(18f * s, 15f * s)
            lineTo(18f * s, 18f * s)
        }
        drawPath(body, color = tint, style = iconStroke(strokeWidth.toPx()))
    }
}

/**
 * 5-pointed star, MapPin icon for the Items category:
 * "M0 -8l3 6 6 1-4.5 4 1.5 6-6-3.5-6 3.5 1.5-6-4.5-4 6-1z" with transform="scale(0.8)" in the
 * original markup, here pre-applied and translated into a 24x24 viewBox (never filled: outline only).
 */
@Composable
fun IconStar(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.inkOnBrand,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val points = listOf(
            12f to 5.6f,
            14.4f to 10.4f,
            19.2f to 11.2f,
            15.6f to 14.4f,
            16.8f to 19.2f,
            12f to 16.4f,
            7.2f to 19.2f,
            8.4f to 14.4f,
            4.8f to 11.2f,
            9.6f to 10.4f,
        )
        val path = Path().apply {
            points.forEachIndexed { index, (x, y) ->
                if (index == 0) moveTo(x * s, y * s) else lineTo(x * s, y * s)
            }
            close()
        }
        drawPath(path, color = tint, style = iconStroke(strokeWidth.toPx()))
    }
}

/**
 * Teardrop pin, NavBar's "Map" icon:
 * "M12 21s-7-6.2-7-11a7 7 0 0114 0c0 4.8-7 11-7 11z" + circle (12,10,r2.5).
 */
@Composable
fun IconMapPinOutline(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val path = Path().apply {
            moveTo(12f * s, 21f * s)
            relativeCubicTo(0f, 0f, -7f * s, -6.2f * s, -7f * s, -11f * s)
            arcTo(
                rect = Rect(left = 5f * s, top = 3f * s, right = 19f * s, bottom = 17f * s),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            relativeCubicTo(0f, 4.8f * s, -7f * s, 11f * s, -7f * s, 11f * s)
            close()
        }
        drawPath(path, color = tint, style = iconStroke(strokeWidth.toPx()))
        drawCircle(tint, radius = 2.5f * s, center = Offset(12f * s, 10f * s), style = iconStroke(strokeWidth.toPx()))
    }
}

/** preview.html NavBar "Catalog": "M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01" */
@Composable
fun IconCatalogList(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        val lines = Path().apply {
            moveTo(8f * s, 6f * s); lineTo(20f * s, 6f * s)
            moveTo(8f * s, 12f * s); lineTo(20f * s, 12f * s)
            moveTo(8f * s, 18f * s); lineTo(20f * s, 18f * s)
        }
        drawPath(lines, color = tint, style = stroke)
        drawCircle(tint, radius = 1f * s, center = Offset(4f * s, 6f * s))
        drawCircle(tint, radius = 1f * s, center = Offset(4f * s, 12f * s))
        drawCircle(tint, radius = 1f * s, center = Offset(4f * s, 18f * s))
    }
}

/** preview.html NavBar "Settings": "M4 7h10M18 7h2M4 17h2M10 17h10" + two circles (knobs) */
@Composable
fun IconSettingsSliders(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        val tracks = Path().apply {
            moveTo(4f * s, 7f * s); lineTo(14f * s, 7f * s)
            moveTo(18f * s, 7f * s); lineTo(20f * s, 7f * s)
            moveTo(4f * s, 17f * s); lineTo(6f * s, 17f * s)
            moveTo(10f * s, 17f * s); lineTo(20f * s, 17f * s)
        }
        drawPath(tracks, color = tint, style = stroke)
        drawCircle(tint, radius = 2f * s, center = Offset(16f * s, 7f * s), style = stroke)
        drawCircle(tint, radius = 2f * s, center = Offset(8f * s, 17f * s), style = stroke)
    }
}

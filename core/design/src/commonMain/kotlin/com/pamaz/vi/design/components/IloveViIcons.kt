package com.pamaz.vi.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
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

// --- Map Editor toolbar/dialog icons -------------------------------------------------------
//
// Not from a preview.html mockup (the Map Editor Functional Analysis's own schematic names these
// by role, not by exact glyph): conventional icon shapes at this system's standard 24x24 viewBox
// and 2px stroke, matching every other hand-drawn icon in this file.

/** Place mode: a crosshair — four short ticks around a hollow center circle. */
@Composable
fun IconCrosshair(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        drawCircle(tint, radius = 3f * s, center = Offset(12f * s, 12f * s), style = stroke)
        val ticks = Path().apply {
            moveTo(12f * s, 2f * s); lineTo(12f * s, 6f * s)
            moveTo(12f * s, 18f * s); lineTo(12f * s, 22f * s)
            moveTo(2f * s, 12f * s); lineTo(6f * s, 12f * s)
            moveTo(18f * s, 12f * s); lineTo(22f * s, 12f * s)
        }
        drawPath(ticks, color = tint, style = stroke)
    }
}

/** Select mode: a pointer-cursor arrow. */
@Composable
fun IconCursorArrow(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val path = Path().apply {
            moveTo(5f * s, 3f * s)
            lineTo(5f * s, 19f * s)
            lineTo(9.5f * s, 15f * s)
            lineTo(13f * s, 21.5f * s)
            lineTo(15.5f * s, 20f * s)
            lineTo(12f * s, 13.5f * s)
            lineTo(18f * s, 13f * s)
            close()
        }
        drawPath(
            path,
            color = tint,
            style = Stroke(
                width = strokeWidth.toPx(),
                join = StrokeJoin.Round,
                cap = StrokeCap.Round
            ),
        )
    }
}

/** Undo: a counter-clockwise arrow (a left-pointing arrowhead on a curved stroke). */
@Composable
fun IconUndo(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(5f * s, 5f * s),
            size = Size(14f * s, 14f * s),
            style = stroke,
        )
        val arrowhead = Path().apply {
            moveTo(5f * s, 5f * s)
            lineTo(5f * s, 10f * s)
            lineTo(10f * s, 10f * s)
        }
        drawPath(arrowhead, color = tint, style = stroke)
    }
}

/** Redo: [IconUndo] mirrored horizontally. */
@Composable
fun IconRedo(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(5f * s, 5f * s),
            size = Size(14f * s, 14f * s),
            style = stroke,
        )
        val arrowhead = Path().apply {
            moveTo(19f * s, 5f * s)
            lineTo(19f * s, 10f * s)
            lineTo(14f * s, 10f * s)
        }
        drawPath(arrowhead, color = tint, style = stroke)
    }
}

/** "Saved" (show/hide database pins) toggle: an open eye. */
@Composable
fun IconEye(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        val lids = Path().apply {
            moveTo(2f * s, 12f * s)
            cubicTo(5f * s, 6f * s, 19f * s, 6f * s, 22f * s, 12f * s)
            cubicTo(19f * s, 18f * s, 5f * s, 18f * s, 2f * s, 12f * s)
            close()
        }
        drawPath(lids, color = tint, style = stroke)
        drawCircle(tint, radius = 3f * s, center = Offset(12f * s, 12f * s), style = stroke)
    }
}

/** Close/remove ("x") — the active-entry banner's close button, an attached image's remove button. */
@Composable
fun IconClose(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val path = Path().apply {
            moveTo(6f * s, 6f * s); lineTo(18f * s, 18f * s)
            moveTo(18f * s, 6f * s); lineTo(6f * s, 18f * s)
        }
        drawPath(path, color = tint, style = iconStroke(strokeWidth.toPx()))
    }
}

/** Warning triangle — the header's "N pins need an entry" chip, the non-admin banner. */
@Composable
fun IconWarningTriangle(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.warningYellow,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        val triangle = Path().apply {
            moveTo(12f * s, 3.5f * s)
            lineTo(21.5f * s, 20f * s)
            lineTo(2.5f * s, 20f * s)
            close()
        }
        drawPath(triangle, color = tint, style = stroke)
        val exclamation = Path().apply {
            moveTo(12f * s, 9.5f * s); lineTo(12f * s, 14f * s)
        }
        drawPath(exclamation, color = tint, style = stroke)
        drawCircle(tint, radius = 1f * s, center = Offset(12f * s, 17f * s))
    }
}

/**
 * Magnifying glass — the Map header's search action (MAP-01, Could-priority search). No exact
 * preview.html reference exists for the Map screen itself (the design canvas's "Map"/"Map
 * filters" screens have no exported SVG in this repo, unlike every other icon above); drawn here
 * in the same 24x24, stroke-only style as the rest of this file: a circle plus a short diagonal
 * handle, the universal search glyph.
 */
@Composable
fun IconSearch(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val stroke = iconStroke(strokeWidth.toPx())
        drawCircle(
            color = tint,
            radius = 6.5f * s,
            center = Offset(10f * s, 10f * s),
            style = stroke,
        )
        val handle = Path().apply {
            moveTo(15f * s, 15f * s)
            lineTo(21f * s, 21f * s)
        }
        drawPath(handle, color = tint, style = stroke)
    }
}

/**
 * Filter funnel — the Map header's "Filters" action (MAP-02/MAP-05). Same unconfirmed-asset
 * caveat as [IconSearch]: no design-canvas export exists for this glyph, so it's drawn here in
 * this file's usual stroke style as the standard funnel shape.
 */
@Composable
fun IconFilter(
    modifier: Modifier = Modifier,
    tint: Color = IloveViColors.ink,
    size: Dp = 24.dp,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier.size(size)) {
        val s = size.toPx() / NATIVE_BOX
        val path = Path().apply {
            moveTo(4f * s, 5f * s)
            lineTo(20f * s, 5f * s)
            lineTo(13.5f * s, 13f * s)
            lineTo(13.5f * s, 19f * s)
            lineTo(10.5f * s, 20.5f * s)
            lineTo(10.5f * s, 13f * s)
            close()
        }
        drawPath(path, color = tint, style = iconStroke(strokeWidth.toPx()))
    }
}

// --- Category/sub-type icon set (step I) -----------------------------------------------------
//
// Unlike the icons above (plain Canvas-drawing composables, each hardcoded to one specific use),
// these 13 are built as [ImageVector]s and exposed through an [IloveViIcon] enum: core:model's
// EntryCategory/WeaponType/VehicleType/ItemType need to be *mapped to a value* and passed around
// (see EntryIcons.kt), which a bare `@Composable fun IconXxx()` can't be — an enum constant backed
// by a plain-data ImageVector is the standard Compose shape for "pick an icon based on data". The
// 24x24 viewport, 2px stroke, round caps/joins and "never filled" rule are unchanged from the
// Canvas icons above and from design/tokens.json's Iconography section; only the underlying API
// differs, and only because the mapping in bullet 2 requires it.
//
// This first set was drawn by hand (not sourced from a designer mockup — none of these 13 exist
// in preview.html, since GTA VI's real weapon/vehicle/item icons aren't known pre-release) and can
// be freely replaced later: swapping an icon only ever means changing the path-data string below
// for that one [IloveViIcon] entry — the enum, the mapping functions and every call site stay
// untouched. See the design system README's "Iconography" section for how to add or replace one.

/** Builds a 24x24, stroke-only (never filled) [ImageVector] from one SVG path-data string. */
private fun strokeIconVector(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(pathData),
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ).build()

/**
 * The 13 category/sub-type icons: one per [com.pamaz.vi.core.model.WeaponType] (5),
 * [com.pamaz.vi.core.model.VehicleType] (5) and [com.pamaz.vi.core.model.ItemType] (2) value, plus
 * [BOX] for the generic Items *category* (there is no single "item" sub-type to fall back to).
 * [PISTOL]/[CAR]/[BOX] double as the Weapons/Vehicles/Items *category* icons — see
 * `EntryIcons.kt`'s `EntryCategory.icon()`.
 */
enum class IloveViIcon(val imageVector: ImageVector) {
    /** pistol: M3 7h17v4H3z M7 11l-1.5 8h4.5l1.5-8 M11.5 11v3H15 */
    PISTOL(strokeIconVector("Pistol", "M3 7h17v4H3z M7 11l-1.5 8h4.5l1.5-8 M11.5 11v3H15")),

    /** rifle: M6 10h9v3H6z M15 11h7 M6 10.5L2 9v5l4-.5 M10 13v4 M17 9v2 M8 13l-1 4 */
    RIFLE(
        strokeIconVector(
            "Rifle",
            "M6 10h9v3H6z M15 11h7 M6 10.5L2 9v5l4-.5 M10 13v4 M17 9v2 M8 13l-1 4"
        )
    ),

    /** machine_gun: M5 8h12v3.5H5z M17 9.75h5 M5 9L2 8v4l3-.5 M9 11.5h5v5H9z M7 11.5l-1 4 M18 8v-2 M16 14l2 4 M18 14l1.5 4 */
    MACHINE_GUN(
        strokeIconVector(
            "MachineGun",
            "M5 8h12v3.5H5z M17 9.75h5 M5 9L2 8v4l3-.5 M9 11.5h5v5H9z M7 11.5l-1 4 M18 8v-2 M16 14l2 4 M18 14l1.5 4",
        ),
    ),

    /** melee: M20 4c-5 1-9 4-11 9l2 2c5-2 8-6 9-11z M7.5 12.5l4 4 M9.5 14.5L4.5 19.5 */
    MELEE(
        strokeIconVector(
            "Melee",
            "M20 4c-5 1-9 4-11 9l2 2c5-2 8-6 9-11z M7.5 12.5l4 4 M9.5 14.5L4.5 19.5"
        )
    ),

    /** explosive: M12 8a6 6 0 11-.01 0 M10 8V5h4v3 M14 5l4-1 M18 4l1.5 2.5 */
    EXPLOSIVE(
        strokeIconVector(
            "Explosive",
            "M12 8a6 6 0 11-.01 0 M10 8V5h4v3 M14 5l4-1 M18 4l1.5 2.5"
        )
    ),

    /** car: M5 17H3v-5l2-1 2-4h10l2 4 2 1v5h-2 M9 17h6 M7 15a2 2 0 100 4 2 2 0 000-4 M17 15a2 2 0 100 4 2 2 0 000-4 */
    CAR(
        strokeIconVector(
            "Car",
            "M5 17H3v-5l2-1 2-4h10l2 4 2 1v5h-2 M9 17h6 M7 15a2 2 0 100 4 2 2 0 000-4 M17 15a2 2 0 100 4 2 2 0 000-4",
        ),
    ),

    /** motorcycle: M6 13.5a3.5 3.5 0 100 7 3.5 3.5 0 000-7 M18 13.5a3.5 3.5 0 100 7 3.5 3.5 0 000-7 M6 17l3.5-5h5.5l3 5 M9 12l1.5-3h4l1 3 M15.5 9l1.5-2.5h2.5 M16.5 12.5l1.5 4.5 */
    MOTORCYCLE(
        strokeIconVector(
            "Motorcycle",
            "M6 13.5a3.5 3.5 0 100 7 3.5 3.5 0 000-7 M18 13.5a3.5 3.5 0 100 7 3.5 3.5 0 000-7 M6 17l3.5-5h5.5l3 5 " +
                    "M9 12l1.5-3h4l1 3 M15.5 9l1.5-2.5h2.5 M16.5 12.5l1.5 4.5",
        ),
    ),

    /** boat: M3 15l2 5h14l2-5z M12 3v12 M12 4l6 9h-6 */
    BOAT(strokeIconVector("Boat", "M3 15l2 5h14l2-5z M12 3v12 M12 4l6 9h-6")),

    /** helicopter: M4 12a6 4 0 016-4h4a6 4 0 016 4 4 4 0 01-4 4H8a4 4 0 01-4-4z M3 5h18 M12 5v3 M20 11h3 M6 19h12 M9 16v3 M15 16v3 */
    HELICOPTER(
        strokeIconVector(
            "Helicopter",
            "M4 12a6 4 0 016-4h4a6 4 0 016 4 4 4 0 01-4 4H8a4 4 0 01-4-4z M3 5h18 M12 5v3 M20 11h3 M6 19h12 M9 16v3 M15 16v3",
        ),
    ),

    /** plane: M2 14l8-2V6a2 2 0 014 0v6l8 2v2l-8-1.5V19l2 1.5V22l-4-1-4 1v-1.5l2-1.5v-4.5L2 16z */
    PLANE(
        strokeIconVector(
            "Plane",
            "M2 14l8-2V6a2 2 0 014 0v6l8 2v2l-8-1.5V19l2 1.5V22l-4-1-4 1v-1.5l2-1.5v-4.5L2 16z"
        )
    ),

    /** ammo: M5 20v-7a2 2 0 014 0v7z M10 20v-9a2 2 0 014 0v9z M15 20v-7a2 2 0 014 0v7z */
    AMMO(
        strokeIconVector(
            "Ammo",
            "M5 20v-7a2 2 0 014 0v7z M10 20v-9a2 2 0 014 0v9z M15 20v-7a2 2 0 014 0v7z"
        )
    ),

    /** armor: M12 3l8 3v6c0 5-4 8-8 9-4-1-8-4-8-9V6z */
    ARMOR(strokeIconVector("Armor", "M12 3l8 3v6c0 5-4 8-8 9-4-1-8-4-8-9V6z")),

    /** box: M4 8l8-4 8 4v8l-8 4-8-4z M4 8l8 4 8-4 M12 12v8 */
    BOX(strokeIconVector("Box", "M4 8l8-4 8 4v8l-8 4-8-4z M4 8l8 4 8-4 M12 12v8")),
}

/**
 * Renders one [IloveViIcon] at [size] (default 24dp) in [tint]. [contentDescription] should stay
 * `null` (the default) when the icon sits next to a text label that already says the same thing
 * (Chips, buttons) — it is decorative there; pass the sub-type's name (from string resources, see
 * `strings.xml`) when the icon is the only identifier for what it represents (a map pin, a
 * thumbnail with no adjacent label).
 */
@Composable
fun IloveViIcon(
    icon: IloveViIcon,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = IloveViColors.ink,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = icon.imageVector,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

package com.pamaz.vi.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Radii from design/tokens.json → radius.tokens.
 *
 * `radiusPill` (999px) is expressed as RoundedCornerShape(percent = 50) instead of the
 * literal value 999.dp: in Compose the corner radius is clamped to half of the shorter side
 * anyway, so the visual result is identical to 999px but the shape stays correct even
 * if the component's height changes (e.g. Chip/Badge with different content).
 */
object IloveViShapes {
    val radiusXs: Shape = RoundedCornerShape(8.dp)
    val radiusSm: Shape = RoundedCornerShape(12.dp)
    val radiusMd: Shape = RoundedCornerShape(16.dp)
    val radiusLg: Shape = RoundedCornerShape(20.dp)
    val radiusPill: Shape = RoundedCornerShape(percent = 50)
}

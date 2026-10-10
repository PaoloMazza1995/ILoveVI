package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViTheme

/**
 * design/components/IconButton/README.md "map control cluster" (v1.1): the map has no user
 * position in this version, so the former 3-button row (back / zoom in / center on player) is
 * replaced by this 2-button, zoom-only cluster. Each [IloveViIconButton] is 44dp with 8dp gap,
 * the whole cluster sits on a `bg-800` surface with a 1dp `border` outline and 12dp corners
 * (`radius-sm`) — deliberately not `radius-xs`, which is what the standalone OnMap IconButton
 * variant uses elsewhere.
 */
@Composable
fun IloveViMapControls(
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(IloveViShapes.radiusSm)
            .background(IloveViColors.bg800, IloveViShapes.radiusSm)
            .border(1.dp, IloveViColors.border, IloveViShapes.radiusSm),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IloveViIconButton(contentDescription = "Zoom in", onClick = onZoomIn) { tint -> IconZoomIn(tint = tint) }
        IloveViIconButton(contentDescription = "Zoom out", onClick = onZoomOut) { tint -> IconZoomOut(tint = tint) }
    }
}

@Preview
@Composable
private fun IloveViMapControlsPreview() {
    IloveViTheme {
        IloveViMapControls(onZoomIn = {}, onZoomOut = {})
    }
}

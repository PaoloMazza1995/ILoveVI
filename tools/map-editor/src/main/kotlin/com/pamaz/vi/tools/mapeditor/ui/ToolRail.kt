package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.components.IconCrosshair
import com.pamaz.vi.design.components.IconCursorArrow
import com.pamaz.vi.design.components.IconEye
import com.pamaz.vi.design.components.IconRedo
import com.pamaz.vi.design.components.IconUndo
import com.pamaz.vi.design.components.IloveViRailButton
import com.pamaz.vi.tools.mapeditor.model.EditMode

/** §4 region 2, 72dp wide: Place, Select, Undo, Redo, and the "Saved" (show/hide database pins) toggle. */
@Composable
fun ToolRail(
    mode: EditMode,
    canUndo: Boolean,
    canRedo: Boolean,
    showDbPins: Boolean,
    onModeChange: (EditMode) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleShowDbPins: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = IloveViColors.border
    Column(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(IloveViColors.bg800)
            .drawBehind {
                drawLine(
                    borderColor,
                    Offset(size.width, 0f),
                    Offset(size.width, size.height),
                    1.dp.toPx()
                )
            }
            .padding(IloveViSpacing.space2),
        verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space2),
    ) {
        IloveViRailButton(
            label = "Place",
            active = mode == EditMode.PLACE,
            onClick = { onModeChange(EditMode.PLACE) }) { tint ->
            IconCrosshair(tint = tint)
        }
        IloveViRailButton(
            label = "Select",
            active = mode == EditMode.SELECT,
            onClick = { onModeChange(EditMode.SELECT) }) { tint ->
            IconCursorArrow(tint = tint)
        }
        RailDivider()
        IloveViRailButton(
            label = "Undo",
            active = false,
            enabled = canUndo,
            onClick = onUndo
        ) { tint -> IconUndo(tint = tint) }
        IloveViRailButton(
            label = "Redo",
            active = false,
            enabled = canRedo,
            onClick = onRedo
        ) { tint -> IconRedo(tint = tint) }
        RailDivider()
        IloveViRailButton(
            label = "Saved",
            active = showDbPins,
            onClick = onToggleShowDbPins
        ) { tint ->
            IconEye(tint = tint)
        }
    }
}

@Composable
private fun RailDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(IloveViColors.border),
    )
}

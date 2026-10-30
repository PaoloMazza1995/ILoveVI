package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.tools.mapeditor.state.EditorUiState

/** §4 region 8, 32dp: autosave status, draft counts, cursor coordinates (EDT-12). */
@Composable
fun StatusBar(
    uiState: EditorUiState,
    cursorPosition: NormalizedPoint?,
    modifier: Modifier = Modifier
) {
    val borderColor = IloveViColors.border
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(IloveViColors.bg800)
            .drawBehind {
                drawLine(borderColor, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            }
            .padding(horizontal = IloveViSpacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space4),
    ) {
        Text(
            uiState.autosaveStatus.ifBlank { "Draft not saved yet" },
            color = IloveViColors.inkFaint,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "${uiState.draftPins.size} pins · ${uiState.draftEntries.size} new entries",
            color = IloveViColors.inkFaint,
            style = MaterialTheme.typography.bodySmall,
        )
        Box(Modifier.weight(1f))
        cursorPosition?.let {
            Text(
                "x ${"%.3f".format(it.x)}  y ${"%.3f".format(it.y)}",
                color = IloveViColors.inkFaint,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

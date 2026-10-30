package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.tools.mapeditor.state.EditorState
import com.pamaz.vi.tools.mapeditor.state.EditorUiState

private enum class RightTab { INSPECTOR, DRAFT }

/** §4 region 6/7, 392dp wide: Inspector / Draft (n) tabs. */
@Composable
fun RightPanel(
    uiState: EditorUiState,
    editorState: EditorState,
    onPinPlaced: String?,
    onConsumePinPlaced: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(RightTab.INSPECTOR) }

    // EDT-31: the Inspector opens for a newly-placed pin, unless an entry was active (rapid "place
    // another" clicks shouldn't yank focus away from whatever tab the admin is already looking at).
    LaunchedEffect(onPinPlaced) {
        if (onPinPlaced != null) {
            if (uiState.activeEntry == null) tab = RightTab.INSPECTOR
            onConsumePinPlaced()
        }
    }

    val borderColor = IloveViColors.border
    Column(
        modifier = modifier
            .width(392.dp)
            .fillMaxHeight()
            .background(IloveViColors.bg800)
            .drawBehind {
                drawLine(borderColor, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
            },
    ) {
        Row(Modifier.fillMaxWidth().height(48.dp)) {
            TabHeader(
                label = "Inspector",
                active = tab == RightTab.INSPECTOR,
                onClick = { tab = RightTab.INSPECTOR },
                modifier = Modifier.weight(1f),
            )
            TabHeader(
                label = "Draft (${uiState.draftPins.size})",
                active = tab == RightTab.DRAFT,
                onClick = { tab = RightTab.DRAFT },
                modifier = Modifier.weight(1f),
            )
        }
        when (tab) {
            RightTab.INSPECTOR -> InspectorPanel(uiState, editorState, Modifier.weight(1f))
            RightTab.DRAFT -> DraftPanel(
                uiState = uiState,
                onSelectPin = { pinId -> editorState.select(pinId); tab = RightTab.INSPECTOR },
                onDiscardDraft = editorState::requestDiscardDraft,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TabHeader(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick)
            .padding(horizontal = IloveViSpacing.space3),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            color = if (active) IloveViColors.ink else IloveViColors.inkMuted,
            style = if (active) MaterialTheme.typography.displaySmall else MaterialTheme.typography.bodyLarge,
        )
    }
}

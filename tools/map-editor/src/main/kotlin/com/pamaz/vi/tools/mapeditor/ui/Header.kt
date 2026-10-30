package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.components.IconWarningTriangle
import com.pamaz.vi.design.components.IloveViButton
import com.pamaz.vi.tools.mapeditor.state.EditorUiState

/**
 * §4 region 1, 56dp: wordmark, the "Map" menu (§1: "Always available"), map version chip,
 * incomplete-pins warning, user chip, Commit button.
 */
@Composable
fun Header(
    uiState: EditorUiState,
    onCommitClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onReplaceMapClick: () -> Unit,
    onMapInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = IloveViColors.border
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(IloveViColors.bg800)
            .drawBehind {
                drawLine(
                    borderColor,
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    1.dp.toPx()
                )
            }
            .padding(horizontal = IloveViSpacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space4),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ILOVEVI",
                color = IloveViColors.brandPink,
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                " MAP EDITOR",
                color = IloveViColors.ink,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        MapMenu(onReplaceMapClick = onReplaceMapClick, onMapInfoClick = onMapInfoClick)

        uiState.mapConfig?.let { config ->
            Text(
                "Map v${config.mapVersion}  ${config.widthPx} x ${config.heightPx}",
                color = IloveViColors.inkMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (uiState.incompleteDraftPinIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clip(IloveViShapes.radiusPill)
                    .background(IloveViColors.warningYellow, IloveViShapes.radiusPill)
                    .padding(horizontal = IloveViSpacing.space3, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space1),
            ) {
                IconWarningTriangle(tint = IloveViColors.inkOnBrand, size = 16.dp)
                Text(
                    "${uiState.incompleteDraftPinIds.size} pin(s) need an entry",
                    color = IloveViColors.inkOnBrand,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Box(Modifier.weight(1f))

        uiState.session?.let { session ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    session.email,
                    color = IloveViColors.inkMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Sign out",
                    color = IloveViColors.inkFaint,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(start = IloveViSpacing.space2)
                        .clip(IloveViShapes.radiusXs)
                        .clickable(onClick = onSignOutClick)
                        .padding(4.dp),
                )
            }
        }

        Box(Modifier.width(160.dp)) {
            IloveViButton(
                text = "COMMIT ${uiState.draftPins.size}",
                onClick = onCommitClick,
                enabled = uiState.isCommittable,
                useDisplayFont = true,
            )
        }
    }
}

/** §1: "Always available: a 'Map' menu in the header with 'Replace map...' and 'Map info'." */
@Composable
private fun MapMenu(onReplaceMapClick: () -> Unit, onMapInfoClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            "Map ▾",
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .clip(IloveViShapes.radiusXs)
                .clickable { expanded = true }
                .padding(horizontal = IloveViSpacing.space2, vertical = 6.dp),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Replace map...") },
                onClick = { expanded = false; onReplaceMapClick() },
            )
            DropdownMenuItem(
                text = { Text("Map info") },
                onClick = { expanded = false; onMapInfoClick() },
            )
        }
    }
}

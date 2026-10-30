package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.design.components.IloveViButton
import com.pamaz.vi.design.components.IloveViButtonVariant
import com.pamaz.vi.design.components.IloveViEmptyStateCard
import com.pamaz.vi.design.components.IloveViListRow
import com.pamaz.vi.design.components.IloveViListRowTrailing
import com.pamaz.vi.design.components.IloveViMapPin
import com.pamaz.vi.tools.mapeditor.model.DraftEntry
import com.pamaz.vi.tools.mapeditor.model.DraftPin
import com.pamaz.vi.tools.mapeditor.model.EntrySource
import com.pamaz.vi.tools.mapeditor.model.rawEntryId
import com.pamaz.vi.tools.mapeditor.state.EditorUiState

/** §5.7/EDT-60..62: per-category counts, the grouped pin list, and the two-click Discard button. */
@Composable
fun DraftPanel(
    uiState: EditorUiState,
    onSelectPin: (String) -> Unit,
    onDiscardDraft: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(IloveViSpacing.space4)) {
        if (!uiState.hasDraftChanges) {
            IloveViEmptyStateCard(
                title = "Draft is empty",
                body = "Switch to Place mode and click the map to start adding pins.",
            )
            return@Column
        }

        val grouped = uiState.draftPins.groupBy { it.category }
        CategoryCounts(grouped)

        LazyColumn(Modifier.weight(1f)) {
            EntryCategory.entries.forEach { category ->
                val pins = grouped[category].orEmpty()
                if (pins.isEmpty()) return@forEach
                items(pins) { pin -> DraftPinRow(pin, uiState, onClick = { onSelectPin(pin.id) }) }
            }
        }

        IloveViButton(
            text = if (uiState.discardArmed) "Click again to discard the whole draft" else "Discard draft",
            onClick = onDiscardDraft,
            variant = IloveViButtonVariant.Secondary,
            modifier = Modifier.padding(top = IloveViSpacing.space2),
        )
    }
}

@Composable
private fun CategoryCounts(grouped: Map<EntryCategory, List<DraftPin>>) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = IloveViSpacing.space3),
        horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space3),
    ) {
        EntryCategory.entries.forEach { category ->
            val count = grouped[category]?.size ?: 0
            Text(
                "${category.name.lowercase().replaceFirstChar { it.uppercase() }}: $count",
                color = IloveViColors.inkMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DraftPinRow(pin: DraftPin, uiState: EditorUiState, onClick: () -> Unit) {
    val isComplete = pin.isComplete(uiState.draftEntriesById)
    IloveViListRow(
        title = entryLabelFor(pin, uiState),
        subtitle = "${pin.subType.lowercase()} · x ${"%.3f".format(pin.position.x)} y ${
            "%.3f".format(
                pin.position.y
            )
        }",
        titleColor = if (isComplete) IloveViColors.ink else IloveViColors.warningYellow,
        trailing = IloveViListRowTrailing.Chevron,
        onClick = onClick,
        thumbnail = {
            IloveViMapPin(
                category = pin.category.toPinCategory(),
                icon = iconFor(pin.category, pin.subType),
                diameter = 24.dp,
                incomplete = !isComplete,
            )
        },
    )
}

private fun entryLabelFor(pin: DraftPin, uiState: EditorUiState): String {
    val source = pin.entry ?: return "No entry yet"
    return when (source) {
        is EntrySource.Db -> uiState.dbEntries.find { it.entryRef.rawEntryId() == source.id }?.name
            ?: source.id

        is EntrySource.Draft -> uiState.draftEntriesById[source.id]?.name?.takeIf { it.isNotBlank() }
            ?: "No entry yet"
    }
}

@Preview
@Composable
private fun DraftPanelEmptyPreview() {
    IloveViTheme {
        DraftPanel(uiState = EditorUiState(), onSelectPin = {}, onDiscardDraft = {})
    }
}

@Preview
@Composable
private fun DraftPanelWithIncompletePinPreview() {
    val complete = DraftPin(
        id = "pin-1",
        position = NormalizedPoint(0.4f, 0.4f),
        category = EntryCategory.WEAPON,
        subType = "PISTOL",
        entry = EntrySource.Draft("entry-1"),
    )
    val incomplete = DraftPin(
        id = "pin-2",
        position = NormalizedPoint(0.6f, 0.6f),
        category = EntryCategory.VEHICLE,
        subType = "CAR",
        entry = null,
    )
    val uiState = EditorUiState(
        draftPins = listOf(complete, incomplete),
        draftEntries = listOf(
            DraftEntry(
                id = "entry-1",
                category = EntryCategory.WEAPON,
                subType = "PISTOL",
                name = "Falcon .45"
            ),
        ),
    )
    IloveViTheme {
        DraftPanel(uiState = uiState, onSelectPin = {}, onDiscardDraft = {})
    }
}

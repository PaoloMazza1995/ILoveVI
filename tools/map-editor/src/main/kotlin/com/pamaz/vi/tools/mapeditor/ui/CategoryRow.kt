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
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.components.IconClose
import com.pamaz.vi.design.components.IloveViChip
import com.pamaz.vi.design.components.IloveViChipGroup
import com.pamaz.vi.design.components.IloveViIconButton
import com.pamaz.vi.tools.mapeditor.model.subTypes

private fun EntryCategory.label(): String = toDesignCategory().label

/**
 * §4 region 3, 64dp: category chips + sub-type chips for the pins about to be placed (EDT-21),
 * replaced by the active-entry banner when "Place another pin of this entry" is armed (EDT-32).
 */
@Composable
fun CategoryRow(
    activeCategory: EntryCategory,
    activeSubType: String,
    activeEntryLabel: String?,
    onCategoryChange: (EntryCategory) -> Unit,
    onSubTypeChange: (String) -> Unit,
    onClearActiveEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = IloveViColors.border
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
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
        if (activeEntryLabel != null) {
            Row(
                modifier = Modifier
                    .background(IloveViColors.bg900, IloveViShapes.radiusSm)
                    .padding(horizontal = IloveViSpacing.space3, vertical = IloveViSpacing.space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space2),
            ) {
                Text(
                    "Placing pins for:",
                    color = IloveViColors.inkMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    activeEntryLabel,
                    color = IloveViColors.ink,
                    style = MaterialTheme.typography.titleMedium
                )
                IloveViIconButton(
                    contentDescription = "Stop placing pins for $activeEntryLabel",
                    onClick = onClearActiveEntry
                ) { tint ->
                    IconClose(tint = tint, size = 16.dp)
                }
            }
        } else {
            IloveViChipGroup {
                EntryCategory.entries.forEach { category ->
                    IloveViChip(
                        label = category.label(),
                        active = category == activeCategory,
                        onClick = { onCategoryChange(category) },
                        category = category.toDesignCategory(),
                    )
                }
            }
            IloveViChipGroup {
                activeCategory.subTypes().forEach { subType ->
                    IloveViChip(
                        label = subType.lowercase().replace('_', ' ')
                            .replaceFirstChar { it.uppercase() },
                        active = subType == activeSubType,
                        onClick = { onSubTypeChange(subType) },
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth()) {
            Text(
                if (activeEntryLabel != null) "Click the map to add another pin" else "Click the map to add a pin",
                color = IloveViColors.inkFaint,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

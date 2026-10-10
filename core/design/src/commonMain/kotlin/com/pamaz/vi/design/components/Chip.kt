package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.pamaz.vi.design.IloveViCategory
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme

/**
 * Pill filter, always in a horizontal group (see [IloveViChipGroup]).
 * design/components/Chip/README.md: fixed height 44dp, radius-pill radius, body-md text (-> titleMedium).
 *
 * Inactive category variant: the README only specifies the background (`bg-800`), not the text
 * color; preview.html doesn't show this state. Here the category color is used as text
 * color even when inactive (to keep the visual tie to the category), a choice not
 * confirmed by the design system — to be verified with design.
 */
@Composable
fun IloveViChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    category: IloveViCategory? = null,
) {
    val background = when {
        category != null && active -> category.color
        category != null -> IloveViColors.bg800
        active -> IloveViColors.ink
        else -> IloveViColors.bg800
    }
    val textColor = when {
        category != null && active -> IloveViColors.inkOnBrand
        category != null -> category.color
        active -> IloveViColors.inkOnBrand
        else -> IloveViColors.ink
    }

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(IloveViShapes.radiusPill)
            .background(background, IloveViShapes.radiusPill)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = textColor, style = MaterialTheme.typography.titleMedium)
    }
}

/** Horizontal scrollable group of Chips, as required by the README (never wraps). */
@Composable
fun IloveViChipGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space2),
    ) { content() }
}

@Preview
@Composable
private fun IloveViChipGenericPreview() {
    IloveViTheme {
        IloveViChipGroup {
            IloveViChip(label = "All", active = true, onClick = {})
            IloveViChip(label = "Radio station", active = false, onClick = {})
        }
    }
}

@Preview
@Composable
private fun IloveViChipCategoryPreview() {
    IloveViTheme {
        IloveViChipGroup {
            IloveViChip(label = "Weapons", active = true, onClick = {}, category = IloveViCategory.Weapons)
            IloveViChip(label = "Vehicles", active = true, onClick = {}, category = IloveViCategory.Vehicles)
            IloveViChip(label = "Items", active = true, onClick = {}, category = IloveViCategory.Items)
        }
    }
}

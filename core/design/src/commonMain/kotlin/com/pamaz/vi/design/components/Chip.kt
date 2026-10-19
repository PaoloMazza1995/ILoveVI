package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
 *
 * [icon] is an optional leading glyph (the category icon for a category chip, the sub-type icon
 * for a Filters sub-type chip) — a small (18dp) icon fits inside the fixed 44dp height without
 * growing the chip, matching [IloveViShapes.radiusPill]'s existing vertical padding. It's
 * decorative (`contentDescription = null`): [label] already says the same thing out loud.
 */
@Composable
fun IloveViChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    category: IloveViCategory? = null,
    icon: IloveViIcon? = null,
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
        if (icon != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space1),
            ) {
                IloveViIcon(icon = icon, size = 18.dp, tint = textColor)
                Text(text = label, color = textColor, style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Text(text = label, color = textColor, style = MaterialTheme.typography.titleMedium)
        }
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

/** Category chips with a leading icon (Map/Catalog tab filters), and Filters sub-type chips. */
@Preview
@Composable
private fun IloveViChipWithIconPreview() {
    IloveViTheme {
        Column {
            IloveViChipGroup {
                IloveViChip(
                    label = "Weapons", active = true, onClick = {},
                    category = IloveViCategory.Weapons, icon = IloveViIcon.PISTOL,
                )
                IloveViChip(
                    label = "Vehicles", active = true, onClick = {},
                    category = IloveViCategory.Vehicles, icon = IloveViIcon.CAR,
                )
                IloveViChip(
                    label = "Items", active = true, onClick = {},
                    category = IloveViCategory.Items, icon = IloveViIcon.BOX,
                )
            }
            IloveViChipGroup {
                IloveViChip(label = "Rifle", active = false, onClick = {}, icon = IloveViIcon.RIFLE)
                IloveViChip(
                    label = "Motorcycle",
                    active = false,
                    onClick = {},
                    icon = IloveViIcon.MOTORCYCLE
                )
                IloveViChip(label = "Armor", active = false, onClick = {}, icon = IloveViIcon.ARMOR)
            }
        }
    }
}

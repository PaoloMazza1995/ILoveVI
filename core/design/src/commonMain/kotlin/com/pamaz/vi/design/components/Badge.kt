package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pamaz.vi.design.IloveViCategory
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme

/**
 * design/components/Badge/README.md: non-tappable label, never a Chip. Always
 * radius-pill, 12px horizontal padding (= space-3, a reused token). The vertical padding (6px
 * in preview.html) doesn't correspond to any spacing token (between space-1=4 and space-2=8): a
 * literal value taken from preview.html.
 */
@Composable
fun IloveViCategoryBadge(category: IloveViCategory, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(IloveViShapes.radiusPill)
            .background(IloveViColors.bg800, IloveViShapes.radiusPill)
            .padding(horizontal = IloveViSpacing.space3, vertical = 6.dp),
    ) {
        Text(
            text = category.label.uppercase(),
            color = category.color,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Status badge: solid background in the color that communicates the outcome, only to confirm an action that already happened. */
@Composable
fun IloveViStatusBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(IloveViShapes.radiusPill)
            .background(color, IloveViShapes.radiusPill)
            .padding(horizontal = IloveViSpacing.space3, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = IloveViColors.inkOnBrand,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
        )
    }
}

@Preview
@Composable
private fun IloveViCategoryBadgePreview() {
    IloveViTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space2)) {
            IloveViCategoryBadge(category = IloveViCategory.Weapons)
            IloveViCategoryBadge(category = IloveViCategory.Vehicles)
            IloveViCategoryBadge(category = IloveViCategory.Items)
        }
    }
}

@Preview
@Composable
private fun IloveViStatusBadgePreview() {
    IloveViTheme {
        IloveViStatusBadge(text = "Added", color = IloveViColors.brandTeal)
    }
}

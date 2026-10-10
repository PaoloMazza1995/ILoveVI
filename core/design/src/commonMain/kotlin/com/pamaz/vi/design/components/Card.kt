package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViCategory
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme

/**
 * design/components/Card/README.md: container for related information, always bg-800 (or
 * a featured gradient) with a 1px border, never shadows.
 *
 * `#3A1B6B` in the "Featured" variant's gradient is a literal value from preview.html,
 * absent from design/tokens.json: it doesn't match any token and is used here as-is, flagged
 * because it should ideally be promoted to a token if the gradient is reused elsewhere.
 */
@Composable
fun IloveViFeaturedCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(IloveViShapes.radiusLg)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF3A1B6B), IloveViColors.bg800),
                ),
                IloveViShapes.radiusLg,
            )
            .border(1.dp, IloveViColors.border, IloveViShapes.radiusLg)
            .padding(IloveViSpacing.space5),
    ) {
        Text(
            text = label.uppercase(),
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = value,
            color = IloveViColors.ink,
            style = MaterialTheme.typography.displaySmall,
        )
    }
}

/**
 * Compact variant: category icon (at most one use of the color per card) + two lines of
 * text, like the card for a selected pin on the map.
 */
@Composable
fun IloveViCompactCard(
    title: String,
    subtitle: String,
    category: IloveViCategory,
    modifier: Modifier = Modifier,
    icon: @Composable (tint: Color) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(IloveViShapes.radiusMd)
            .background(IloveViColors.bg800, IloveViShapes.radiusMd)
            .border(1.dp, IloveViColors.border, IloveViShapes.radiusMd)
            .padding(IloveViSpacing.space4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(category.color, CircleShape),
            contentAlignment = Alignment.Center,
        ) { icon(IloveViColors.inkOnBrand) }

        Column(modifier = Modifier.padding(start = IloveViSpacing.space3)) {
            Text(
                text = title,
                color = IloveViColors.ink,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                color = IloveViColors.inkMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
private fun IloveViFeaturedCardPreview() {
    IloveViTheme {
        IloveViFeaturedCard(
            label = "Map",
            value = "7 items nearby",
            modifier = Modifier.width(300.dp),
        )
    }
}

@Preview
@Composable
private fun IloveViCompactCardPreview() {
    IloveViTheme {
        IloveViCompactCard(
            title = "Equipped pistol",
            subtitle = "Weapons · Vinewood",
            category = IloveViCategory.Weapons,
            modifier = Modifier.width(300.dp),
            icon = { tint -> IconWeaponCrosshair(tint = tint, size = 24.dp) },
        )
    }
}

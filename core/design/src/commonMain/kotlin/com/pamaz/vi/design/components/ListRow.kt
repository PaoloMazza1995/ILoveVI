package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme

/**
 * design/components/ListRow/README.md: thumbnail + text + action/indicator.
 * A row is EITHER a link OR has an action, never both: [trailing] enforces this choice at the
 * type level (a Chevron indicator implies a link, an Action implies a button, never both together).
 */
sealed interface IloveViListRowTrailing {
    data object Chevron : IloveViListRowTrailing
    data class Action(
        val contentDescription: String,
        val onClick: () -> Unit,
        val icon: @Composable () -> Unit,
    ) : IloveViListRowTrailing
}

/**
 * The thumbnail's height and radius aren't the same for every row: the README specifies 56dp
 * (radius-xs) for the catalog and 48dp (10dp radius, a literal value not present among the
 * radius-* tokens) for music playlist rows. The minimum row height stated in the
 * README (64/72px) differs slightly from the one used in preview.html for the 48dp row
 * (68px instead of 64px): preview.html is followed here, as it reports the actual values.
 */
@Composable
fun IloveViListRow(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    thumbnailSize: Dp = 56.dp,
    thumbnailShape: Shape = IloveViShapes.radiusXs,
    trailing: IloveViListRowTrailing = IloveViListRowTrailing.Chevron,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    thumbnail: @Composable () -> Unit,
) {
    val rowHeight = if (thumbnailSize >= 56.dp) 72.dp else 68.dp
    val dividerColor = IloveViColors.border

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight)
            .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base }
            .then(
                if (showDivider) {
                    Modifier.drawBehind {
                        drawLine(
                            color = dividerColor,
                            start = androidx.compose.ui.geometry.Offset(0f, size.height),
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(thumbnailSize)
                .clip(thumbnailShape)
                .background(IloveViColors.bg800, thumbnailShape),
            contentAlignment = Alignment.Center,
        ) { thumbnail() }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = IloveViSpacing.space3),
        ) {
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

        when (trailing) {
            is IloveViListRowTrailing.Chevron -> IconChevronRight(tint = IloveViColors.inkFaint)
            is IloveViListRowTrailing.Action -> IloveViIconButton(
                contentDescription = trailing.contentDescription,
                onClick = trailing.onClick,
            ) { trailing.icon() }
        }
    }
}

@Preview
@Composable
private fun IloveViListRowCatalogPreview() {
    IloveViTheme {
        IloveViListRow(
            title = "Equipped pistol",
            subtitle = "Weapons · Vinewood",
            trailing = IloveViListRowTrailing.Chevron,
            onClick = {},
            thumbnail = { IconWeaponCrosshair(tint = IloveViColors.brandPink, size = 26.dp) },
        )
    }
}

@Preview
@Composable
private fun IloveViListRowCompilationPreview() {
    IloveViTheme {
        IloveViListRow(
            title = "In the Wind",
            subtitle = "Radio Vice · 3:24",
            thumbnailSize = 48.dp,
            thumbnailShape = RoundedCornerShape(10.dp),
            trailing = IloveViListRowTrailing.Action(
                contentDescription = "Add to playlist",
                onClick = {},
                icon = {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(IloveViColors.brandTeal, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { IconCheck(tint = IloveViColors.inkOnBrand) }
                },
            ),
            thumbnail = { IconMusicNote(tint = IloveViColors.inkFaint, size = 22.dp) },
        )
    }
}

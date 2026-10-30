package com.pamaz.vi.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme

/**
 * A dashed-border placeholder card for an empty/unselected state — distinct from [IloveViFeaturedCard]/
 * [IloveViCompactCard] (`Card.kt`), which always hold *something*; this is specifically for "there
 * is nothing here yet, here's what to do about it." Introduced for the Map Editor Functional
 * Analysis's edge-case table (§6: "No pin selected," "Empty draft") but generic enough for any
 * screen's empty state.
 *
 * The dash itself is drawn with `Modifier.drawBehind` (`drawRoundRect` + a dashed `PathEffect`)
 * since Compose has no dashed-border `Modifier.border` overload — everything else (radius, border
 * color, spacing, typography) still comes from the shared tokens, nothing is a one-off literal
 * beyond the dash pattern itself.
 *
 * [content] is an optional trailing slot below the title/body — e.g. the Map Editor's empty map
 * state (PLT-08) needs a primary/secondary button pair here; every other caller just omits it.
 */
@Composable
fun IloveViEmptyStateCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = IloveViColors.border,
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    size = Size(size.width, size.height),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(
                                8.dp.toPx(),
                                6.dp.toPx()
                            )
                        ),
                    ),
                )
            }
            .padding(IloveViSpacing.space5),
    ) {
        Text(text = title, color = IloveViColors.ink, style = MaterialTheme.typography.titleMedium)
        Text(
            text = body,
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = IloveViSpacing.space1),
        )
        content()
    }
}

@Preview
@Composable
private fun IloveViEmptyStateCardPreview() {
    IloveViTheme {
        IloveViEmptyStateCard(
            title = "No pin selected",
            body = "Place a new pin or select an existing one on the map to edit it here.",
            modifier = Modifier.width(340.dp),
        )
    }
}

@Preview
@Composable
private fun IloveViEmptyStateCardWithContentPreview() {
    IloveViTheme {
        IloveViEmptyStateCard(
            title = "No map loaded",
            body = "Load a map image to get started.",
            modifier = Modifier.width(340.dp),
        ) {
            IloveViButton(
                text = "Load map image...",
                onClick = {},
                modifier = Modifier.padding(top = IloveViSpacing.space3),
            )
            IloveViButton(
                text = "Open local image (offline, no upload)",
                onClick = {},
                variant = IloveViButtonVariant.Secondary,
                modifier = Modifier.padding(top = IloveViSpacing.space2),
            )
        }
    }
}

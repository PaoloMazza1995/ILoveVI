package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViFontFamily
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme

/**
 * Vertical icon-above-label button for a narrow tool rail (icon on top, a short text caption
 * underneath, always visible — never icon-only). Distinct from [IloveViNavBar]'s destinations
 * (fixed 5-item horizontal bar, active = plain color change, no border) and from
 * [IloveViIconButton] (44dp, icon-only, a `contentDescription` instead of a visible label): the
 * Map Editor Functional Analysis's tool column (EDT-20, §4 region 2) explicitly wants a visible
 * label under every icon *and* "active state with pink border," which neither existing component
 * provides. Every button carries its own text label (never icon-only), satisfying this app's
 * accessibility rule everywhere this is used without the caller having to remember a
 * `contentDescription`.
 *
 * [active] draws a 2dp `brandPink` border and tints both the icon and label pink; inactive is
 * `inkMuted`. [enabled] (e.g. Undo/Redo with an empty history) dims to `inkFaint` and stops the
 * click from firing at all, rather than leaving the button clickable with just a dimmer icon.
 * Caller supplies the icon (same `(tint: Color) -> Unit` shape [IloveViIconButton] already uses)
 * so this works with both the Canvas-drawn `IconXxx()` composables and category/sub-type
 * [IloveViIcon]s alike.
 */
@Composable
fun IloveViRailButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (tint: Color) -> Unit,
) {
    val tint = when {
        !enabled -> IloveViColors.inkFaint
        active -> IloveViColors.brandPink
        else -> IloveViColors.inkMuted
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(IloveViShapes.radiusSm)
            .let {
                if (active) it.border(2.dp, IloveViColors.brandPink, IloveViShapes.radiusSm) else it
            }
            .semantics { role = Role.Button }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = IloveViSpacing.space2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon(tint)
        Text(
            text = label,
            color = tint,
            style = railLabelStyle(),
            modifier = Modifier.padding(top = IloveViSpacing.space1),
        )
    }
}

/** Same 12sp/600 "caption-small" treatment `IloveViNavBar`'s own label uses — not a tokens.json style, literal value for this one spot. */
@Composable
private fun railLabelStyle(): TextStyle = TextStyle(
    fontFamily = IloveViFontFamily.sans(),
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
)

@Preview
@Composable
private fun IloveViRailButtonActivePreview() {
    IloveViTheme {
        Column(
            modifier = Modifier.width(72.dp).background(IloveViColors.bg900)
                .padding(IloveViSpacing.space2),
        ) {
            IloveViRailButton(
                label = "Place",
                active = true,
                onClick = {}) { tint -> IconCheck(tint = tint) }
        }
    }
}

@Preview
@Composable
private fun IloveViRailButtonInactivePreview() {
    IloveViTheme {
        Column(
            modifier = Modifier.width(72.dp).background(IloveViColors.bg900)
                .padding(IloveViSpacing.space2),
        ) {
            IloveViRailButton(label = "Select", active = false, onClick = {}) { tint ->
                IconCheck(
                    tint = tint
                )
            }
        }
    }
}

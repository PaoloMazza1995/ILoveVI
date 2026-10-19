package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pamaz.vi.design.IloveViCategory
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViFontFamily
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme
import ilovevi.core.design.generated.resources.Res
import ilovevi.core.design.generated.resources.design_cluster_badge_content_description
import org.jetbrains.compose.resources.stringResource

/**
 * [BadgeDiameter] is purely visual; [HitTargetDiameter] is what's actually tappable, this app's
 * 44dp-minimum rule everywhere a control is tappable (same rule `:core:mapview`'s pin hit-testing
 * and this module's own [IloveViIconButton] use).
 */
private val BadgeDiameter = 40.dp
private val HitTargetDiameter = 44.dp
private val BadgeBorderWidth = 2.dp

/** The highest number this badge ever shows as a literal digit; anything above becomes "99+". */
private const val MAX_DISPLAYED_COUNT = 99

/**
 * The count text, Anton ([IloveViFontFamily.display]) at a literal 14sp: none of
 * `Type.kt`'s display-* slots (20/32/40sp) fit 2-3 characters inside a 40dp circle, the same
 * situation `IloveViNavBar`'s own local "caption-small" style is already in (see that component's
 * README note) — a local, undocumented-by-tokens size for the same reason.
 */
@Composable
private fun clusterCountTextStyle(): TextStyle = TextStyle(
    fontFamily = IloveViFontFamily.display(),
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 16.sp,
)

/**
 * Pin-group marker for the map: a solid circle in the cluster's [category] color (or
 * [IloveViColors.brandPurple] — the one brand accent not already a category color — when
 * [category] is `null`, meaning a mixed-category cluster) with the pin count centered inside.
 * Tapping it is expected to zoom the map in on that cluster; this component only draws the badge
 * and reports the tap, it owns no clustering/grouping logic at all (that's `feature:map`'s).
 *
 * [count] is shown as a literal number up to [MAX_DISPLAYED_COUNT], then `"99+"` — this app's
 * product rule groups pins once a cluster passes 10, so this badge is never shown for `count <=
 * 10` in practice, but it doesn't enforce that itself (same "component only draws" boundary).
 *
 * 40dp visual diameter, 44dp tap target (this app's minimum everywhere a control is tappable),
 * 2dp [IloveViColors.bg900] border so the badge stays readable over any map background, same
 * idiom [IloveViMapPin]'s own border uses. Text color is
 * [IloveViColors.inkOnBrand], already verified (see the design system README's Accessibility
 * section) to stay above 4.5:1 contrast against every brand accent, pink/orange/teal/purple alike.
 */
@Composable
fun IloveViClusterBadge(
    count: Int,
    category: IloveViCategory?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill = category?.color ?: IloveViColors.brandPurple
    val displayText = if (count > MAX_DISPLAYED_COUNT) "$MAX_DISPLAYED_COUNT+" else count.toString()
    val description = stringResource(Res.string.design_cluster_badge_content_description, count)

    Box(
        modifier = modifier
            .size(HitTargetDiameter)
            .semantics {
                this.contentDescription = description
                this.role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(BadgeDiameter)
                .clip(CircleShape)
                .background(fill, CircleShape)
                .border(BadgeBorderWidth, IloveViColors.bg900, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = displayText,
                color = IloveViColors.inkOnBrand,
                style = clusterCountTextStyle()
            )
        }
    }
}

/** The counts and categories every ClusterBadge preview below is built from. */
private val PreviewCounts = listOf(11, 42, 99, 120)
private val PreviewCategories = listOf(
    IloveViCategory.Weapons, IloveViCategory.Vehicles, IloveViCategory.Items, null,
)

@Composable
private fun ClusterBadgeGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space3)) {
        for (count in PreviewCounts) {
            Row(horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space3)) {
                for (category in PreviewCategories) {
                    IloveViClusterBadge(count = count, category = category, onClick = {})
                }
            }
        }
    }
}

/**
 * 11/42/99/120 (one row each) x Weapons/Vehicles/Items/mixed (one column each) — the only theme
 * this design system has is Night (see the README's "A single theme, Night"): there is no light
 * variant to preview here, unlike a typical light/dark pair.
 */
@Preview
@Composable
private fun IloveViClusterBadgeAllCountsAndCategoriesPreview() {
    IloveViTheme {
        Box(modifier = Modifier.background(IloveViColors.bg900).padding(IloveViSpacing.space3)) {
            ClusterBadgeGrid()
        }
    }
}

/** Same grid at 2x system font scale, to check the count text never clips the circle at "99+" / large accessibility text sizes. */
@Preview(fontScale = 2f)
@Composable
private fun IloveViClusterBadgeLargeTextPreview() {
    IloveViTheme {
        Box(modifier = Modifier.background(IloveViColors.bg900).padding(IloveViSpacing.space3)) {
            ClusterBadgeGrid()
        }
    }
}

package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViFontFamily
import com.pamaz.vi.design.IloveViTheme

/**
 * design/components/NavBar/README.md: always 5 destinations (Map, Catalog, Music, News,
 * Settings), never fewer or more (v1.1 adds News).
 */
enum class IloveViNavDestination(val label: String) {
    Map("Map"),
    Catalog("Catalog"),
    Music("Music"),
    News("News"),
    Settings("Settings"),
}

/**
 * Label under the icon at 12sp/600: literal value from preview.html, does not match the
 * `caption` token (13sp/500) or any other tokens.json style, so it is not exposed in Type.kt
 * as a style reusable elsewhere.
 */
@Composable
private fun navLabelStyle(): TextStyle = TextStyle(
    fontFamily = IloveViFontFamily.sans(),
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
)

/**
 * Fixed navigation bar, 5 destinations, 72dp height, 1px top border,
 * no shadow. Active: brand-pink; inactive: ink-muted. No category color here.
 * Each destination gets equal width (`weight(1f)`); the label is clipped to a single line with
 * an ellipsis because at narrow widths (5 items in 360dp = 72dp/column) a longer translated
 * label such as the Italian "Impostazioni" would otherwise overflow its column.
 */
@Composable
fun IloveViNavBar(
    selected: IloveViNavDestination,
    onSelect: (IloveViNavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = IloveViColors.border
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(IloveViColors.bg900)
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        IloveViNavDestination.entries.forEach { destination ->
            val tint = if (destination == selected) IloveViColors.brandPink else IloveViColors.inkMuted
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(destination) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                NavIcon(destination = destination, tint = tint)
                Text(
                    text = destination.label,
                    color = tint,
                    style = navLabelStyle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun NavIcon(destination: IloveViNavDestination, tint: Color) {
    when (destination) {
        IloveViNavDestination.Map -> IconMapPinOutline(tint = tint)
        IloveViNavDestination.Catalog -> IconCatalogList(tint = tint)
        IloveViNavDestination.Music -> IconMusicNote(tint = tint)
        IloveViNavDestination.News -> IconRss(tint = tint)
        IloveViNavDestination.Settings -> IconSettingsSliders(tint = tint)
    }
}

@Preview
@Composable
private fun IloveViNavBarPreview() {
    IloveViTheme {
        IloveViNavBar(selected = IloveViNavDestination.Map, onSelect = {})
    }
}

@Preview
@Composable
private fun IloveViNavBarNewsActivePreview() {
    IloveViTheme {
        IloveViNavBar(selected = IloveViNavDestination.News, onSelect = {})
    }
}

package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pamaz.vi.design.IloveViCategory
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViFontFamily
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme

/** design/components/MapPin/README.md */
enum class IloveViMapPinCategory(val category: IloveViCategory) {
    Weapon(IloveViCategory.Weapons),
    Vehicle(IloveViCategory.Vehicles),
    Item(IloveViCategory.Items),
}

/** [category]'s generic icon, used when [IloveViMapPin] isn't given a more specific sub-type [icon]. */
private fun IloveViMapPinCategory.defaultIcon(): IloveViIcon = when (this) {
    IloveViMapPinCategory.Weapon -> IloveViIcon.PISTOL
    IloveViMapPinCategory.Vehicle -> IloveViIcon.CAR
    IloveViMapPinCategory.Item -> IloveViIcon.BOX
}

private val DefaultPinDiameter = 32.dp
private val SelectedRingExtra = 16.dp
private val SelectedBoxExtra = 20.dp
private val PinBorderWidth = 3.dp
private val DimmedBorderWidth = 2.dp
private val IconInset = 4.dp
private val IncompleteBadgeDiameter = 14.dp

/**
 * Marker on the map: a circle in the category color, ink-on-brand icon, 3dp bg-900 outer border.
 * [diameter] (default 32dp, the original fixed size) is now a parameter so callers can match the
 * different sizes a single screen needs (e.g. `:tools:map-editor`'s 34dp draft / 22dp
 * already-in-the-database pins) without each caller wrapping this composable in its own `Modifier.
 * size()` (which wouldn't actually rescale the hardcoded internal layout). When selected, the
 * colored circle gets an extra 3dp ink outer ring, offset by a fixed 16dp beyond [diameter] — the
 * same proportion the original fixed-size 32dp/48dp pair used (preview.html's colored circle keeps
 * the same radius in both the normal and selected pin).
 *
 * [icon] shows the entry's *sub-type* glyph (e.g. [IloveViIcon.RIFLE] for a rifle), from
 * `EntryIcons.kt`'s `Entry.icon()`; it defaults to [category]'s generic icon
 * (Pistol/Car/Box) for callers that only know the category. The icon itself stays decorative
 * (`contentDescription = null`): the caller (`:core:mapview`'s `PinLayer`) already attaches one
 * merged semantics description to the whole pin, so a second description here would be announced
 * twice by a screen reader.
 *
 * [dimmed] renders an **outline-only** circle (no fill, no icon, never a selection ring) in the
 * category color — for a read-only reference (e.g. `:tools:map-editor`'s "pins already in the
 * database" overlay, Map Editor Functional Analysis EDT-11: "dimmed, outline only, not
 * clickable") that must stay visually distinct from the pins actually being edited. At the small
 * sizes this is normally used at, an icon wouldn't be legible anyway.
 *
 * [incomplete] draws a small `warningYellow` "!" badge in the top-right corner — a draft pin with
 * no entry yet (Map Editor Functional Analysis EDT-33/EDT-50). Has no effect when [dimmed] (a
 * database pin is never incomplete).
 */
@Composable
fun IloveViMapPin(
    category: IloveViMapPinCategory,
    modifier: Modifier = Modifier,
    icon: IloveViIcon = category.defaultIcon(),
    diameter: Dp = DefaultPinDiameter,
    selected: Boolean = false,
    dimmed: Boolean = false,
    incomplete: Boolean = false,
) {
    val boxSize = if (selected && !dimmed) diameter + SelectedBoxExtra else diameter
    val iconSize = diameter - (PinBorderWidth + IconInset) * 2

    Box(modifier = modifier.size(boxSize), contentAlignment = Alignment.Center) {
        if (dimmed) {
            Box(
                modifier = Modifier
                    .size(diameter)
                    .clip(CircleShape)
                    .border(DimmedBorderWidth, category.category.color, CircleShape),
            )
            return@Box
        }

        if (selected) {
            Box(
                modifier = Modifier
                    .size(diameter + SelectedRingExtra)
                    .clip(CircleShape)
                    .border(PinBorderWidth, IloveViColors.ink, CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(category.category.color, CircleShape)
                .border(PinBorderWidth, IloveViColors.bg900, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            IloveViIcon(icon = icon, tint = IloveViColors.inkOnBrand, size = iconSize)
        }
        if (incomplete) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(IncompleteBadgeDiameter)
                    .clip(CircleShape)
                    .background(IloveViColors.warningYellow, CircleShape)
                    .border(1.dp, IloveViColors.bg900, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    color = IloveViColors.inkOnBrand,
                    style = TextStyle(
                        fontFamily = IloveViFontFamily.sans(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun IloveViMapPinWeaponPreview() {
    IloveViTheme { IloveViMapPin(category = IloveViMapPinCategory.Weapon) }
}

@Preview
@Composable
private fun IloveViMapPinVehiclePreview() {
    IloveViTheme { IloveViMapPin(category = IloveViMapPinCategory.Vehicle) }
}

@Preview
@Composable
private fun IloveViMapPinItemPreview() {
    IloveViTheme { IloveViMapPin(category = IloveViMapPinCategory.Item) }
}

@Preview
@Composable
private fun IloveViMapPinSelectedPreview() {
    IloveViTheme { IloveViMapPin(category = IloveViMapPinCategory.Weapon, selected = true) }
}

/** A pin with a specific sub-type icon, rather than the default category-level one. */
@Preview
@Composable
private fun IloveViMapPinSubTypePreview() {
    IloveViTheme {
        IloveViMapPin(category = IloveViMapPinCategory.Weapon, icon = IloveViIcon.RIFLE)
    }
}

@Preview
@Composable
private fun IloveViMapPinDimmedPreview() {
    IloveViTheme { IloveViMapPin(category = IloveViMapPinCategory.Vehicle, dimmed = true) }
}

/** A draft pin with no entry yet (Map Editor Functional Analysis EDT-33). */
@Preview
@Composable
private fun IloveViMapPinIncompletePreview() {
    IloveViTheme { IloveViMapPin(category = IloveViMapPinCategory.Item, incomplete = true) }
}

/** The map editor's own draft (34dp) vs. already-in-the-database (22dp, dimmed) sizes, side by side. */
@Preview
@Composable
private fun IloveViMapPinSizesPreview() {
    IloveViTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space3)) {
            IloveViMapPin(category = IloveViMapPinCategory.Weapon, diameter = 34.dp)
            IloveViMapPin(category = IloveViMapPinCategory.Weapon, diameter = 22.dp, dimmed = true)
        }
    }
}

package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViCategory
import com.pamaz.vi.design.IloveViColors
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

private val PinDiameter = 32.dp
private val SelectedRingDiameter = 48.dp
private val SelectedBoxDiameter = 52.dp
private val PinBorderWidth = 3.dp
private val IconInset = 4.dp

/**
 * Marker on the map: 32dp circle in the category color, ink-on-brand icon, 3dp
 * bg-900 outer border. The colored pin always stays 32dp: when selected (52dp total) it gets
 * an extra 3dp ink outer ring at 48dp diameter, as in preview.html (the colored circle
 * there keeps the same r16 radius in both the normal and selected pin).
 *
 * [icon] shows the entry's *sub-type* glyph (e.g. [IloveViIcon.RIFLE] for a rifle), from
 * `EntryIcons.kt`'s `Entry.icon()`; it defaults to [category]'s generic icon
 * (Pistol/Car/Box) for callers that only know the category. The icon itself stays decorative
 * (`contentDescription = null`): the caller (`:core:mapview`'s `PinLayer`) already attaches one
 * merged semantics description to the whole pin, so a second description here would be announced
 * twice by a screen reader.
 */
@Composable
fun IloveViMapPin(
    category: IloveViMapPinCategory,
    modifier: Modifier = Modifier,
    icon: IloveViIcon = category.defaultIcon(),
    selected: Boolean = false,
) {
    val boxSize = if (selected) SelectedBoxDiameter else PinDiameter
    val iconSize = PinDiameter - (PinBorderWidth + IconInset) * 2

    Box(modifier = modifier.size(boxSize), contentAlignment = Alignment.Center) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(SelectedRingDiameter)
                    .clip(CircleShape)
                    .border(PinBorderWidth, IloveViColors.ink, CircleShape),
            )
        }
        Box(
            modifier = Modifier
                .size(PinDiameter)
                .clip(CircleShape)
                .background(category.category.color, CircleShape)
                .border(PinBorderWidth, IloveViColors.bg900, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            IloveViIcon(icon = icon, tint = IloveViColors.inkOnBrand, size = iconSize)
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

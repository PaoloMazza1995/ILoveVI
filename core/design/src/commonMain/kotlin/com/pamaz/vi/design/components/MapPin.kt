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
 */
@Composable
fun IloveViMapPin(
    category: IloveViMapPinCategory,
    modifier: Modifier = Modifier,
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
            when (category) {
                IloveViMapPinCategory.Weapon -> IconWeaponCrosshair(tint = IloveViColors.inkOnBrand, size = iconSize)
                IloveViMapPinCategory.Vehicle -> IconVehicle(tint = IloveViColors.inkOnBrand, size = iconSize)
                IloveViMapPinCategory.Item -> IconStar(tint = IloveViColors.inkOnBrand, size = iconSize)
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

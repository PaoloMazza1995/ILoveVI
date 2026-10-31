package com.pamaz.vi.feature.catalog.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.model.Entry
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.components.labelRes
import ilovevi.feature.catalog.generated.resources.Res
import ilovevi.feature.catalog.generated.resources.detail_attribute_ammo
import ilovevi.feature.catalog.generated.resources.detail_attribute_price
import ilovevi.feature.catalog.generated.resources.detail_attribute_type
import org.jetbrains.compose.resources.stringResource

/**
 * DET-04's attributes table: one composable, driven by [entry]'s concrete [Entry] type, for all
 * three categories — [Weapon] shows its fixed type/price/ammo fields; [Vehicle]/[Item] have no
 * fixed attribute set (per the Functional Analysis: "Vehicles and items need their own sets", not
 * yet designed), so they render whichever free-form key/value pairs
 * [Weapon.attributes][com.pamaz.vi.core.model.Weapon.attributes]-shaped `attributes` map happens
 * to carry, label-humanized (`top_speed` -> `Top Speed`) since those keys are raw storage keys,
 * never meant to be shown verbatim.
 */
@Composable
internal fun AttributesTable(entry: Entry, modifier: Modifier = Modifier) {
    val rows: List<Pair<String, String>> = when (entry) {
        is Weapon -> buildList {
            add(stringResource(Res.string.detail_attribute_type) to stringResource(entry.labelRes()))
            entry.price?.let { price -> add(stringResource(Res.string.detail_attribute_price) to price.toString()) }
            entry.ammoType?.let { ammo -> add(stringResource(Res.string.detail_attribute_ammo) to ammo) }
        }

        is Vehicle -> entry.attributes.map { (key, value) -> key.humanizeAttributeKey() to value }
        is Item -> entry.attributes.map { (key, value) -> key.humanizeAttributeKey() to value }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        rows.forEach { (label, value) -> AttributeRow(label = label, value = value) }
    }
}

@Composable
private fun AttributeRow(label: String, value: String, modifier: Modifier = Modifier) {
    val dividerColor = IloveViColors.border
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(vertical = IloveViSpacing.space3),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(text = value, color = IloveViColors.ink, style = MaterialTheme.typography.titleMedium)
    }
}

private fun String.humanizeAttributeKey(): String =
    replace('_', ' ').split(' ')
        .joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

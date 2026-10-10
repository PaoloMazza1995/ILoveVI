package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViTheme

/**
 * design/components/Switch/README.md: 52x32dp, radius-md track (16dp), 24dp thumb with 4dp
 * margin on each side. On: brand-teal track, ink-on-brand thumb, on the right. Off:
 * border track, ink-muted thumb, on the left.
 */
@Composable
fun IloveViSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val trackColor = if (checked) IloveViColors.brandTeal else IloveViColors.border
    val thumbColor = if (checked) IloveViColors.inkOnBrand else IloveViColors.inkMuted
    val alignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(IloveViShapes.radiusMd)
            .background(trackColor, IloveViShapes.radiusMd)
            .let {
                if (onCheckedChange != null) {
                    it.clickable { onCheckedChange(!checked) }
                } else it
            }
            .padding(4.dp),
        contentAlignment = alignment,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(thumbColor, CircleShape),
        )
    }
}

/**
 * The Switch should never be used on its own: the README mandates a label on the left that says
 * what it controls. This wrapper makes the rule impossible to violate from the public API.
 */
@Composable
fun IloveViLabeledSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = IloveViColors.ink, style = MaterialTheme.typography.bodyLarge)
        IloveViSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview
@Composable
private fun IloveViSwitchOnPreview() {
    IloveViTheme {
        IloveViLabeledSwitch(label = "Map labels", checked = true, onCheckedChange = {})
    }
}

@Preview
@Composable
private fun IloveViSwitchOffPreview() {
    IloveViTheme {
        IloveViLabeledSwitch(label = "Auto sync", checked = false, onCheckedChange = {})
    }
}

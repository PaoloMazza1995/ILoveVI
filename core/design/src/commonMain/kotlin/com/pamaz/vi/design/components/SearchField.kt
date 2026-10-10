package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViTheme

/**
 * Text search field, the same in Catalog and Music.
 * design/components/SearchField/README.md: bg-800 background, border outline, radius-sm radius, fixed
 * 48dp height, no magnifying-glass icon, placeholder always a concrete question about what to search for.
 */
@Composable
fun IloveViSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = IloveViColors.ink)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(IloveViShapes.radiusSm)
            .background(IloveViColors.bg800, IloveViShapes.radiusSm)
            .border(1.dp, IloveViColors.border, IloveViShapes.radiusSm)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(text = placeholder, color = IloveViColors.inkMuted, style = textStyle)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            singleLine = true,
            cursorBrush = SolidColor(IloveViColors.ink),
        )
    }
}

@Preview
@Composable
private fun IloveViSearchFieldEmptyPreview() {
    IloveViTheme {
        IloveViSearchField(value = "", onValueChange = {}, placeholder = "Search by name or area")
    }
}

@Preview
@Composable
private fun IloveViSearchFieldWithTextPreview() {
    IloveViTheme {
        IloveViSearchField(value = "Pistol", onValueChange = {}, placeholder = "Search by name or area")
    }
}

package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViTheme

/** design/components/Button/README.md */
enum class IloveViButtonVariant { Primary, Secondary }

/**
 * Button for a screen's or card's main action.
 * Fixed height 52dp, radius-md radius, body-lg text (-> labelLarge), full width by default.
 */
@Composable
fun IloveViButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: IloveViButtonVariant = IloveViButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val background = when {
        !enabled -> IloveViColors.border
        variant == IloveViButtonVariant.Primary -> IloveViColors.brandPink
        else -> Color.Transparent
    }
    val textColor = when {
        !enabled -> IloveViColors.inkFaint
        variant == IloveViButtonVariant.Primary -> IloveViColors.inkOnBrand
        else -> IloveViColors.ink
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(IloveViShapes.radiusMd)
            .let {
                if (variant == IloveViButtonVariant.Secondary && enabled) {
                    it.border(1.dp, IloveViColors.border, IloveViShapes.radiusMd)
                } else it
            }
            .background(background, IloveViShapes.radiusMd)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = textColor, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview
@Composable
private fun IloveViButtonPrimaryPreview() {
    IloveViTheme {
        IloveViButton(text = "Show on map", onClick = {}, variant = IloveViButtonVariant.Primary)
    }
}

@Preview
@Composable
private fun IloveViButtonSecondaryPreview() {
    IloveViTheme {
        IloveViButton(text = "Add more tracks", onClick = {}, variant = IloveViButtonVariant.Secondary)
    }
}

@Preview
@Composable
private fun IloveViButtonDisabledPreview() {
    IloveViTheme {
        IloveViButton(text = "Not available", onClick = {}, enabled = false)
    }
}

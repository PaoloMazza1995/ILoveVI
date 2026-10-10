package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViTheme

/** design/components/IconButton/README.md */
enum class IloveViIconButtonSurface { OnDarkSurface, OnMap }

/**
 * 44x44dp control with a single icon, never text: the app's minimum tappable size.
 * `icon` draws the glyph directly (see IloveViIcons.kt) in the `tint` color suggested by the
 * surface variant; the caller can still force a different color (e.g. a category color to
 * signal a default action). See [IloveViMapControls] for the two-button map zoom cluster.
 */
@Composable
fun IloveViIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    surface: IloveViIconButtonSurface = IloveViIconButtonSurface.OnDarkSurface,
    icon: @Composable (tint: Color) -> Unit,
) {
    val background = when (surface) {
        IloveViIconButtonSurface.OnDarkSurface -> Color.Transparent
        IloveViIconButtonSurface.OnMap -> IloveViColors.bg800
    }
    val shape = IloveViShapes.radiusXs

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(shape)
            .let {
                if (surface == IloveViIconButtonSurface.OnMap) {
                    it.border(1.dp, IloveViColors.border, shape)
                } else it
            }
            .background(background, shape)
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon(IloveViColors.ink)
    }
}

@Preview
@Composable
private fun IloveViIconButtonBackPreview() {
    IloveViTheme {
        IloveViIconButton(
            contentDescription = "Back",
            onClick = {},
            surface = IloveViIconButtonSurface.OnDarkSurface,
        ) { tint -> IconArrowBack(tint = tint) }
    }
}

@Preview
@Composable
private fun IloveViIconButtonZoomPreview() {
    IloveViTheme {
        IloveViIconButton(
            contentDescription = "Zoom in",
            onClick = {},
            surface = IloveViIconButtonSurface.OnMap,
        ) { tint -> IconZoomIn(tint = tint) }
    }
}

@Preview
@Composable
private fun IloveViIconButtonRefreshPreview() {
    IloveViTheme {
        IloveViIconButton(
            contentDescription = "Refresh",
            onClick = {},
            surface = IloveViIconButtonSurface.OnDarkSurface,
        ) { tint -> IconRefresh(tint = tint) }
    }
}

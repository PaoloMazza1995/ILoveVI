package com.pamaz.vi.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Choice of token exposure: static objects for colors/spacing/shapes, Material3's
 * CompositionLocal for typography.
 *
 * Rationale: design/tokens.json defines only ONE theme ("Night"), so colors, spacing and
 * radii never vary at runtime — there's no need for the ceremony of a dedicated CompositionLocal
 * for values that never change (consistent with the product's own "Zero ceremony" principle).
 * IloveViColors / IloveViSpacing / IloveViShapes therefore remain plain Kotlin objects,
 * usable even outside a Composable tree.
 * Typography, on the other hand, is installed in MaterialTheme (which already exposes it via
 * its internal CompositionLocal, MaterialTheme.typography): this means any standard
 * Material3 components used in the future inherit it automatically, without having to reinvent
 * a propagation mechanism already present in the framework.
 */
@Composable
fun IloveViTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = IloveViColors.brandPink,
        onPrimary = IloveViColors.inkOnBrand,
        secondary = IloveViColors.brandOrange,
        onSecondary = IloveViColors.inkOnBrand,
        tertiary = IloveViColors.brandTeal,
        onTertiary = IloveViColors.inkOnBrand,
        background = IloveViColors.bg900,
        onBackground = IloveViColors.ink,
        surface = IloveViColors.bg800,
        onSurface = IloveViColors.ink,
        surfaceVariant = IloveViColors.bg800,
        onSurfaceVariant = IloveViColors.inkMuted,
        outline = IloveViColors.border,
        outlineVariant = IloveViColors.border,
        // No "error" token exists in tokens.json: errorContainer/error/onError stay
        // at Material3's default values, they are not invented.
    )
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        // No radius token beyond radius-lg (20px) exists for an "extraLarge" slot:
        // it stays at Shapes()'s default value.
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = rememberIloveViTypography(),
        shapes = shapes,
        content = content,
    )
}

@Preview
@Composable
private fun IloveViThemePreview() {
    IloveViTheme {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            color = MaterialTheme.colorScheme.background,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "ILoveVI Theme",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Previewing color scheme, typography, and shapes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


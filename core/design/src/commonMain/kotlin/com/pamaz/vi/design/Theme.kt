package com.pamaz.vi.design

import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * `System` follows the device's light/dark setting (live — [isSystemInDarkTheme] already
 * recomposes on a device setting change, no extra plumbing needed), `Light`/`Dark` pin one theme
 * regardless of the device. The default of the *stored preference* (not of this enum) is
 * [System] — see `:core:data`'s `SettingsRepository`/`Preferences.themeMode` and `shared`'s
 * `App()` for where that default actually lives.
 */
enum class IloveViThemeMode { System, Light, Dark }

/** Resolves [IloveViThemeMode] to an actual light/dark choice, given the live device setting. */
@Composable
fun IloveViThemeMode.resolveIsDark(): Boolean = when (this) {
    IloveViThemeMode.System -> isSystemInDarkTheme()
    IloveViThemeMode.Light -> false
    IloveViThemeMode.Dark -> true
}

/**
 * Choice of token exposure: see [IloveViColors]' own KDoc for the Color.kt half of this. Spacing/
 * shapes still never vary by theme (only colors do), so [IloveViSpacing]/[IloveViShapes] stay
 * plain objects as before — this step didn't need to touch either.
 *
 * [themeMode] resolves to light/dark via [resolveIsDark]; [ApplySystemBarsAppearance] (expect/
 * actual composable, one per platform) switches the status/navigation bar icon color to match —
 * a composable (not a plain function called from a [androidx.compose.runtime.LaunchedEffect])
 * because Android's implementation needs `LocalView.current`, only readable in composition.
 */
@Composable
fun IloveViTheme(
    themeMode: IloveViThemeMode = IloveViThemeMode.System,
    content: @Composable () -> Unit
) {
    val isDark = themeMode.resolveIsDark()
    val themeColors = if (isDark) NightColors else LightColors

    ApplySystemBarsAppearance(isDark)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = themeColors.brandPink,
            onPrimary = themeColors.inkOnBrand,
            secondary = themeColors.brandOrange,
            onSecondary = themeColors.inkOnBrand,
            tertiary = themeColors.brandTeal,
            onTertiary = themeColors.inkOnBrand,
            background = themeColors.bg900,
            onBackground = themeColors.ink,
            surface = themeColors.bg800,
            onSurface = themeColors.ink,
            surfaceVariant = themeColors.bg800,
            onSurfaceVariant = themeColors.inkMuted,
            outline = themeColors.border,
            outlineVariant = themeColors.border,
        )
    } else {
        lightColorScheme(
            primary = themeColors.brandPink,
            onPrimary = themeColors.inkOnBrand,
            secondary = themeColors.brandOrange,
            onSecondary = themeColors.inkOnBrand,
            tertiary = themeColors.brandTeal,
            onTertiary = themeColors.inkOnBrand,
            background = themeColors.bg900,
            onBackground = themeColors.ink,
            surface = themeColors.bg800,
            onSurface = themeColors.ink,
            surfaceVariant = themeColors.bg800,
            onSurfaceVariant = themeColors.inkMuted,
            outline = themeColors.border,
            outlineVariant = themeColors.border,
            // No "error" token exists in tokens.json for either theme: errorContainer/error/
            // onError stay at Material3's default values, they are not invented.
        )
    }
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        // No radius token beyond radius-lg (20px) exists for an "extraLarge" slot:
        // it stays at Shapes()'s default value.
    )
    CompositionLocalProvider(LocalIloveViColors provides themeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = rememberIloveViTypography(),
            shapes = shapes,
            content = content,
        )
    }
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

@Preview
@Composable
private fun IloveViThemeLightPreview() {
    IloveViTheme(themeMode = IloveViThemeMode.Light) {
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

/**
 * Switches the status/navigation bar icon color to match [isDark] — `androidMain`'s `actual` uses
 * `LocalView.current`'s window + `WindowCompat.getInsetsController(...).isAppearanceLightStatusBars`/
 * `isAppearanceLightNavigationBars`; `iosMain`'s sets the key window's
 * `overrideUserInterfaceStyle`. `jvmMain`'s is a no-op (no OS-level system bars on desktop).
 */
@Composable
expect fun ApplySystemBarsAppearance(isDark: Boolean)

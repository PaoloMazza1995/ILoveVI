package com.pamaz.vi.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light status/nav bar icons on a dark theme (so they read against the dark background), dark
 * icons on a light theme — the inverse of [isDark], matching `WindowCompat`'s own
 * `isAppearanceLight*` naming (`true` = dark icons for a light background).
 */
@Composable
actual fun ApplySystemBarsAppearance(isDark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
    }
}

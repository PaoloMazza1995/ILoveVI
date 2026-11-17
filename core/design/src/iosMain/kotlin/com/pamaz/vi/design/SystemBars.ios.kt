package com.pamaz.vi.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIApplication

/**
 * Sets the key window's `overrideUserInterfaceStyle`, which drives both the status bar's
 * light/dark icon style and any other system chrome that follows light/dark appearance on iOS —
 * there's no separate "status bar only" API worth calling here, unlike Android's two-flag
 * `WindowInsetsController`.
 */
@Composable
actual fun ApplySystemBarsAppearance(isDark: Boolean) {
    LaunchedEffect(isDark) {
        val style = if (isDark) UIUserInterfaceStyle.UIUserInterfaceStyleDark
        else UIUserInterfaceStyle.UIUserInterfaceStyleLight
        UIApplication.sharedApplication.keyWindow?.overrideUserInterfaceStyle = style
    }
}

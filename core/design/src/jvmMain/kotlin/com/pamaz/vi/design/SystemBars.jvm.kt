package com.pamaz.vi.design

import androidx.compose.runtime.Composable

/** No OS-level system bars to theme on the JVM/desktop target. */
@Composable
actual fun ApplySystemBarsAppearance(isDark: Boolean) {
}

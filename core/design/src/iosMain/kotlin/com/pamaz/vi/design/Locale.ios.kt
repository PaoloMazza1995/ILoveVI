package com.pamaz.vi.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.Foundation.NSUserDefaults

/**
 * Best-effort only: iOS has no public, same-process API to override `NSLocale.preferredLanguages`
 * (it's re-read fresh only at process start), so unlike Android this cannot apply immediately —
 * see `com.pamaz.vi.design.ApplyLanguagePreference`'s own KDoc (`Locale.kt`, commonMain) for why.
 * Writing the preferred language into the standard `AppleLanguages` key is the widely-used
 * technique for making it take effect on the *next* cold launch.
 */
@Composable
actual fun ApplyLanguagePreference(languageCode: String) {
    LaunchedEffect(languageCode) {
        NSUserDefaults.standardUserDefaults.setObject(
            listOf(languageCode),
            forKey = "AppleLanguages"
        )
    }
}

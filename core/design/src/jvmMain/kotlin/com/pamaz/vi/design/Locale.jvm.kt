package com.pamaz.vi.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import java.util.Locale

/** The desktop/JVM equivalent of the Android override — see `Locale.kt` (commonMain) for why. */
@Composable
actual fun ApplyLanguagePreference(languageCode: String) {
    LaunchedEffect(languageCode) {
        Locale.setDefault(Locale.forLanguageTag(languageCode))
    }
}

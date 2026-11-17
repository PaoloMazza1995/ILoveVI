package com.pamaz.vi.design

import android.app.Activity
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Recreates the current [Activity] after a real language change (never on the first composition —
 * `previousLanguageCode` starts `null` so the initial apply doesn't cause a recreate loop at
 * launch) so every `stringResource(...)` in the torn-down-and-rebuilt composition resolves fresh
 * against the new locale.
 *
 * [LocaleList.setDefault] — not [AppCompatDelegate.setApplicationLocales] — is the call that
 * actually matters here: decompiling the real `androidx.compose.ui:ui-text-android` 1.12.0 sources
 * (not JetBrains' Compose Multiplatform redirect artifact, which is an empty stub on Android — the
 * real Jetpack Compose library is what Android actually uses) shows
 * `androidx.compose.ui.text.intl.Locale.current` — what `stringResource(...)` resolves against —
 * reads `android.os.LocaleList.getDefault()` on every API level this app supports (`minSdk` 24).
 * `AppCompatDelegate.setApplicationLocales()` alone does **not** reliably update that: its
 * live-apply path (pre-API-33) hooks `Activity.attachBaseContext`, which only runs for an
 * `AppCompatActivity` — this app's `MainActivity` is a plain `ComponentActivity` — so a first
 * attempt at this fix that called only `setApplicationLocales()` silently did nothing (the bug a
 * real test run surfaced: picking English kept every string in Italian). `LocaleList.setDefault`
 * is itself public SDK API (confirmed against the real `android.jar`, not assumed), and is exactly
 * the value every API level reads — `AppCompatDelegate.setApplicationLocales()` is still called
 * too, since it's what tells the OS's own "App languages" system settings screen (API 33+) which
 * language this app is using, which `LocaleList.setDefault` alone doesn't do.
 */
@Composable
actual fun ApplyLanguagePreference(languageCode: String) {
    val context = LocalContext.current
    var previousLanguageCode by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(languageCode) {
        val locale = Locale.forLanguageTag(languageCode)
        val changed = previousLanguageCode != null && previousLanguageCode != languageCode

        LocaleList.setDefault(LocaleList(locale))
        Locale.setDefault(locale)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))

        previousLanguageCode = languageCode
        if (changed) {
            (context as? Activity)?.recreate()
        }
    }
}

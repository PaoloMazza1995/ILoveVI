package com.pamaz.vi.design

import androidx.compose.runtime.Composable

/**
 * Applies [languageCode] ("en"/"it") to the app's own language, so `stringResource(...)` resolves
 * against it instead of the device's system language.
 *
 * Compose Multiplatform resources resolve `stringResource(...)` through
 * `rememberResourceEnvironment()`, which reads `androidx.compose.ui.text.intl.Locale.current` —
 * **not** the platform's own locale-reading `expect fun getSystemEnvironment()` (that one's only
 * used by the non-composable, rarely-called `getSystemResourceEnvironment()`). The composable path
 * is cached via `remember(...)` on Compose's own `Locale.current`, and there is deliberately no
 * public API to override it: `ComposeEnvironment`/`LocalComposeEnvironment`/`ResourceEnvironment`'s
 * constructor are all `internal` to `org.jetbrains.compose.resources` (confirmed by decompiling the
 * real `components-resources` 1.12.0 jar/sources — not from memory, per this repo's convention; a
 * first attempt at this file tried to use those APIs and failed to compile for exactly this reason).
 * This is a known, currently-unresolved Compose Multiplatform limitation (JetBrains YouTrack
 * CMP-8376, GitHub jetbrains/compose-multiplatform#4197 — "no reliable way to switch localization
 * at runtime"), not something this app is missing a trick for.
 *
 * So each platform does the best *real* thing available to it instead:
 * - **Android**: `androidx.compose.ui.text.intl.Locale.current` is backed by the Activity's own
 *   `Resources.configuration`, which [androidx.appcompat.app.AppCompatDelegate.setApplicationLocales]
 *   (a public, officially-supported AndroidX API) updates immediately. Combined with recreating the
 *   current `Activity`, this makes the change apply with no app restart — just a brief recreate, the
 *   same UX every Android app that supports in-app language switching has.
 * - **iOS**: there is no public, same-process override. Persisting the choice into `AppleLanguages`
 *   is the standard best-effort technique — it takes effect on the *next* cold launch (iOS itself
 *   re-reads `NSLocale.preferredLanguages` fresh only at process start), not immediately.
 * - **JVM**: `java.util.Locale.setDefault(...)`, the desktop-equivalent primitive.
 */
@Composable
expect fun ApplyLanguagePreference(languageCode: String)

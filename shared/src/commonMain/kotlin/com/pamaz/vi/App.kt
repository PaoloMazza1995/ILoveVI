package com.pamaz.vi

import androidx.compose.runtime.Composable

/**
 * The app's single entry point — every platform's shell (`androidApp`'s `MainActivity`,
 * `shared`'s own `MainViewController` for iOS, `webApp`'s `main`) calls this and nothing else.
 *
 * `actual`s live in two places, not one: `:core:design` and `:core:mapview` (the real app
 * shell's dependencies — see `shared/README.md`) only target JVM/Android/iOS, not js/wasmJs, so
 * the real shell's `actual` lives in the `appMain` intermediate source set (shared by
 * `androidMain`/`iosMain` only); `jsMain`/`wasmJsMain` each get their own minimal placeholder
 * `actual` instead of the real shell.
 */
@Composable
expect fun App()

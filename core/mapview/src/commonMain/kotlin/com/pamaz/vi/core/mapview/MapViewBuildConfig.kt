package com.pamaz.vi.core.mapview

/**
 * Whether the bundled dev map image ([rememberDevMapImage]) and its fallback placeholder may ever
 * be shown. Backed by [SINGLE_IMAGE_DEV_MODE_ENABLED], a `const val` generated at build time by
 * this module's `generateMapViewBuildConfig` Gradle task (see `build.gradle.kts`) — `true` unless
 * this build was invoked with `-Pilovevi.release=true` (see the module README's "Release safety"
 * section). A `const val` is inlined by the Kotlin compiler at every call site, so this can never
 * be left "on" by a stray runtime setting — the only way to change it is to recompile with a
 * different Gradle property.
 *
 * Public (not `internal`) so the app module can reuse this exact flag to decide whether its Map
 * tab shows [MapViewerSampleScreen] or a plain stub (step 8), instead of inventing a second,
 * redundant release/debug switch.
 */
object MapViewBuildConfig {
    val singleImageDevModeEnabled: Boolean get() = SINGLE_IMAGE_DEV_MODE_ENABLED
}

package com.pamaz.vi.core.mapview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.pamaz.vi.design.IloveViColors

/**
 * A self-contained demo of this module, meant for a host app's debug-only screens (step 8's Map
 * tab: "if `:core:mapview` and its `MapViewerSampleScreen` exist, the Map tab stub shows that
 * sample screen in debug builds" — see `shared/README.md`). Shows [rememberDevMapImage] (the
 * bundled dev map, or its grid-with-labels fallback — see that function's KDoc) with 6 fake pins
 * across all three categories, so the map is visible and interactive in the real app without any
 * backend, network or real content.
 *
 * This is the same content [MapViewerPreview.kt]'s private `MapViewerSingleImagePreview` shows,
 * promoted to a public, non-`@Preview` composable a host app can actually call. Gating this
 * behind [MapViewBuildConfig.singleImageDevModeEnabled] (or not calling it at all in release) is
 * the caller's responsibility — this composable itself has no opinion on when it should be shown.
 *
 * @param selectedPinId forwarded to [MapViewer]'s `selectedPinId` — lets a caller that already
 * has some externally-selected entry id (e.g. "Show on map" from a Detail screen) highlight it if
 * it happens to match one of this screen's fake pins. Real content doesn't exist yet (available
 * only after the game's release), so this is mostly a wiring demonstration today.
 */
@Composable
fun MapViewerSampleScreen(modifier: Modifier = Modifier, selectedPinId: String? = null) {
    val devImage = rememberDevMapImage()
    if (devImage == null) {
        Box(modifier = modifier.fillMaxSize().background(IloveViColors.bg900))
    } else {
        val state = rememberMapViewerState()
        val pins = remember { fakePins(6) }
        MapViewer(
            state = state,
            content = MapContent.SingleImage(devImage),
            pins = pins,
            selectedPinId = selectedPinId,
            modifier = modifier.fillMaxSize(),
        )
    }
}

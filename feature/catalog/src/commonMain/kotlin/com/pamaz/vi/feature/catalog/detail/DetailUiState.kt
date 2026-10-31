package com.pamaz.vi.feature.catalog.detail

import com.pamaz.vi.core.model.Entry
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin

/**
 * The Detail screen's single UI state (DET-01..04). [pin] is `null` when the entry has no known
 * location — the screen shows "Location not available yet" and hides "Show on map" in that case
 * (see `DetailScreen`'s KDoc). [mapConfig] is the current map image/tiles the mini-map needs to
 * actually draw [pin] on; if it's unavailable while [pin] isn't, the screen still shows the zone
 * text/description/"Show on map" (none of which need the map image itself), just not the visual
 * mini-map embed.
 */
sealed interface DetailUiState {
    data object Loading : DetailUiState
    data object NotFound : DetailUiState
    data class Loaded(val entry: Entry, val pin: MapPin?, val mapConfig: MapConfig?) : DetailUiState
}

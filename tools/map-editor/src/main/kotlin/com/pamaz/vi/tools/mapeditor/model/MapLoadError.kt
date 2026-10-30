package com.pamaz.vi.tools.mapeditor.model

/**
 * Why the map failed to load (replaces an indefinite "Loading..." — see `state/EditorState.kt`'s
 * `load()`): a plain-words [message] plus, when relevant, the exact network [requestedUrl] that
 * failed, so a maintainer can tell "config missing" from "tile 404" from "no network" at a glance
 * instead of guessing from a spinner that never stops.
 */
data class MapLoadError(val message: String, val requestedUrl: String? = null)

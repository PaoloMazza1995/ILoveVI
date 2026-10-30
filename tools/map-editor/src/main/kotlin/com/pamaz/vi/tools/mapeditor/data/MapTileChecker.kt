package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.tools.mapeditor.model.MapLoadError

/**
 * PLT-08's "Loading..." fix: after `map_config` loads, checks that tile `0/0/0` of
 * [config]'s `tileUrlTemplate` is actually reachable, so a 404/private-bucket/no-network tile
 * surfaces as a plain-words [MapLoadError] (`state/EditorState.kt`'s `load()`) instead of an
 * indefinite spinner. The fake backend's placeholder tile URLs (`data/fake/FakeMapConfigRepository.kt`)
 * don't point anywhere real, so [FakeMapTileChecker] always reports "reachable" rather than
 * failing every load in demo mode — only the real, Supabase-backed checker does a network call.
 */
interface MapTileChecker {
    /** `null` means reachable; otherwise why it isn't, with the exact address that was requested. */
    suspend fun checkReachable(config: MapConfig): MapLoadError?
}

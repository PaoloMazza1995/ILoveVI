package com.pamaz.vi.tools.mapeditor.data.fake

import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.tools.mapeditor.data.MapTileChecker
import com.pamaz.vi.tools.mapeditor.model.MapLoadError

/** Always "reachable" — the fake backend's tile URLs are placeholders, not real addresses to probe. */
class FakeMapTileChecker : MapTileChecker {
    override suspend fun checkReachable(config: MapConfig): MapLoadError? = null
}

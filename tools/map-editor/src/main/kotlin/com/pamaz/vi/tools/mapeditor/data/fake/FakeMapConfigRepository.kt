package com.pamaz.vi.tools.mapeditor.data.fake

import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.tools.mapeditor.data.MapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.MapConfigResult
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * One fixed map version by default — "Map v1 4096 x 4096" in the header chip, matching the
 * screenshot. Mutable (not just the companion `CURRENT` constant) so [FakeMapPublisher] can stand
 * in for a real "Load map image..." publish (PLT-08) by actually updating what the next
 * [loadCurrent] call returns, the same way [SupabaseMapPublisher][com.pamaz.vi.tools.mapeditor.data.supabase.SupabaseMapPublisher]
 * updates the real `map_config` table — this only works because `di/AppModule.kt` binds both to
 * the *same* Koin singleton instance.
 */
@OptIn(ExperimentalTime::class)
class FakeMapConfigRepository : MapConfigRepository {
    companion object {
        val CURRENT = MapConfig(
            mapVersion = 1,
            sourceImageUrl = "https://example.com/fake-map-v1.png",
            widthPx = 4096,
            heightPx = 4096,
            tileUrlTemplate = "https://example.com/tiles/{z}/{x}/{y}.png",
            minZoom = 0,
            maxZoom = 5,
            updatedAt = Clock.System.now(),
        )
    }

    private var current: MapConfig = CURRENT

    override suspend fun loadCurrent(): MapConfigResult = MapConfigResult.Loaded(current)

    /** The fake backend never produces a [MapConfigResult.Missing]/[MapConfigResult.Invalid] row — this is the plain [MapConfig] for callers that don't need to switch on [MapConfigResult]. */
    val currentConfig: MapConfig get() = current

    /** What [FakeMapPublisher] calls on a simulated successful publish. */
    fun publish(config: MapConfig) {
        current = config
    }
}

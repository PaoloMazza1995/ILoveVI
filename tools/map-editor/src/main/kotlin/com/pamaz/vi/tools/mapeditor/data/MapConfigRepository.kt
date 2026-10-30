package com.pamaz.vi.tools.mapeditor.data

/**
 * Loads the current map (tiles or a local image — core:mapview's `MapContent` decides which, from
 * whatever this returns). Returns [MapConfigResult], not a nullable [com.pamaz.vi.core.model.MapConfig]
 * directly, specifically so a bad `map_config` row (which would otherwise throw while building
 * that type) is just another normal result to switch on, never an uncaught exception.
 */
interface MapConfigRepository {
    suspend fun loadCurrent(): MapConfigResult
}

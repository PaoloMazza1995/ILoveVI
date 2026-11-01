package com.pamaz.vi.feature.map.fake

import com.pamaz.vi.core.data.repository.MapPinWithEntryName
import com.pamaz.vi.core.data.repository.MapRepository
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** A tiny in-memory stand-in for the real [MapRepository], for [com.pamaz.vi.feature.map.map.MapViewModel] tests. */
internal class FakeMapRepository : MapRepository {
    private val mapConfigFlow = MutableStateFlow<MapConfig?>(null)
    private val pinsFlow = MutableStateFlow<List<MapPinWithEntryName>>(emptyList())
    var throwOnPins: Boolean = false

    fun setMapConfig(config: MapConfig?) {
        mapConfigFlow.value = config
    }

    fun setPins(pins: List<MapPinWithEntryName>) {
        pinsFlow.value = pins
    }

    override fun currentMapConfig(): Flow<MapConfig?> = mapConfigFlow

    /**
     * Filters by [categories] only — this fake has no sub-type data to filter by ([MapPinWithEntryName]
     * carries no sub-type column, only the real database's join does). [MapViewModel]'s own
     * "every sub-type off means empty, not unfiltered" rule is tested without ever reaching this
     * method at all (see that test), so that's not a gap in coverage.
     */
    override fun pins(
        categories: Set<EntryCategory>,
        subTypes: Set<String>?
    ): Flow<List<MapPinWithEntryName>> =
        pinsFlow.map { all ->
            if (throwOnPins) error("boom")
            all.filter { entry -> entry.pin.category in categories }
        }

    override fun pinByEntryId(category: EntryCategory, entryId: String): Flow<MapPin?> =
        pinsFlow.map { all -> all.firstOrNull { it.pin.category == category }?.pin }
}

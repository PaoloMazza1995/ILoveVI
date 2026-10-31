package com.pamaz.vi.feature.catalog.fake

import com.pamaz.vi.core.data.repository.MapPinWithEntryName
import com.pamaz.vi.core.data.repository.MapRepository
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** A tiny in-memory stand-in for the real [MapRepository], for [com.pamaz.vi.feature.catalog.detail.DetailViewModel] tests. */
internal class FakeMapRepository : MapRepository {
    private val mapConfigFlow = MutableStateFlow<MapConfig?>(null)
    private val pinsFlow = MutableStateFlow<List<MapPin>>(emptyList())

    fun setMapConfig(config: MapConfig?) {
        mapConfigFlow.value = config
    }

    fun setPins(pins: List<MapPin>) {
        pinsFlow.value = pins
    }

    override fun currentMapConfig(): Flow<MapConfig?> = mapConfigFlow

    override fun pins(
        categories: Set<EntryCategory>,
        subTypes: Set<String>?
    ): Flow<List<MapPinWithEntryName>> =
        pinsFlow.map { pins ->
            pins.filter { it.category in categories }.map { MapPinWithEntryName(it, "") }
        }

    override fun pinByEntryId(category: EntryCategory, entryId: String): Flow<MapPin?> =
        pinsFlow.map { pins -> pins.firstOrNull { it.category == category && it.entryRef.rawEntryId() == entryId } }

    private fun com.pamaz.vi.core.model.EntryRef.rawEntryId(): String = when (this) {
        is com.pamaz.vi.core.model.EntryRef.ToWeapon -> id.value
        is com.pamaz.vi.core.model.EntryRef.ToVehicle -> id.value
        is com.pamaz.vi.core.model.EntryRef.ToItem -> id.value
    }
}

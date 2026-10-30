package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.tools.mapeditor.data.PinRepository
import com.pamaz.vi.tools.mapeditor.model.DbPin
import com.pamaz.vi.tools.mapeditor.model.rawEntryId

/**
 * Adapts [MapPinRepository] (reads `core:model.MapPin` — `EntryRef`, `mapVersion`, `updatedAt`,
 * everything a *draft* pin's database counterpart would eventually carry) to [PinRepository]'s
 * much thinner [DbPin] (just enough to draw the read-only overlay, EDT-11, plus [DbPin.mapVersion]
 * for PLT-08's version decision) — the two shapes only differ in how they name "which entry":
 * [MapPin.entryRef] vs [DbPin.entryId]/[DbPin.category].
 */
class SupabasePinRepositoryAdapter(private val repository: MapPinRepository) : PinRepository {
    override suspend fun loadAll(): List<DbPin> = repository.loadPins().map { it.toDbPin() }
}

internal fun MapPin.toDbPin() = DbPin(
    id = id.value,
    category = entryRef.category,
    entryId = entryRef.rawEntryId(),
    position = position,
    zone = zone,
    mapVersion = mapVersion,
)

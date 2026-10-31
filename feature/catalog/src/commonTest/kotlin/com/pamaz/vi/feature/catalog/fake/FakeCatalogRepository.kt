package com.pamaz.vi.feature.catalog.fake

import com.pamaz.vi.core.data.repository.CatalogRepository
import com.pamaz.vi.core.model.Entry
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * A tiny in-memory stand-in for the real `:core:database`-backed [CatalogRepository], for
 * ViewModel tests. [search]'s folding is a deliberately small, local echo of
 * `:core:database`'s own `foldForSearch()` (lowercase + a handful of Latin-1 accents) — just
 * enough to exercise "case- and accent-insensitive search" as a real behavior in these tests,
 * not a full reimplementation of that module's own, already-tested folding table.
 */
internal class FakeCatalogRepository : CatalogRepository {
    private val weaponsFlow = MutableStateFlow<List<Weapon>>(emptyList())
    private val vehiclesFlow = MutableStateFlow<List<Vehicle>>(emptyList())
    private val itemsFlow = MutableStateFlow<List<Item>>(emptyList())

    fun setWeapons(weapons: List<Weapon>) {
        weaponsFlow.value = weapons
    }

    fun setVehicles(vehicles: List<Vehicle>) {
        vehiclesFlow.value = vehicles
    }

    fun setItems(items: List<Item>) {
        itemsFlow.value = items
    }

    override fun weapons(): Flow<List<Weapon>> = weaponsFlow
    override fun vehicles(): Flow<List<Vehicle>> = vehiclesFlow
    override fun items(): Flow<List<Item>> = itemsFlow

    override fun weapon(id: WeaponId): Flow<Weapon?> =
        weaponsFlow.map { list -> list.firstOrNull { it.id == id } }

    override fun vehicle(id: VehicleId): Flow<Vehicle?> =
        vehiclesFlow.map { list -> list.firstOrNull { it.id == id } }

    override fun item(id: ItemId): Flow<Item?> =
        itemsFlow.map { list -> list.firstOrNull { it.id == id } }

    override fun searchWeapons(query: String): Flow<List<Weapon>> =
        weaponsFlow.map { list -> list.filter { it.matches(query) } }

    override fun searchVehicles(query: String): Flow<List<Vehicle>> =
        vehiclesFlow.map { list -> list.filter { it.matches(query) } }

    override fun searchItems(query: String): Flow<List<Item>> =
        itemsFlow.map { list -> list.filter { it.matches(query) } }

    override fun entry(category: EntryCategory, entryId: String): Flow<Entry?> = when (category) {
        EntryCategory.WEAPON -> weapon(WeaponId(entryId))
        EntryCategory.VEHICLE -> vehicle(VehicleId(entryId))
        EntryCategory.ITEM -> item(ItemId(entryId))
    }

    private fun Entry.matches(query: String): Boolean {
        val folded = query.fold()
        return name.fold().contains(folded) || zone?.fold()?.contains(folded) == true
    }

    private fun String.fold(): String = lowercase()
        .replace('é', 'e').replace('è', 'e').replace('ê', 'e')
        .replace('á', 'a').replace('à', 'a').replace('â', 'a')
}

package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.database.dao.CatalogDao
import com.pamaz.vi.core.model.Entry
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import kotlinx.coroutines.flow.Flow

/**
 * Weapons/vehicles/items, read-only, offline-first: every method reads straight from
 * `:core:database` and never touches the network itself — `ContentSyncer` is the only thing that
 * ever writes these tables. A screen depends on this interface, never on `:core:database`
 * directly, so the database stays this module's own implementation detail.
 */
interface CatalogRepository {
    fun weapons(): Flow<List<Weapon>>
    fun vehicles(): Flow<List<Vehicle>>
    fun items(): Flow<List<Item>>

    fun weapon(id: WeaponId): Flow<Weapon?>
    fun vehicle(id: VehicleId): Flow<Vehicle?>
    fun item(id: ItemId): Flow<Item?>

    /** Case/accent-insensitive match against name or zone. */
    fun searchWeapons(query: String): Flow<List<Weapon>>
    fun searchVehicles(query: String): Flow<List<Vehicle>>
    fun searchItems(query: String): Flow<List<Item>>

    /** Looks an entry up without the caller needing to know which of the three tables it lives in. */
    fun entry(category: EntryCategory, entryId: String): Flow<Entry?>
}

internal class CatalogRepositoryImpl(private val catalogDao: CatalogDao) : CatalogRepository {
    override fun weapons(): Flow<List<Weapon>> = catalogDao.weapons()
    override fun vehicles(): Flow<List<Vehicle>> = catalogDao.vehicles()
    override fun items(): Flow<List<Item>> = catalogDao.items()

    override fun weapon(id: WeaponId): Flow<Weapon?> = catalogDao.weapon(id)
    override fun vehicle(id: VehicleId): Flow<Vehicle?> = catalogDao.vehicle(id)
    override fun item(id: ItemId): Flow<Item?> = catalogDao.item(id)

    override fun searchWeapons(query: String): Flow<List<Weapon>> = catalogDao.searchWeapons(query)
    override fun searchVehicles(query: String): Flow<List<Vehicle>> =
        catalogDao.searchVehicles(query)

    override fun searchItems(query: String): Flow<List<Item>> = catalogDao.searchItems(query)

    override fun entry(category: EntryCategory, entryId: String): Flow<Entry?> =
        catalogDao.entry(category, entryId)
}

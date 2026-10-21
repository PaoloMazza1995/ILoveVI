package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.asListFlow
import com.pamaz.vi.core.database.asOneOrNullFlow
import com.pamaz.vi.core.database.foldForSearch
import com.pamaz.vi.core.database.mapper.encodeAttributes
import com.pamaz.vi.core.database.mapper.itemRowToItem
import com.pamaz.vi.core.database.mapper.vehicleRowToVehicle
import com.pamaz.vi.core.database.mapper.weaponRowToWeapon
import com.pamaz.vi.core.model.Entry
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

/**
 * Catalog data: weapons, vehicles and items. Writes ([upsertWeapon]/[tombstoneWeapon] etc.) are
 * exposed here too (rather than on a separate sync-only DAO) since they operate on exactly the
 * tables this DAO already wraps; the sync engine calls them directly.
 */
interface CatalogDao {
    fun weapons(): Flow<List<Weapon>>
    fun vehicles(): Flow<List<Vehicle>>
    fun items(): Flow<List<Item>>

    fun weapon(id: WeaponId): Flow<Weapon?>
    fun vehicle(id: VehicleId): Flow<Vehicle?>
    fun item(id: ItemId): Flow<Item?>

    /** Case/accent-insensitive match against name or zone — see this module's README's "Search" section. */
    fun searchWeapons(query: String): Flow<List<Weapon>>
    fun searchVehicles(query: String): Flow<List<Vehicle>>
    fun searchItems(query: String): Flow<List<Item>>

    /** Looks an entry up without the caller needing to know which of the three tables it lives in. */
    fun entry(category: EntryCategory, entryId: String): Flow<Entry?>

    suspend fun upsertWeapon(weapon: Weapon)
    suspend fun upsertVehicle(vehicle: Vehicle)
    suspend fun upsertItem(item: Item)

    suspend fun tombstoneWeapon(id: WeaponId, deletedAt: Instant, updatedAt: Instant)
    suspend fun tombstoneVehicle(id: VehicleId, deletedAt: Instant, updatedAt: Instant)
    suspend fun tombstoneItem(id: ItemId, deletedAt: Instant, updatedAt: Instant)
}

internal class CatalogDaoImpl(database: IloveViDatabase) : CatalogDao {
    private val weaponsQueries = database.weaponsQueries
    private val vehiclesQueries = database.vehiclesQueries
    private val itemsQueries = database.itemsQueries

    override fun weapons(): Flow<List<Weapon>> =
        weaponsQueries.selectAll(::weaponRowToWeapon).asListFlow()

    override fun vehicles(): Flow<List<Vehicle>> =
        vehiclesQueries.selectAll(::vehicleRowToVehicle).asListFlow()

    override fun items(): Flow<List<Item>> =
        itemsQueries.selectAll(::itemRowToItem).asListFlow()

    override fun weapon(id: WeaponId): Flow<Weapon?> =
        weaponsQueries.selectById(id.value, ::weaponRowToWeapon).asOneOrNullFlow()

    override fun vehicle(id: VehicleId): Flow<Vehicle?> =
        vehiclesQueries.selectById(id.value, ::vehicleRowToVehicle).asOneOrNullFlow()

    override fun item(id: ItemId): Flow<Item?> =
        itemsQueries.selectById(id.value, ::itemRowToItem).asOneOrNullFlow()

    override fun searchWeapons(query: String): Flow<List<Weapon>> =
        weaponsQueries.search(query.foldForSearch(), ::weaponRowToWeapon).asListFlow()

    override fun searchVehicles(query: String): Flow<List<Vehicle>> =
        vehiclesQueries.search(query.foldForSearch(), ::vehicleRowToVehicle).asListFlow()

    override fun searchItems(query: String): Flow<List<Item>> =
        itemsQueries.search(query.foldForSearch(), ::itemRowToItem).asListFlow()

    override fun entry(category: EntryCategory, entryId: String): Flow<Entry?> = when (category) {
        EntryCategory.WEAPON -> weapon(WeaponId(entryId))
        EntryCategory.VEHICLE -> vehicle(VehicleId(entryId))
        EntryCategory.ITEM -> item(ItemId(entryId))
    }

    override suspend fun upsertWeapon(weapon: Weapon) {
        weaponsQueries.upsert(
            id = weapon.id.value,
            name = weapon.name,
            description = weapon.description,
            subType = weapon.subType.name,
            zone = weapon.zone,
            imageUrl = weapon.imageUrl,
            attributes = encodeAttributes(weapon.attributes),
            price = weapon.price?.toLong(),
            ammoType = weapon.ammoType,
            searchKey = entrySearchKeyFor(weapon.name, weapon.zone),
            updatedAt = weapon.updatedAt.toEpochMilliseconds(),
            deletedAt = null,
        )
    }

    override suspend fun upsertVehicle(vehicle: Vehicle) {
        vehiclesQueries.upsert(
            id = vehicle.id.value,
            name = vehicle.name,
            description = vehicle.description,
            subType = vehicle.subType.name,
            zone = vehicle.zone,
            imageUrl = vehicle.imageUrl,
            attributes = encodeAttributes(vehicle.attributes),
            searchKey = entrySearchKeyFor(vehicle.name, vehicle.zone),
            updatedAt = vehicle.updatedAt.toEpochMilliseconds(),
            deletedAt = null,
        )
    }

    override suspend fun upsertItem(item: Item) {
        itemsQueries.upsert(
            id = item.id.value,
            name = item.name,
            description = item.description,
            subType = item.subType.name,
            zone = item.zone,
            imageUrl = item.imageUrl,
            attributes = encodeAttributes(item.attributes),
            searchKey = entrySearchKeyFor(item.name, item.zone),
            updatedAt = item.updatedAt.toEpochMilliseconds(),
            deletedAt = null,
        )
    }

    override suspend fun tombstoneWeapon(id: WeaponId, deletedAt: Instant, updatedAt: Instant) {
        weaponsQueries.tombstone(
            deletedAt.toEpochMilliseconds(),
            updatedAt.toEpochMilliseconds(),
            id.value
        )
    }

    override suspend fun tombstoneVehicle(id: VehicleId, deletedAt: Instant, updatedAt: Instant) {
        vehiclesQueries.tombstone(
            deletedAt.toEpochMilliseconds(),
            updatedAt.toEpochMilliseconds(),
            id.value
        )
    }

    override suspend fun tombstoneItem(id: ItemId, deletedAt: Instant, updatedAt: Instant) {
        itemsQueries.tombstone(
            deletedAt.toEpochMilliseconds(),
            updatedAt.toEpochMilliseconds(),
            id.value
        )
    }
}

internal fun entrySearchKeyFor(name: String, zone: String?): String =
    "$name ${zone.orEmpty()}".foldForSearch()

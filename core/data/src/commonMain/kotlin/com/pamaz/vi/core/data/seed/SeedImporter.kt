package com.pamaz.vi.core.data.seed

import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.dao.CatalogDao
import com.pamaz.vi.core.database.dao.MapDao
import com.pamaz.vi.core.database.dao.MusicDao
import com.pamaz.vi.core.database.dao.SyncDao
import com.pamaz.vi.core.database.inTransaction
import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.Song
import com.pamaz.vi.core.model.SongId
import com.pamaz.vi.core.model.Station
import com.pamaz.vi.core.model.StationId
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

/** Bookkeeping key `SeedImporter` reuses `SyncDao`'s per-table cursor table for — "has the bundled seed been applied yet", not a real backend table. */
internal const val SEED_SYNC_STATE_KEY = "seed"

/**
 * Imports [SEED_JSON] into the local database exactly once, so the app is never empty offline on
 * a first launch before any real sync has had a chance to run. Reuses `SyncDao`'s per-table cursor
 * table as the "already imported" marker ([SEED_SYNC_STATE_KEY]) rather than adding a new table/
 * column to `:core:database` for a one-bit flag.
 */
interface SeedImporter {
    /** No-ops if the seed was already imported in a previous launch. */
    suspend fun importIfNeeded()
}

internal class SeedImporterImpl(
    private val database: IloveViDatabase,
    private val syncDao: SyncDao,
    private val catalogDao: CatalogDao,
    private val musicDao: MusicDao,
    private val mapDao: MapDao,
) : SeedImporter {

    override suspend fun importIfNeeded() {
        if (syncDao.lastSyncedAt(SEED_SYNC_STATE_KEY).first() != null) return

        val payload = Json.decodeFromString<SeedPayload>(SEED_JSON)
        database.inTransaction {
            payload.mapConfig.forEach { mapDao.upsertMapConfig(it.toMapConfig()) }
            payload.stations.forEach { musicDao.upsertStation(it.toStation()) }
            payload.weapons.forEach { catalogDao.upsertWeapon(it.toWeapon()) }
            payload.vehicles.forEach { catalogDao.upsertVehicle(it.toVehicle()) }
            payload.items.forEach { catalogDao.upsertItem(it.toItem()) }
            payload.songs.forEach { musicDao.upsertSong(it.toSong()) }
            payload.mapPins.forEach { mapDao.upsertPin(it.toMapPin()) }
            syncDao.setLastSyncedAt(SEED_SYNC_STATE_KEY, Clock.System.now())
        }
    }
}

private fun SeedMapConfigDto.toMapConfig() = MapConfig(
    mapVersion = mapVersion,
    sourceImageUrl = sourceImageUrl,
    widthPx = widthPx,
    heightPx = heightPx,
    tileUrlTemplate = tileUrlTemplate,
    minZoom = minZoom,
    maxZoom = maxZoom,
    updatedAt = Instant.parse(updatedAt),
)

private fun SeedStationDto.toStation() = Station(
    id = StationId(id),
    name = name,
    updatedAt = Instant.parse(updatedAt),
)

private fun SeedWeaponDto.toWeapon() = Weapon(
    id = WeaponId(id),
    name = name,
    description = description,
    subType = WeaponType.valueOf(subType),
    zone = zone,
    price = price,
    ammoType = ammoType,
    updatedAt = Instant.parse(updatedAt),
)

private fun SeedVehicleDto.toVehicle() = Vehicle(
    id = VehicleId(id),
    name = name,
    description = description,
    subType = VehicleType.valueOf(subType),
    zone = zone,
    updatedAt = Instant.parse(updatedAt),
)

private fun SeedItemDto.toItem() = Item(
    id = ItemId(id),
    name = name,
    description = description,
    subType = ItemType.valueOf(subType),
    zone = zone,
    updatedAt = Instant.parse(updatedAt),
)

private fun SeedSongDto.toSong() = Song(
    id = SongId(id),
    title = title,
    artist = artist,
    stationId = StationId(stationId),
    updatedAt = Instant.parse(updatedAt),
)

private fun SeedMapPinDto.toMapPin(): MapPin {
    val entryRef = when (category) {
        "WEAPON" -> EntryRef.ToWeapon(WeaponId(entryId))
        "VEHICLE" -> EntryRef.ToVehicle(VehicleId(entryId))
        "ITEM" -> EntryRef.ToItem(ItemId(entryId))
        else -> error("Seed data has an unknown map_pins.category \"$category\" for pin \"$id\".")
    }
    return MapPin(
        id = MapPinId(id),
        entryRef = entryRef,
        position = NormalizedPoint(x, y),
        zone = zone,
        mapVersion = mapVersion,
        updatedAt = Instant.parse(updatedAt),
    )
}

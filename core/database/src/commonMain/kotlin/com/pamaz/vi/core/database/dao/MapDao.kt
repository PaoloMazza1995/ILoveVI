package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.asListFlow
import com.pamaz.vi.core.database.asOneOrNullFlow
import com.pamaz.vi.core.database.mapper.mapConfigRowToMapConfig
import com.pamaz.vi.core.database.mapper.mapPinRowToMapPin
import com.pamaz.vi.core.database.mapper.pinJoinRowToMapPinWithEntry
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/** A [MapPin] paired with the display name of the entry it marks, so callers don't need a second catalog lookup just to label it. */
data class MapPinWithEntryName(val pin: MapPin, val entryName: String)

interface MapDao {
    fun currentMapConfig(): Flow<MapConfig?>

    /**
     * Pins for the given [categories], optionally narrowed to [subTypes] (the weapon/vehicle/item
     * sub-type enum names relevant to whichever categories were requested). An empty or null
     * [subTypes] means "no sub-type filter" for every requested category.
     */
    fun pins(
        categories: Set<EntryCategory>,
        subTypes: Set<String>? = null
    ): Flow<List<MapPinWithEntryName>>

    fun pinByEntryId(category: EntryCategory, entryId: String): Flow<MapPin?>

    suspend fun upsertMapConfig(config: MapConfig)
    suspend fun upsertPin(pin: MapPin)
    suspend fun tombstonePin(id: MapPinId, deletedAt: Instant, updatedAt: Instant)
    suspend fun tombstoneMapConfig(mapVersion: Int, deletedAt: Instant, updatedAt: Instant)
}

internal class MapDaoImpl(database: IloveViDatabase) : MapDao {
    private val mapConfigQueries = database.mapConfigQueries
    private val mapPinsQueries = database.mapPinsQueries

    override fun currentMapConfig(): Flow<MapConfig?> =
        mapConfigQueries.selectCurrent(::mapConfigRowToMapConfig).asOneOrNullFlow()

    override fun pins(
        categories: Set<EntryCategory>,
        subTypes: Set<String>?
    ): Flow<List<MapPinWithEntryName>> {
        val flows = categories.map { category -> pinsForCategory(category, subTypes) }
        if (flows.isEmpty()) return flowOf(emptyList())
        return combine(flows) { lists -> lists.toList().flatten() }
    }

    private fun pinsForCategory(
        category: EntryCategory,
        subTypes: Set<String>?
    ): Flow<List<MapPinWithEntryName>> {
        val query = when (category) {
            EntryCategory.WEAPON ->
                if (subTypes.isNullOrEmpty()) mapPinsQueries.weaponPinsAll(::pinJoinRowToMapPinWithEntry)
                else mapPinsQueries.weaponPinsBySubTypes(
                    subTypes.toList(),
                    ::pinJoinRowToMapPinWithEntry
                )

            EntryCategory.VEHICLE ->
                if (subTypes.isNullOrEmpty()) mapPinsQueries.vehiclePinsAll(::pinJoinRowToMapPinWithEntry)
                else mapPinsQueries.vehiclePinsBySubTypes(
                    subTypes.toList(),
                    ::pinJoinRowToMapPinWithEntry
                )

            EntryCategory.ITEM ->
                if (subTypes.isNullOrEmpty()) mapPinsQueries.itemPinsAll(::pinJoinRowToMapPinWithEntry)
                else mapPinsQueries.itemPinsBySubTypes(
                    subTypes.toList(),
                    ::pinJoinRowToMapPinWithEntry
                )
        }
        return query.asListFlow()
            .map { rows -> rows.map { row -> MapPinWithEntryName(row.pin, row.entryName) } }
    }

    override fun pinByEntryId(category: EntryCategory, entryId: String): Flow<MapPin?> =
        mapPinsQueries.pinByEntryId(category.name, entryId, ::mapPinRowToMapPin).asOneOrNullFlow()

    override suspend fun upsertMapConfig(config: MapConfig) {
        mapConfigQueries.upsert(
            mapVersion = config.mapVersion.toLong(),
            sourceImageUrl = config.sourceImageUrl,
            widthPx = config.widthPx.toLong(),
            heightPx = config.heightPx.toLong(),
            tileUrlTemplate = config.tileUrlTemplate,
            minZoom = config.minZoom.toLong(),
            maxZoom = config.maxZoom.toLong(),
            updatedAt = config.updatedAt.toEpochMilliseconds(),
            deletedAt = null,
        )
    }

    override suspend fun upsertPin(pin: MapPin) {
        mapPinsQueries.upsert(
            id = pin.id.value,
            category = pin.category.name,
            entryId = pin.entryRef.entryIdValue(),
            x = pin.position.x.toDouble(),
            y = pin.position.y.toDouble(),
            zone = pin.zone,
            mapVersion = pin.mapVersion.toLong(),
            updatedAt = pin.updatedAt.toEpochMilliseconds(),
            deletedAt = null,
        )
    }

    override suspend fun tombstonePin(id: MapPinId, deletedAt: Instant, updatedAt: Instant) {
        mapPinsQueries.tombstone(
            deletedAt.toEpochMilliseconds(),
            updatedAt.toEpochMilliseconds(),
            id.value
        )
    }

    override suspend fun tombstoneMapConfig(
        mapVersion: Int,
        deletedAt: Instant,
        updatedAt: Instant
    ) {
        mapConfigQueries.tombstone(
            deletedAt.toEpochMilliseconds(),
            updatedAt.toEpochMilliseconds(),
            mapVersion.toLong()
        )
    }
}

private fun EntryRef.entryIdValue(): String = when (this) {
    is EntryRef.ToWeapon -> id.value
    is EntryRef.ToVehicle -> id.value
    is EntryRef.ToItem -> id.value
}

package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.database.dao.MapDao
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The current map image/tiles and pins — read-only, see `CatalogRepository`'s KDoc for why this
 * wraps `:core:database` rather than a screen depending on it directly. [MapPinWithEntryName] is
 * this module's own type (see [RepositoryTypes.kt]'s KDoc for why it isn't `:core:database`'s).
 */
interface MapRepository {
    fun currentMapConfig(): Flow<MapConfig?>

    /** Pins for [categories], optionally narrowed to [subTypes] (an empty/null set means no sub-type filter). */
    fun pins(
        categories: Set<EntryCategory>,
        subTypes: Set<String>? = null
    ): Flow<List<MapPinWithEntryName>>

    fun pinByEntryId(category: EntryCategory, entryId: String): Flow<MapPin?>
}

internal class MapRepositoryImpl(private val mapDao: MapDao) : MapRepository {
    override fun currentMapConfig(): Flow<MapConfig?> = mapDao.currentMapConfig()

    override fun pins(
        categories: Set<EntryCategory>,
        subTypes: Set<String>?
    ): Flow<List<MapPinWithEntryName>> =
        mapDao.pins(categories, subTypes).map { rows ->
            rows.map { row -> MapPinWithEntryName(row.pin, row.entryName) }
        }

    override fun pinByEntryId(category: EntryCategory, entryId: String): Flow<MapPin?> =
        mapDao.pinByEntryId(category, entryId)
}

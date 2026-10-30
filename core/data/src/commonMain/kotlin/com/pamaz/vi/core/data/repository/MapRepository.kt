package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.database.dao.MapDao
import com.pamaz.vi.core.database.dao.MapPinWithEntryName
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import kotlinx.coroutines.flow.Flow

/**
 * The current map image/tiles and pins — read-only, see `CatalogRepository`'s KDoc for why this
 * wraps `:core:database` rather than a screen depending on it directly. [MapPinWithEntryName] is
 * reused as-is from `:core:database` (same rationale as `MusicRepository`'s `Compilation` reuse).
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
        mapDao.pins(categories, subTypes)

    override fun pinByEntryId(category: EntryCategory, entryId: String): Flow<MapPin?> =
        mapDao.pinByEntryId(category, entryId)
}

package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.database.dao.MusicDao
import com.pamaz.vi.core.model.Song
import com.pamaz.vi.core.model.SongId
import com.pamaz.vi.core.model.Station
import com.pamaz.vi.core.model.StationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Stations/songs (read-only, synced content) and compilations (local-only, user-owned) — see
 * `CatalogRepository`'s KDoc for why this wraps `:core:database` rather than a screen depending on
 * it directly. [Compilation]/[CompilationSongEntry] are this module's own types (see
 * [RepositoryTypes.kt]'s KDoc for why they aren't `:core:database`'s).
 */
interface MusicRepository {
    fun stations(): Flow<List<Station>>
    fun songs(): Flow<List<Song>>
    fun songsByStation(stationId: StationId): Flow<List<Song>>
    fun song(id: SongId): Flow<Song?>

    /** [stationId]/[query] null means "no filter on that axis"; [query] is matched case/accent-insensitively. */
    fun searchSongs(stationId: StationId?, query: String?): Flow<List<Song>>

    fun compilations(): Flow<List<Compilation>>
    fun compilation(id: String): Flow<Compilation?>
    fun songsForCompilation(compilationId: String): Flow<List<CompilationSongEntry>>

    /** Creates a new compilation (a fresh, locally-generated id) and returns it. */
    suspend fun createCompilation(name: String, spotifyPlaylistId: String? = null): String
    suspend fun renameCompilation(id: String, name: String)
    suspend fun deleteCompilation(id: String)

    /** Appends [songId] at the end of the compilation's current order. */
    suspend fun addSongToCompilation(compilationId: String, songId: SongId)
    suspend fun removeSongFromCompilation(compilationId: String, songId: SongId)

    /** Replaces the compilation's whole song order with [orderedSongIds]. */
    suspend fun reorderCompilation(compilationId: String, orderedSongIds: List<SongId>)
}

internal class MusicRepositoryImpl(private val musicDao: MusicDao) : MusicRepository {
    override fun stations(): Flow<List<Station>> = musicDao.stations()
    override fun songs(): Flow<List<Song>> = musicDao.songs()
    override fun songsByStation(stationId: StationId): Flow<List<Song>> =
        musicDao.songsByStation(stationId)

    override fun song(id: SongId): Flow<Song?> = musicDao.song(id)

    override fun searchSongs(stationId: StationId?, query: String?): Flow<List<Song>> =
        musicDao.searchSongs(stationId, query)

    override fun compilations(): Flow<List<Compilation>> =
        musicDao.compilations().map { list -> list.map { it.toRepositoryCompilation() } }

    override fun compilation(id: String): Flow<Compilation?> =
        musicDao.compilation(id).map { it?.toRepositoryCompilation() }

    override fun songsForCompilation(compilationId: String): Flow<List<CompilationSongEntry>> =
        musicDao.songsForCompilation(compilationId)
            .map { entries -> entries.map { CompilationSongEntry(it.song, it.position) } }

    override suspend fun createCompilation(name: String, spotifyPlaylistId: String?): String {
        val id = "compilation-${Clock.System.now().toEpochMilliseconds()}"
        musicDao.createCompilation(id, name, Clock.System.now(), spotifyPlaylistId)
        return id
    }

    override suspend fun renameCompilation(id: String, name: String) =
        musicDao.renameCompilation(id, name)

    override suspend fun deleteCompilation(id: String) = musicDao.deleteCompilation(id)

    override suspend fun addSongToCompilation(compilationId: String, songId: SongId) =
        musicDao.addSongToCompilation(compilationId, songId)

    override suspend fun removeSongFromCompilation(compilationId: String, songId: SongId) =
        musicDao.removeSongFromCompilation(compilationId, songId)

    override suspend fun reorderCompilation(compilationId: String, orderedSongIds: List<SongId>) =
        musicDao.reorderCompilation(compilationId, orderedSongIds)
}

private fun com.pamaz.vi.core.database.dao.Compilation.toRepositoryCompilation() = Compilation(
    id = id,
    name = name,
    createdAt = createdAt,
    spotifyPlaylistId = spotifyPlaylistId,
)

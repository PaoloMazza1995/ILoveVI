package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.asListFlow
import com.pamaz.vi.core.database.asOneOrNullFlow
import com.pamaz.vi.core.database.foldForSearch
import com.pamaz.vi.core.database.mapper.songJoinRowToSong
import com.pamaz.vi.core.database.mapper.songsForCompilationRowToEntry
import com.pamaz.vi.core.database.mapper.stationRowToStation
import com.pamaz.vi.core.model.Song
import com.pamaz.vi.core.model.SongId
import com.pamaz.vi.core.model.Station
import com.pamaz.vi.core.model.StationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

/** One ordered entry in a compilation's song list. */
data class CompilationSongEntry(val song: Song, val position: Int)

interface MusicDao {
    fun stations(): Flow<List<Station>>
    fun songs(): Flow<List<Song>>
    fun songsByStation(stationId: StationId): Flow<List<Song>>
    fun song(id: SongId): Flow<Song?>

    /** [stationId]/[query] null means "no filter on that axis"; [query] is matched case/accent-insensitively. */
    fun searchSongs(stationId: StationId?, query: String?): Flow<List<Song>>

    suspend fun upsertStation(station: Station)
    suspend fun upsertSong(song: Song)
    suspend fun tombstoneStation(id: StationId, deletedAt: Instant, updatedAt: Instant)
    suspend fun tombstoneSong(id: SongId, deletedAt: Instant, updatedAt: Instant)

    fun compilations(): Flow<List<Compilation>>
    fun compilation(id: String): Flow<Compilation?>
    fun songsForCompilation(compilationId: String): Flow<List<CompilationSongEntry>>

    suspend fun createCompilation(id: String, name: String, createdAt: Instant, spotifyPlaylistId: String? = null)
    suspend fun renameCompilation(id: String, name: String)
    suspend fun setCompilationSpotifyPlaylistId(id: String, spotifyPlaylistId: String?)
    suspend fun deleteCompilation(id: String)

    /** Appends [songId] at the end of the compilation's current order. */
    suspend fun addSongToCompilation(compilationId: String, songId: SongId)
    suspend fun removeSongFromCompilation(compilationId: String, songId: SongId)

    /** Replaces the compilation's whole song order with [orderedSongIds]. */
    suspend fun reorderCompilation(compilationId: String, orderedSongIds: List<SongId>)
}

data class Compilation(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val spotifyPlaylistId: String?,
)

internal class MusicDaoImpl(private val database: IloveViDatabase) : MusicDao {
    private val stationsQueries = database.stationsQueries
    private val songsQueries = database.songsQueries
    private val compilationsQueries = database.compilationsQueries
    private val compilationSongsQueries = database.compilationSongsQueries

    override fun stations(): Flow<List<Station>> =
        stationsQueries.selectAll(::stationRowToStation).asListFlow()

    override fun songs(): Flow<List<Song>> =
        songsQueries.selectAll(::songJoinRowToSong).asListFlow()

    override fun songsByStation(stationId: StationId): Flow<List<Song>> =
        songsQueries.selectByStation(stationId.value, ::songJoinRowToSong).asListFlow()

    override fun song(id: SongId): Flow<Song?> =
        songsQueries.selectById(id.value, ::songJoinRowToSong).asOneOrNullFlow()

    override fun searchSongs(stationId: StationId?, query: String?): Flow<List<Song>> =
        songsQueries.search(stationId?.value, query?.foldForSearch(), ::songJoinRowToSong).asListFlow()

    override suspend fun upsertStation(station: Station) {
        stationsQueries.upsert(station.id.value, station.name, station.updatedAt.toEpochMilliseconds(), null)
    }

    override suspend fun upsertSong(song: Song) {
        songsQueries.upsert(
            id = song.id.value,
            title = song.title,
            artist = song.artist,
            stationId = song.stationId.value,
            spotifyTrackId = song.spotifyTrackId,
            searchKey = songSearchKeyFor(song.title, song.artist),
            updatedAt = song.updatedAt.toEpochMilliseconds(),
            deletedAt = null,
        )
    }

    override suspend fun tombstoneStation(id: StationId, deletedAt: Instant, updatedAt: Instant) {
        stationsQueries.tombstone(deletedAt.toEpochMilliseconds(), updatedAt.toEpochMilliseconds(), id.value)
    }

    override suspend fun tombstoneSong(id: SongId, deletedAt: Instant, updatedAt: Instant) {
        songsQueries.tombstone(deletedAt.toEpochMilliseconds(), updatedAt.toEpochMilliseconds(), id.value)
    }

    override fun compilations(): Flow<List<Compilation>> =
        compilationsQueries.selectAll(::compilationRowToCompilation).asListFlow()

    override fun compilation(id: String): Flow<Compilation?> =
        compilationsQueries.selectById(id, ::compilationRowToCompilation).asOneOrNullFlow()

    override fun songsForCompilation(compilationId: String): Flow<List<CompilationSongEntry>> =
        compilationSongsQueries.songsForCompilation(compilationId, ::songsForCompilationRowToEntry)
            .asListFlow()
            .map { entries -> entries.map { CompilationSongEntry(it.song, it.position) } }

    override suspend fun createCompilation(id: String, name: String, createdAt: Instant, spotifyPlaylistId: String?) {
        compilationsQueries.insert(id, name, createdAt.toEpochMilliseconds(), spotifyPlaylistId)
    }

    override suspend fun renameCompilation(id: String, name: String) {
        compilationsQueries.updateName(name, id)
    }

    override suspend fun setCompilationSpotifyPlaylistId(id: String, spotifyPlaylistId: String?) {
        compilationsQueries.updateSpotifyPlaylistId(spotifyPlaylistId, id)
    }

    override suspend fun deleteCompilation(id: String) {
        compilationSongsQueries.clearCompilation(id)
        compilationsQueries.delete(id)
    }

    override suspend fun addSongToCompilation(compilationId: String, songId: SongId) {
        database.transaction {
            val maxPosition = compilationSongsQueries.maxPosition(compilationId).executeAsOne().MAX
            compilationSongsQueries.addSong(compilationId, songId.value, (maxPosition ?: -1L) + 1L)
        }
    }

    override suspend fun removeSongFromCompilation(compilationId: String, songId: SongId) {
        compilationSongsQueries.removeSong(compilationId, songId.value)
    }

    override suspend fun reorderCompilation(compilationId: String, orderedSongIds: List<SongId>) {
        database.transaction {
            orderedSongIds.forEachIndexed { index, songId ->
                compilationSongsQueries.updatePosition(index.toLong(), compilationId, songId.value)
            }
        }
    }
}

private fun compilationRowToCompilation(
    id: String,
    name: String,
    createdAt: Long,
    spotifyPlaylistId: String?,
): Compilation = Compilation(
    id = id,
    name = name,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    spotifyPlaylistId = spotifyPlaylistId,
)

private fun songSearchKeyFor(title: String, artist: String): String =
    "$title $artist".foldForSearch()

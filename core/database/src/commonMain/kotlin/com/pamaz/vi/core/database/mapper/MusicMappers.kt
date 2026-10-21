package com.pamaz.vi.core.database.mapper

import com.pamaz.vi.core.model.Song
import com.pamaz.vi.core.model.SongId
import com.pamaz.vi.core.model.Station
import com.pamaz.vi.core.model.StationId
import kotlin.time.Instant

internal fun stationRowToStation(
    id: String,
    name: String,
    updatedAt: Long,
    deletedAt: Long?,
): Station = Station(
    id = StationId(id),
    name = name,
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

/**
 * Shared by every song+station join query (selectAll/selectByStation/search/selectById): they
 * all project the same 12 columns (song's own 8, then the joined station's 4, suffixed `_` by
 * SQLDelight to avoid the name clash on id/updatedAt/deletedAt), so one mapper fits them all.
 */
internal fun songJoinRowToSong(
    id: String,
    title: String,
    artist: String,
    stationId: String,
    spotifyTrackId: String?,
    searchKey: String,
    updatedAt: Long,
    deletedAt: Long?,
    stationId_: String,
    stationName: String,
    stationUpdatedAt: Long,
    stationDeletedAt: Long?,
): Song = Song(
    id = SongId(id),
    title = title,
    artist = artist,
    stationId = StationId(stationId),
    spotifyTrackId = spotifyTrackId,
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

internal data class CompilationSongEntry(val song: Song, val position: Int)

internal fun songsForCompilationRowToEntry(
    id: String,
    title: String,
    artist: String,
    stationId: String,
    spotifyTrackId: String?,
    searchKey: String,
    updatedAt: Long,
    deletedAt: Long?,
    stationId_: String,
    stationName: String,
    stationUpdatedAt: Long,
    stationDeletedAt: Long?,
    position: Long,
): CompilationSongEntry = CompilationSongEntry(
    song = songJoinRowToSong(
        id, title, artist, stationId, spotifyTrackId, searchKey, updatedAt, deletedAt,
        stationId_, stationName, stationUpdatedAt, stationDeletedAt,
    ),
    position = position.toInt(),
)

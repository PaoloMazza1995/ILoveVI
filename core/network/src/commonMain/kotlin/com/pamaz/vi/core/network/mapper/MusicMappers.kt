package com.pamaz.vi.core.network.mapper

import com.pamaz.vi.core.model.Song
import com.pamaz.vi.core.model.SongId
import com.pamaz.vi.core.model.Station
import com.pamaz.vi.core.model.StationId
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.dto.SongDto
import com.pamaz.vi.core.network.dto.StationDto
import kotlin.time.Instant

/** Maps a `stations` row. */
internal fun StationDto.toChange(): Change<Station> = rowChange(id, deletedAt) {
    Station(id = StationId(id), name = name, updatedAt = Instant.parse(updatedAt))
}

/** Maps a `songs` row. */
internal fun SongDto.toChange(): Change<Song> = rowChange(id, deletedAt) {
    Song(
        id = SongId(id),
        title = title,
        artist = artist,
        stationId = StationId(stationId),
        spotifyTrackId = spotifyTrackId,
        updatedAt = Instant.parse(updatedAt),
    )
}

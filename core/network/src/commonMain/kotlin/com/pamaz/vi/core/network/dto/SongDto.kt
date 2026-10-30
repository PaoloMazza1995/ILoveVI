package com.pamaz.vi.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The `songs` row shape — `backend/supabase/migrations/20261011120003_stations_and_songs.sql`. */
@Serializable
internal data class SongDto(
    val id: String,
    val title: String,
    val artist: String,
    @SerialName("station_id") val stationId: String,
    @SerialName("spotify_track_id") val spotifyTrackId: String? = null,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

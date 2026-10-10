package com.pamaz.vi.core.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

/** Typed id of a [Song]. Serializes as a plain JSON string, not as an object. */
@JvmInline
@Serializable
value class SongId(val value: String)

/**
 * A song on a [Station]'s playlist. No lyrics, no audio and no third-party artwork URL live
 * here: this model only carries the metadata needed to identify the track and, optionally,
 * match it on Spotify.
 */
@Serializable
data class Song(
    val id: SongId,
    /** Track title, as written in the game: not translated in this model. */
    val title: String,
    val artist: String,
    val stationId: StationId,
    /** Cached id of the matched Spotify track, when a match has been found. */
    val spotifyTrackId: String? = null,
    /** Last time this song's data changed, used for delta sync. */
    val updatedAt: Instant,
)

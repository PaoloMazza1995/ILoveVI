package com.pamaz.vi.core.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Typed id of a [Station]. Serializes as a plain JSON string, not as an object. */
@JvmInline
@Serializable
value class StationId(val value: String)

/** A radio station from the game's soundtrack. */
@Serializable
data class Station(
    val id: StationId,
    /** Station name, as written in the game: not translated in this model. */
    val name: String,
    /** Last time this station's data changed, used for delta sync. */
    val updatedAt: kotlin.time.Instant,
)

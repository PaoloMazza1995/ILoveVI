package com.pamaz.vi.core.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/** Typed id of a [Vehicle]. Serializes as a plain JSON string, not as an object. */
@JvmInline
@Serializable
value class VehicleId(val value: String)

/**
 * Vehicle sub-type. Generic placeholder categories, easy to change once the real GTA VI vehicle
 * classification is known.
 */
@Serializable
enum class VehicleType { CAR, MOTORCYCLE, BOAT, HELICOPTER, PLANE }

/**
 * A vehicle in the catalog/map.
 */
@Serializable
data class Vehicle(
    val id: VehicleId,
    override val name: String,
    /** Flavor/description text, as written in the game: not translated in this model. */
    val description: String,
    val subType: VehicleType,
    override val zone: String? = null,
    override val imageUrl: String? = null,
    /** Free-form extra key/value pairs (e.g. "top speed", "seats") not worth their own field yet. */
    val attributes: Map<String, String> = emptyMap(),
    override val updatedAt: kotlin.time.Instant,
) : Entry {
    override val entryId: String get() = id.value
    override val category: EntryCategory get() = EntryCategory.VEHICLE
}

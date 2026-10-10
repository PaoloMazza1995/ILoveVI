package com.pamaz.vi.core.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

/** Typed id of a [Weapon]. Serializes as a plain JSON string, not as an object. */
@JvmInline
@Serializable
value class WeaponId(val value: String)

/**
 * Weapon sub-type. Generic placeholder categories, easy to change once the real GTA VI weapon
 * classification is known.
 */
@Serializable
enum class WeaponType { PISTOL, RIFLE, MACHINE_GUN, MELEE, EXPLOSIVE }

/**
 * A weapon in the catalog/map.
 */
@Serializable
data class Weapon(
    val id: WeaponId,
    override val name: String,
    /** Flavor/description text, as written in the game: not translated in this model. */
    override val description: String,
    val subType: WeaponType,
    override val zone: String? = null,
    override val imageUrl: String? = null,
    /** Purchase price, when the weapon can be bought. */
    val price: Int? = null,
    /** e.g. "9mm", "shells" — free text rather than an enum, since the real taxonomy is unknown yet. */
    val ammoType: String? = null,
    /** Free-form extra key/value pairs (e.g. "magazine size", "fire rate") not worth their own field yet. */
    val attributes: Map<String, String> = emptyMap(),
    override val updatedAt: Instant,
) : Entry {
    override val entryId: String get() = id.value
    override val category: EntryCategory get() = EntryCategory.WEAPON
}

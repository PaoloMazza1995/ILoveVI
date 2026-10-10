package com.pamaz.vi.core.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.time.Instant

/** Typed id of an [Item]. Serializes as a plain JSON string, not as an object. */
@JvmInline
@Serializable
value class ItemId(val value: String)

/**
 * Item sub-type. Generic placeholder categories, easy to change once the real GTA VI item
 * classification is known.
 */
@Serializable
enum class ItemType { HEALTH, AMMO, ARMOR }

/**
 * A pickup item in the catalog/map.
 */
@Serializable
data class Item(
    val id: ItemId,
    override val name: String,
    /** Flavor/description text, as written in the game: not translated in this model. */
    override val description: String,
    val subType: ItemType,
    override val zone: String? = null,
    override val imageUrl: String? = null,
    /** Free-form extra key/value pairs (e.g. "heal amount") not worth their own field yet. */
    val attributes: Map<String, String> = emptyMap(),
    override val updatedAt: Instant,
) : Entry {
    override val entryId: String get() = id.value
    override val category: EntryCategory get() = EntryCategory.ITEM
}

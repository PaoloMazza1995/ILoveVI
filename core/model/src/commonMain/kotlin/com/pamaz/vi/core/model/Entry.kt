package com.pamaz.vi.core.model

import kotlin.time.Instant

/**
 * Read-only, uniform view shared by [Weapon], [Vehicle] and [Item], so catalog/detail screens
 * can iterate over the three content types without branching on their concrete class. The three
 * data classes stay the source of truth: this interface adds no state of its own, only
 * properties each of them already forwards from its own fields.
 *
 * Naming note: each concrete class has its own typed id property literally named `id` (e.g.
 * [Weapon.id]: [WeaponId]), per this module's typed-id convention. A class can't also declare an
 * `override val id: String` under that same name with a different type, so the uniform string id
 * is exposed here as [entryId] instead (`weapon.entryId == weapon.id.value`).
 */
sealed interface Entry {
    /** The entry's typed id, as a plain string — see the class-level note on why this isn't called `id`. */
    val entryId: String

    /** Content name, as written in the game: not translated in this model. */
    val name: String

    /** Flavor/description text, as written in the game: not translated in this model. */
    val description: String

    /** Which of the three map/catalog categories this entry belongs to. */
    val category: EntryCategory

    /** In-game area/location name, when known. */
    val zone: String?

    /** Catalog image URL, when known. */
    val imageUrl: String?

    /** Last time this entry's data changed, used for delta sync. */
    val updatedAt: Instant
}

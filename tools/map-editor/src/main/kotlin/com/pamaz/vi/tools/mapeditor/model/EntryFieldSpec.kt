package com.pamaz.vi.tools.mapeditor.model

import com.pamaz.vi.core.model.EntryCategory

/**
 * One editable *category-specific* column of a `weapons`/`vehicles`/`items` row — [DraftEntry]'s
 * `name`/`subType`/`zone`/`description` are always present (dedicated properties, per the Map
 * Editor Functional Analysis's literal field list, EDT-54) and aren't part of this spec; this
 * covers only what differs per category, e.g. a weapon's price/ammo type.
 */
sealed interface EntryFieldSpec {
    val key: String
    val label: String
    val required: Boolean

    data class Text(
        override val key: String,
        override val label: String,
        override val required: Boolean = false,
    ) : EntryFieldSpec

    data class IntNumber(
        override val key: String,
        override val label: String,
        override val required: Boolean = false,
    ) : EntryFieldSpec
}

/**
 * The single place a weapons/vehicles/items *extra* column is added: [extraFieldsFor] is what
 * both the inline entry form and [validateExtra] iterate over, built directly from
 * `backend/supabase/migrations/20261011120002_weapons_vehicles_items.sql`'s real columns.
 *
 * Per that migration, **only weapons have category-specific columns** (`price`, `ammo_type`) —
 * vehicles and items have no equivalent dedicated columns (only the generic `attributes` jsonb,
 * which this form doesn't expose — the Functional Analysis's field list for the new-entry form
 * never mentions a free-form attributes editor, only "the extra fields of the category"). This
 * directly answers the Functional Analysis's own open question #1 (which proposes "price and
 * ammo type for weapons, price and seats for vehicles" as a guess): there is no real `price`/
 * `seats` column on `vehicles` in this repo's migrations, so vehicles/items simply have an empty
 * extra-fields list here, not placeholder columns that don't exist in the database.
 */
object EntryFields {
    const val PRICE = "price"
    const val AMMO_TYPE = "ammoType"

    fun extraFieldsFor(category: EntryCategory): List<EntryFieldSpec> = when (category) {
        EntryCategory.WEAPON -> listOf(
            EntryFieldSpec.IntNumber(PRICE, "Price"),
            EntryFieldSpec.Text(AMMO_TYPE, "Ammo type"),
        )

        EntryCategory.VEHICLE -> emptyList()
        EntryCategory.ITEM -> emptyList()
    }

    /** Keys of every field in [extra] that fails its own [EntryFieldSpec] — empty means every extra field is valid (all are optional today, but a future required one is already handled). */
    fun validateExtra(category: EntryCategory, extra: Map<String, String>): List<String> =
        extraFieldsFor(category).mapNotNull { spec ->
            val value = extra[spec.key]
            val invalid = when (spec) {
                is EntryFieldSpec.Text -> spec.required && value.isNullOrBlank()
                is EntryFieldSpec.IntNumber ->
                    if (spec.required) value.isNullOrBlank() || value.toIntOrNull() == null
                    else !value.isNullOrBlank() && value.toIntOrNull() == null
            }
            spec.key.takeIf { invalid }
        }
}

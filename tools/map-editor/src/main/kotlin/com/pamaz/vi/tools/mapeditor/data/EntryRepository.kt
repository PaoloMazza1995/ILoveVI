package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.tools.mapeditor.model.EntrySummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.serialization.Serializable

@Serializable
private data class EntryRow(val id: String, val name: String, val zone: String? = null)

/** Loads every non-deleted weapon/vehicle/item as an [EntrySummary] (PLT-03's left panel). Read-only, no RLS/admin requirement — `anon`/`authenticated` can always select. */
class EntryRepository(private val client: SupabaseClient) {
    private suspend fun loadTable(table: String): List<EntryRow> =
        client.from(table).select(columns = Columns.list("id", "name", "zone")) {
            filter { filter("deleted_at", FilterOperator.IS, "null") }
        }.decodeList()

    suspend fun loadAll(): List<EntrySummary> {
        val weapons = loadTable("weapons").map {
            EntrySummary(
                EntryRef.ToWeapon(WeaponId(it.id)),
                it.name,
                it.zone
            )
        }
        val vehicles = loadTable("vehicles").map {
            EntrySummary(
                EntryRef.ToVehicle(VehicleId(it.id)),
                it.name,
                it.zone
            )
        }
        val items = loadTable("items").map {
            EntrySummary(
                EntryRef.ToItem(ItemId(it.id)),
                it.name,
                it.zone
            )
        }
        return weapons + vehicles + items
    }
}

package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.tools.mapeditor.model.EntrySummary
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class EntryRow(
    val id: String,
    val name: String,
    val zone: String? = null,
    @SerialName("sub_type") val subType: String,
)

/** Loads every non-deleted weapon/vehicle/item as an [EntrySummary] (PLT-03's left panel; also the step 5B extension's existing-entry picker/duplicate guard). Read-only, no RLS/admin requirement — `anon`/`authenticated` can always select. Its `loadAll()` already matches [com.pamaz.vi.tools.mapeditor.data.EntryRepository] exactly, so it implements that interface directly rather than needing a wrapper adapter. */
class EntryRepository(private val client: SupabaseClient) :
    com.pamaz.vi.tools.mapeditor.data.EntryRepository {
    private suspend fun loadTable(table: String): List<EntryRow> =
        client.from(table).select(columns = Columns.list("id", "name", "zone", "sub_type")) {
            filter { filter("deleted_at", FilterOperator.IS, "null") }
        }.decodeList()

    override suspend fun loadAll(): List<EntrySummary> {
        val weapons = loadTable("weapons").map {
            EntrySummary(
                EntryRef.ToWeapon(WeaponId(it.id)),
                it.name,
                it.zone,
                it.subType,
            )
        }
        val vehicles = loadTable("vehicles").map {
            EntrySummary(
                EntryRef.ToVehicle(VehicleId(it.id)),
                it.name,
                it.zone,
                it.subType,
            )
        }
        val items = loadTable("items").map {
            EntrySummary(
                EntryRef.ToItem(ItemId(it.id)),
                it.name,
                it.zone,
                it.subType,
            )
        }
        return weapons + vehicles + items
    }
}

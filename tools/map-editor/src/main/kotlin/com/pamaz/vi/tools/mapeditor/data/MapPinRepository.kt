package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.core.model.MapPin
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Reads/writes `map_pins`. Reads work for anyone (RLS: `anon`/`authenticated` select, including
 * soft-deleted rows); every write here requires the caller to already hold a confirmed
 * [com.pamaz.vi.tools.mapeditor.auth.AdminSession] — enforced for real by `is_admin()`/RLS on the
 * server, this repository just never exposes a write method the UI could call without one (see
 * `MapEditorState`, the only caller).
 */
@OptIn(ExperimentalTime::class)
class MapPinRepository(private val client: SupabaseClient) {
    /** Every non-deleted pin for [mapVersion], or every non-deleted pin across all versions when `null` — used by the "all pins" list (PLT-05). */
    suspend fun loadPins(mapVersion: Int? = null): List<MapPin> =
        client.from("map_pins").select {
            filter {
                filter("deleted_at", FilterOperator.IS, "null")
                if (mapVersion != null) eq("map_version", mapVersion)
            }
        }.decodeList<MapPinRow>().mapNotNull { it.toMapPin() }

    /** Upserts [pins] by their natural key `(category, entry_id)` — PLT-07's save, and PLT-06's import-after-confirmation. */
    suspend fun upsertAll(pins: List<MapPin>) {
        if (pins.isEmpty()) return
        client.from("map_pins").upsert(pins.map { it.toUpsertRow() }) {
            onConflict = "category,entry_id"
        }
    }

    /** Soft-deletes one pin (PLT-04's Delete key): sets `deleted_at`, never a real `DELETE`. */
    suspend fun softDelete(pinId: String) {
        client.from("map_pins").update({
            set("deleted_at", Clock.System.now().toString())
        }) {
            filter { eq("id", pinId) }
        }
    }
}

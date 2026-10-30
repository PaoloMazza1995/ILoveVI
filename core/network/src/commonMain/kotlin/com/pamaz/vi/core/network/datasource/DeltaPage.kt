package com.pamaz.vi.core.network.datasource

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlin.time.Instant

/**
 * The one delta-sync query every remote data source in this module issues, following
 * `backend/README.md`'s "Delta sync convention" literally:
 *
 * ```
 * GET /rest/v1/<table>?select=*&updated_at=gt.<since>&order=updated_at.asc.nullslast,<idColumn>.asc.nullslast&limit=<pageSize>
 * ```
 *
 * [since] is omitted from the filter entirely when `null` (first sync — every row, including
 * already-tombstoned ones, is wanted). [idColumn] is `"id"` for every table except `map_config`,
 * whose primary key is `map_version` (see `MapConfigRemoteDataSource`). Soft-deleted rows are
 * never filtered out here — `deleted_at` isn't part of this query at all — exactly because the
 * convention is "a row with `deleted_at` set means remove this row locally, not ignore it."
 *
 * This one call returns a single page; whether the caller needs another one is `items.size ==
 * pageSize` (a short page means there's nothing left), re-calling with `since` set to the last
 * returned row's `updated_at` to continue — the same keyset-ish re-paging the backend README
 * itself describes as sufficient for this convention.
 */
internal suspend inline fun <reified Dto> fetchDeltaPage(
    client: SupabaseClient,
    table: String,
    since: Instant?,
    pageSize: Int,
    idColumn: String = "id",
): List<Dto> = client.from(table).select {
    if (since != null) {
        filter { gt("updated_at", since.toString()) }
    }
    order("updated_at", Order.ASCENDING)
    order(idColumn, Order.ASCENDING)
    limit(pageSize.toLong())
}.decodeList()

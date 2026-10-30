package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.tools.mapeditor.model.EntrySummary

/** Reads every non-deleted weapon/vehicle/item — the existing-entry picker (EDT-52) and duplicate guard (EDT-55). */
interface EntryRepository {
    suspend fun loadAll(): List<EntrySummary>
}

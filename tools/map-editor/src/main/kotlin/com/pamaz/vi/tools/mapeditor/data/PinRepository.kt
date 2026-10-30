package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.tools.mapeditor.model.DbPin

/** Reads every non-deleted pin already in the database — the read-only overlay (EDT-11). */
interface PinRepository {
    suspend fun loadAll(): List<DbPin>
}

package com.pamaz.vi.tools.mapeditor.model

import com.pamaz.vi.core.model.EntryRef

/**
 * One row in the left panel's entry list (PLT-03): the minimum a placement session needs from a
 * [com.pamaz.vi.core.model.Weapon]/[com.pamaz.vi.core.model.Vehicle]/[com.pamaz.vi.core.model.Item]
 * — not the full entity (no description/attributes/price, nothing the map tool doesn't show).
 */
data class EntrySummary(
    val entryRef: EntryRef,
    val name: String,
    val zone: String?,
)

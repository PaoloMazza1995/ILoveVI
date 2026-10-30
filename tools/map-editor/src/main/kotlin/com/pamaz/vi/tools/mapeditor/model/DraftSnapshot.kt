package com.pamaz.vi.tools.mapeditor.model

/** Everything a [com.pamaz.vi.tools.mapeditor.data.DraftStore] persists — the whole local draft (EDT-61). */
data class DraftSnapshot(
    val pins: List<DraftPin> = emptyList(),
    val entries: List<DraftEntry> = emptyList(),
)

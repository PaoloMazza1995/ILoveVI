package com.pamaz.vi.tools.mapeditor.model

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint

/**
 * A pin already saved in the database — read-only (Map Editor Functional Analysis EDT-11:
 * "dimmed... not clickable"). [mapVersion] is which [com.pamaz.vi.core.model.MapConfig] it was
 * placed on — PLT-08's version decision (`state/MapPublisher.kt`) counts how many pins are still
 * on the *current* map version to decide "update in place" vs. "new version," and how many are on
 * an *older* one to warn "need repositioning."
 */
data class DbPin(
    val id: String,
    val category: EntryCategory,
    val entryId: String,
    val position: NormalizedPoint,
    val zone: String? = null,
    val mapVersion: Int = 1,
)

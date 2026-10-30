package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.tools.mapeditor.model.DraftSnapshot

/** Autosave (EDT-61): [save] is called after every draft change; [load] restores it on startup. */
interface DraftStore {
    /** Empty on a fresh install or a damaged file — never throws (see `FileDraftStore`'s damaged-file handling). */
    fun load(): DraftSnapshot
    fun save(snapshot: DraftSnapshot)
    fun clear()
}

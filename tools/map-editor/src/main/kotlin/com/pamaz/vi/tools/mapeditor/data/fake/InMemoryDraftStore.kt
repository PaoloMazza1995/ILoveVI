package com.pamaz.vi.tools.mapeditor.data.fake

import com.pamaz.vi.tools.mapeditor.data.DraftStore
import com.pamaz.vi.tools.mapeditor.model.DraftSnapshot

/** No disk I/O at all — for tests and `@Preview`s that want a draft/`EditorState` with no file-system side effects. */
class InMemoryDraftStore(initial: DraftSnapshot = DraftSnapshot()) : DraftStore {
    private var current = initial

    override fun load(): DraftSnapshot = current
    override fun save(snapshot: DraftSnapshot) {
        current = snapshot
    }

    override fun clear() {
        current = DraftSnapshot()
    }
}

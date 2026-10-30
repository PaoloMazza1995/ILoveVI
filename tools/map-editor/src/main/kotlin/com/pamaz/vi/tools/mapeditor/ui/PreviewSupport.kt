package com.pamaz.vi.tools.mapeditor.ui

import com.pamaz.vi.tools.mapeditor.data.fake.FakeAuthService
import com.pamaz.vi.tools.mapeditor.data.fake.FakeCommitService
import com.pamaz.vi.tools.mapeditor.data.fake.FakeEntryRepository
import com.pamaz.vi.tools.mapeditor.data.fake.FakeMapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.fake.FakeMapPublisher
import com.pamaz.vi.tools.mapeditor.data.fake.FakeMapTileChecker
import com.pamaz.vi.tools.mapeditor.data.fake.FakePinRepository
import com.pamaz.vi.tools.mapeditor.data.fake.InMemoryDraftStore
import com.pamaz.vi.tools.mapeditor.model.MapPublishStep
import com.pamaz.vi.tools.mapeditor.state.EditorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/** One `EditorState` wired entirely to fakes — shared by every `@Preview` in this package so each one isn't rebuilding the same wiring. */
internal fun previewEditorState(forceMapPublishFailureAtStep: MapPublishStep? = null): EditorState {
    val mapConfigRepository = FakeMapConfigRepository()
    return EditorState(
        entryRepository = FakeEntryRepository(),
        pinRepository = FakePinRepository(),
        mapConfigRepository = mapConfigRepository,
        authService = FakeAuthService(),
        commitService = FakeCommitService(delayMs = 0),
        draftStore = InMemoryDraftStore(),
        mapTileChecker = FakeMapTileChecker(),
        mapPublisher = FakeMapPublisher(
            mapConfigRepository,
            FakePinRepository(),
            forceMapPublishFailureAtStep
        ),
        scope = CoroutineScope(Dispatchers.Unconfined),
    )
}

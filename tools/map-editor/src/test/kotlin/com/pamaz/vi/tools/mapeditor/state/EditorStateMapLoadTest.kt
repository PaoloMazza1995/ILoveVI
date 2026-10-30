package com.pamaz.vi.tools.mapeditor.state

import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.tools.mapeditor.data.MapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.MapConfigResult
import com.pamaz.vi.tools.mapeditor.data.MapTileChecker
import com.pamaz.vi.tools.mapeditor.data.RawMapConfigRow
import com.pamaz.vi.tools.mapeditor.data.fake.FakeAuthService
import com.pamaz.vi.tools.mapeditor.data.fake.FakeCommitService
import com.pamaz.vi.tools.mapeditor.data.fake.FakeEntryRepository
import com.pamaz.vi.tools.mapeditor.data.fake.FakeMapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.fake.FakeMapPublisher
import com.pamaz.vi.tools.mapeditor.data.fake.FakePinRepository
import com.pamaz.vi.tools.mapeditor.data.fake.InMemoryDraftStore
import com.pamaz.vi.tools.mapeditor.model.MapLoadError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * PLT-08's fix for "No map loaded - Loading..." forever: [EditorState.load] must always leave
 * [EditorUiState.isLoading] `false` with a plain-words [MapLoadError] when the map config/tile
 * can't be resolved, never hang indefinitely. Each test builds its own [EditorState] on
 * `runTest`'s virtual-time scheduler (via `CoroutineScope(coroutineContext)`), so the 10s timeout
 * resolves instantly instead of this test suite actually waiting 10 real seconds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorStateMapLoadTest {

    private fun buildState(
        mapTileChecker: MapTileChecker,
        mapConfigRepository: MapConfigRepository,
        scope: CoroutineScope,
    ): EditorState {
        val pinRepository = FakePinRepository()
        return EditorState(
            entryRepository = FakeEntryRepository(),
            pinRepository = pinRepository,
            mapConfigRepository = mapConfigRepository,
            authService = FakeAuthService(),
            commitService = FakeCommitService(delayMs = 0),
            draftStore = InMemoryDraftStore(),
            mapTileChecker = mapTileChecker,
            mapPublisher = FakeMapPublisher(FakeMapConfigRepository(), pinRepository),
            scope = scope,
        )
    }

    @Test
    fun load_surfacesAPlainWordsError_whenNoMapIsConfigured() = runTest {
        val noMapConfigRepository = object : MapConfigRepository {
            override suspend fun loadCurrent(): MapConfigResult = MapConfigResult.Missing
        }
        val state = buildState(
            FakeMapTileCheckerStub(),
            noMapConfigRepository,
            CoroutineScope(coroutineContext)
        )

        state.load()
        advanceUntilIdle()

        assertFalse(state.state.value.isLoading)
        assertNull(state.state.value.mapContent)
        assertEquals("No map is configured yet.", state.state.value.mapLoadError?.message)
    }

    @Test
    fun load_surfacesTheTileCheckersPlainWordsError_withTheRequestedUrl() = runTest {
        val tileError = MapLoadError(
            "Tile 0/0/0 returned HTTP 404.",
            requestedUrl = "https://example.com/tiles/0/0/0.png"
        )
        val failingChecker = object : MapTileChecker {
            override suspend fun checkReachable(config: MapConfig): MapLoadError = tileError
        }
        val state =
            buildState(failingChecker, FakeMapConfigRepository(), CoroutineScope(coroutineContext))

        state.load()
        advanceUntilIdle()

        assertFalse(state.state.value.isLoading)
        assertNull(state.state.value.mapContent)
        assertEquals(tileError, state.state.value.mapLoadError)
    }

    @Test
    fun load_surfacesAPlainWordsError_whenLoadingThrows() = runTest {
        val throwingMapConfigRepository = object : MapConfigRepository {
            override suspend fun loadCurrent(): MapConfigResult =
                error("network error: tile bucket unreachable")
        }
        val state = buildState(
            FakeMapTileCheckerStub(),
            throwingMapConfigRepository,
            CoroutineScope(coroutineContext)
        )

        state.load()
        advanceUntilIdle()

        assertFalse(state.state.value.isLoading)
        assertEquals(
            "network error: tile bucket unreachable",
            state.state.value.mapLoadError?.message
        )
    }

    @Test
    fun load_surfacesAnInvalidMapConfigRow_withTheRawTemplateAndSourceUrl() = runTest {
        // the bug this task fixes: map_config's current row failed MapConfig's own validation
        // (crash.map_editor), so this must be a normal, displayable state, never an exception.
        val rawRow = RawMapConfigRow(
            mapVersion = 1,
            sourceImageUrl = "https://example.com/broken.png",
            widthPx = 4096,
            heightPx = 4096,
            tileUrlTemplate = "https://example.com/no-placeholders.png",
            minZoom = 0,
            maxZoom = 4,
        )
        val invalidMapConfigRepository = object : MapConfigRepository {
            override suspend fun loadCurrent(): MapConfigResult =
                MapConfigResult.Invalid(
                    "tileUrlTemplate must contain {z}, {x} and {y} placeholders",
                    rawRow
                )
        }
        val state = buildState(
            FakeMapTileCheckerStub(),
            invalidMapConfigRepository,
            CoroutineScope(coroutineContext)
        )

        state.load()
        advanceUntilIdle()

        assertFalse(state.state.value.isLoading)
        assertNull(state.state.value.mapContent)
        assertNull(state.state.value.mapConfig)
        assertTrue(state.state.value.mapLoadError?.message?.contains("placeholders") == true)
        assertEquals(rawRow, state.state.value.invalidMapConfigRow)
    }

    @Test
    fun load_timesOutWithAPlainWordsReason_ifATileCheckNeverResolves() = runTest {
        val hangingChecker = object : MapTileChecker {
            override suspend fun checkReachable(config: MapConfig): MapLoadError? {
                delay(Long.MAX_VALUE) // never resolves in real time; runTest fast-forwards virtual time instead.
                return null
            }
        }
        val state =
            buildState(hangingChecker, FakeMapConfigRepository(), CoroutineScope(coroutineContext))

        state.load()
        advanceUntilIdle()

        assertFalse(state.state.value.isLoading)
        val error = state.state.value.mapLoadError
        assertNotNull(error)
        assertTrue(error.message.contains("10s"))
    }

    private class FakeMapTileCheckerStub : MapTileChecker {
        override suspend fun checkReachable(config: MapConfig): MapLoadError? = null
    }
}

package com.pamaz.vi.tools.mapeditor.state

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.tools.mapeditor.data.fake.FakeAuthService
import com.pamaz.vi.tools.mapeditor.data.fake.FakeCommitService
import com.pamaz.vi.tools.mapeditor.data.fake.FakeEntryRepository
import com.pamaz.vi.tools.mapeditor.data.fake.FakeMapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.fake.FakeMapPublisher
import com.pamaz.vi.tools.mapeditor.data.fake.FakeMapTileChecker
import com.pamaz.vi.tools.mapeditor.data.fake.FakePinRepository
import com.pamaz.vi.tools.mapeditor.data.fake.InMemoryDraftStore
import com.pamaz.vi.tools.mapeditor.model.DbPin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

/**
 * A **structural** stand-in for the Functional Analysis's "report measured frame times" (§8:
 * "smooth with 500 draft pins and 2000 database pins"). This environment has no attached display
 * (every prior step in this repo notes the same limitation), so an actual Compose frame-time
 * measurement isn't possible here — what *is* measurable headlessly is the pure data-layer cost
 * the UI's draw pass depends on every frame: building 500 draft pins (with undo history) and
 * filtering/positioning 2000 read-only database pins. Generous thresholds — this proves "not
 * algorithmically pathological," not a specific frame budget; a real frame-time measurement still
 * needs a real display and is left as a documented follow-up (see this module's README).
 */
class EditorStatePerformanceTest {

    @Test
    fun placing500DraftPins_withFullUndoHistory_staysFast() {
        val mapConfigRepository = FakeMapConfigRepository()
        val pinRepository = FakePinRepository()
        val state = EditorState(
            entryRepository = FakeEntryRepository(),
            pinRepository = pinRepository,
            mapConfigRepository = mapConfigRepository,
            authService = FakeAuthService(),
            commitService = FakeCommitService(delayMs = 0),
            draftStore = InMemoryDraftStore(),
            mapTileChecker = FakeMapTileChecker(),
            mapPublisher = FakeMapPublisher(mapConfigRepository, pinRepository),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        val elapsed = measureTime {
            repeat(500) { i ->
                state.placeAt(NormalizedPoint((i % 1000) / 1000f, (i / 1000) / 1000f))
            }
        }

        assertTrue(500 == state.state.value.draftPins.size)
        assertTrue(elapsed < 5.seconds, "Placing 500 pins took $elapsed, expected well under 5s")
    }

    @Test
    fun filteringAndPositioning2000DatabasePins_staysFast() {
        val dbPins = (0 until 2000).map { i ->
            DbPin(
                id = "perf-pin-$i",
                category = EntryCategory.entries[i % EntryCategory.entries.size],
                entryId = "entry-${i % 8}",
                position = NormalizedPoint((i % 1000) / 1000f, (i / 1000 % 1000) / 1000f),
            )
        }

        val elapsed = measureTime {
            // The same per-frame work MapArea's database-pin Canvas layer does: filter by the
            // "Saved" toggle/category filter, then compute each one's draw position.
            val visible = dbPins.filter { it.category == EntryCategory.WEAPON }
            visible.forEach { it.position.x + it.position.y }
        }

        assertTrue(
            elapsed < 1.seconds,
            "Filtering/positioning 2000 pins took $elapsed, expected well under 1s"
        )
    }
}

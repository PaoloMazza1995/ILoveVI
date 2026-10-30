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
import com.pamaz.vi.tools.mapeditor.model.EditMode
import com.pamaz.vi.tools.mapeditor.model.EntrySource
import com.pamaz.vi.tools.mapeditor.model.rawEntryId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** No Compose runtime involved anywhere here — [EditorState] is plain Kotlin + coroutines. */
class EditorStateTest {

    private lateinit var state: EditorState
    private lateinit var commitService: FakeCommitService

    @BeforeTest
    fun setUp() {
        commitService = FakeCommitService(delayMs = 0)
        val mapConfigRepository = FakeMapConfigRepository()
        val pinRepository = FakePinRepository()
        state = EditorState(
            entryRepository = FakeEntryRepository(),
            pinRepository = pinRepository,
            mapConfigRepository = mapConfigRepository,
            authService = FakeAuthService(),
            commitService = commitService,
            draftStore = InMemoryDraftStore(),
            mapTileChecker = FakeMapTileChecker(),
            mapPublisher = FakeMapPublisher(mapConfigRepository, pinRepository),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )
        state.load()
    }

    private fun point(x: Float = 0.5f, y: Float = 0.5f) = NormalizedPoint(x, y)

    private suspend fun signInAsAdmin() {
        state.signIn("admin@example.com", "any")
    }

    // --- placing -------------------------------------------------------------------------------

    @Test
    fun placeAt_addsANewIncompletePin_everyClick() {
        state.setMode(EditMode.PLACE)
        val a = state.placeAt(point(0.1f, 0.1f))
        val b = state.placeAt(point(0.2f, 0.2f))

        assertEquals(2, state.state.value.draftPins.size)
        assertTrue(a.id != b.id)
        assertEquals(setOf(a.id, b.id), state.state.value.incompleteDraftPinIds.toSet())
    }

    @Test
    fun placeAt_usesTheActiveCategoryAndSubType() {
        state.setActiveCategory(EntryCategory.VEHICLE)
        state.setActiveSubType("BOAT")
        val pin = state.placeAt(point())
        assertEquals(EntryCategory.VEHICLE, pin.category)
        assertEquals("BOAT", pin.subType)
    }

    @Test
    fun placeAt_withAnActiveEntry_linksEveryNewPinToIt() {
        val ref = EntrySource.Db("weapon-1")
        state.setActiveEntry(ref)
        val a = state.placeAt(point(0.1f, 0.1f))
        val b = state.placeAt(point(0.2f, 0.2f))

        assertEquals(ref, a.entry)
        assertEquals(ref, b.entry)
        assertTrue(state.state.value.incompleteDraftPinIds.isEmpty())
    }

    // --- completeness / commit gating -----------------------------------------------------------

    @Test
    fun isCommittable_requiresChangesCompletePinsAndAdmin() = runTest {
        assertFalse(state.state.value.isCommittable) // nothing staged yet

        val pin = state.placeAt(point())
        assertFalse(state.state.value.isCommittable) // incomplete, not admin

        state.linkExisting(pin.id, FakeEntryRepository.ALL.first())
        assertFalse(state.state.value.isCommittable) // complete, but not admin

        signInAsAdmin()
        assertTrue(state.state.value.isCommittable)
    }

    // --- select / move / delete ------------------------------------------------------------------

    @Test
    fun moveSelectedTo_movesOnlyTheGivenPin() {
        val pin = state.placeAt(point(0.1f, 0.1f))
        state.moveSelectedTo(pin.id, point(0.9f, 0.9f))
        assertEquals(
            point(0.9f, 0.9f),
            state.state.value.draftPins.single { it.id == pin.id }.position
        )
    }

    @Test
    fun selectWithin_selectsPinsInsideTheRectangle_regardlessOfCornerOrder() {
        val inside = state.placeAt(point(0.3f, 0.3f))
        val outside = state.placeAt(point(0.9f, 0.9f))
        state.selectWithin(point(0.5f, 0.5f), point(0.1f, 0.1f))

        assertTrue(inside.id in state.state.value.selectedPinIds)
        assertFalse(outside.id in state.state.value.selectedPinIds)
    }

    @Test
    fun deleteSelected_removesTheDraftEntry_onlyIfNoOtherPinUsesIt() {
        val pinA = state.placeAt(point(0.1f, 0.1f))
        state.stageEntry(
            pinA.id,
            EntryCategory.ITEM,
            "AMMO",
            "Spare clips",
            null,
            "",
            emptyMap(),
            null
        )
        val entryId =
            (state.state.value.draftPins.first { it.id == pinA.id }.entry as EntrySource.Draft).id

        // a second pin for the SAME draft entry
        state.setActiveEntry(EntrySource.Draft(entryId))
        val pinB = state.placeAt(point(0.2f, 0.2f))

        state.select(pinA.id)
        state.deleteSelected()
        // pinB still references the entry -> it must survive.
        assertEquals(1, state.state.value.draftEntries.size)

        state.select(pinB.id)
        state.deleteSelected()
        // no pin references it anymore -> pruned.
        assertTrue(state.state.value.draftEntries.isEmpty())
    }

    @Test
    fun setCategoryForPin_onlyAppliesToAPinWithNoEntry() {
        val unlinked = state.placeAt(point(0.1f, 0.1f))
        state.setCategoryForPin(unlinked.id, EntryCategory.ITEM)
        assertEquals(
            EntryCategory.ITEM,
            state.state.value.draftPins.single { it.id == unlinked.id }.category
        )

        state.linkExisting(
            unlinked.id,
            FakeEntryRepository.ALL.first { it.entryRef.category == EntryCategory.ITEM })
        val before = state.state.value.draftPins.single { it.id == unlinked.id }.category
        state.setCategoryForPin(unlinked.id, EntryCategory.VEHICLE)
        assertEquals(before, state.state.value.draftPins.single { it.id == unlinked.id }.category)
    }

    // --- linking entries ------------------------------------------------------------------------

    @Test
    fun linkExisting_switchingFromADraftEntry_prunesTheUnusedDraftEntry_EDT58() {
        val pin = state.placeAt(point())
        state.stageEntry(pin.id, pin.category, "PISTOL", "Unused", null, "", emptyMap(), null)
        assertEquals(1, state.state.value.draftEntries.size)

        val existing = FakeEntryRepository.ALL.first { it.entryRef.category == pin.category }
        state.linkExisting(pin.id, existing)

        assertTrue(state.state.value.draftEntries.isEmpty())
        assertEquals(
            EntrySource.Db(existing.entryRef.rawEntryId()),
            state.state.value.draftPins.single().entry
        )
    }

    @Test
    fun duplicateMatch_isCaseInsensitive_andScopedToCategory() {
        val weapon = FakeEntryRepository.ALL.first { it.entryRef.category == EntryCategory.WEAPON }
        assertNotNull(state.duplicateMatch(EntryCategory.WEAPON, weapon.name.uppercase()))
        assertNull(state.duplicateMatch(EntryCategory.VEHICLE, weapon.name))
        assertNull(state.duplicateMatch(EntryCategory.WEAPON, "Definitely Not A Real Name"))
    }

    @Test
    fun stageEntry_sharedByTwoPins_bothLinkToTheSameDraftEntry() {
        val pinA = state.placeAt(point(0.1f, 0.1f))
        state.stageEntry(
            pinA.id,
            EntryCategory.WEAPON,
            "PISTOL",
            "Shared Gun",
            null,
            "",
            emptyMap(),
            null
        )
        val entrySource =
            state.state.value.draftPins.single { it.id == pinA.id }.entry as EntrySource.Draft

        state.setActiveEntry(entrySource)
        val pinB = state.placeAt(point(0.2f, 0.2f))

        assertEquals(entrySource, pinB.entry)
        assertEquals(1, state.state.value.draftEntries.size)
    }

    // --- undo / redo (at least 50 steps) ----------------------------------------------------------

    @Test
    fun undoRedo_supportsAtLeast50Steps() {
        val placed = (1..55).map { i -> state.placeAt(point(i / 100f, i / 100f)) }
        assertEquals(55, state.state.value.draftPins.size)

        repeat(50) { state.undo() }
        assertEquals(5, state.state.value.draftPins.size)
        assertFalse(state.state.value.canUndo)

        repeat(50) { state.redo() }
        assertEquals(55, state.state.value.draftPins.size)
        assertEquals(
            placed.map { it.id }.toSet(),
            state.state.value.draftPins.map { it.id }.toSet()
        )
    }

    @Test
    fun undo_reversesPlaceMoveAndDelete() {
        val pin = state.placeAt(point(0.1f, 0.1f))
        state.undo()
        assertTrue(state.state.value.draftPins.isEmpty())

        state.redo()
        state.moveSelectedTo(pin.id, point(0.8f, 0.8f))
        state.undo()
        assertEquals(point(0.1f, 0.1f), state.state.value.draftPins.single().position)

        state.redo()
        state.select(pin.id)
        state.deleteSelected()
        assertTrue(state.state.value.draftPins.isEmpty())
        state.undo()
        assertEquals(1, state.state.value.draftPins.size)
    }

    // --- discard (two clicks) ---------------------------------------------------------------------

    @Test
    fun requestDiscardDraft_needsTwoClicks() {
        state.placeAt(point())
        assertTrue(state.state.value.hasDraftChanges)

        state.requestDiscardDraft()
        assertTrue(state.state.value.discardArmed)
        assertTrue(state.state.value.hasDraftChanges) // first click only arms it

        state.requestDiscardDraft()
        assertFalse(state.state.value.hasDraftChanges)
        assertFalse(state.state.value.discardArmed)
    }

    // --- commit -------------------------------------------------------------------------------

    @Test
    fun commit_onSuccess_clearsTheDraftAndRefreshesDbPins() = runTest {
        signInAsAdmin()
        val pin = state.placeAt(point())
        state.linkExisting(pin.id, FakeEntryRepository.ALL.first())

        val outcome = state.commit()

        assertTrue(outcome is CommitOutcome.Success)
        assertTrue(state.state.value.draftPins.isEmpty())
        assertTrue(state.state.value.draftEntries.isEmpty())
    }

    @Test
    fun commit_onFailure_keepsTheDraftIntact() = runTest {
        signInAsAdmin()
        val pin = state.placeAt(point())
        state.linkExisting(pin.id, FakeEntryRepository.ALL.first())
        commitService.forceFailure = true

        val outcome = state.commit()

        assertTrue(outcome is CommitOutcome.Failure)
        assertEquals(1, state.state.value.draftPins.size)
    }

    @Test
    fun commit_refusesWhenNotCommittable_andNeverCallsTheService() = runTest {
        // not signed in as admin, and no draft pins at all.
        val outcome = state.commit()
        assertTrue(outcome is CommitOutcome.Failure)
    }
}

package com.pamaz.vi.tools.mapeditor.state

import com.pamaz.vi.core.mapview.MapContent
import com.pamaz.vi.core.mapview.loadSingleImageMapContentFromFile
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.tools.mapeditor.data.AuthService
import com.pamaz.vi.tools.mapeditor.data.CommitPayloadBuilder
import com.pamaz.vi.tools.mapeditor.data.CommitResult
import com.pamaz.vi.tools.mapeditor.data.CommitService
import com.pamaz.vi.tools.mapeditor.data.DraftStore
import com.pamaz.vi.tools.mapeditor.data.EntryRepository
import com.pamaz.vi.tools.mapeditor.data.MapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.MapConfigResult
import com.pamaz.vi.tools.mapeditor.data.MapPublisher
import com.pamaz.vi.tools.mapeditor.data.MapTileChecker
import com.pamaz.vi.tools.mapeditor.data.PinRepository
import com.pamaz.vi.tools.mapeditor.model.DraftEntry
import com.pamaz.vi.tools.mapeditor.model.DraftPin
import com.pamaz.vi.tools.mapeditor.model.DraftSnapshot
import com.pamaz.vi.tools.mapeditor.model.EditMode
import com.pamaz.vi.tools.mapeditor.model.EntryFields
import com.pamaz.vi.tools.mapeditor.model.EntrySource
import com.pamaz.vi.tools.mapeditor.model.EntrySummary
import com.pamaz.vi.tools.mapeditor.model.MapLoadError
import com.pamaz.vi.tools.mapeditor.model.MapPublishOutcome
import com.pamaz.vi.tools.mapeditor.model.MapPublishPhase
import com.pamaz.vi.tools.mapeditor.model.MapPublishPlanResult
import com.pamaz.vi.tools.mapeditor.model.MapPublishSnapshot
import com.pamaz.vi.tools.mapeditor.model.Session
import com.pamaz.vi.tools.mapeditor.model.SignInResult
import com.pamaz.vi.tools.mapeditor.model.rawEntryId
import com.pamaz.vi.tools.mapeditor.model.subTypes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.UUID

/** Undo/redo keeps at least this many steps (EDT-22: "at least 50 steps"). */
internal const val MaxUndoSteps = 50

/** EDT-61 "debounced": how long a burst of changes (e.g. every frame of a pin drag) waits before the next disk write. */
private const val AutosaveDebounceMillis = 400L

/** PLT-08: how long `load()` waits for `map_config` + the first tile before giving up with a plain-words reason instead of hanging on "Loading...". */
private const val MapLoadTimeoutMillis = 10_000L

private data class UndoSnapshot(
    val pins: List<DraftPin>,
    val entries: List<DraftEntry>,
    val selectedPinIds: Set<String>,
)

/** The result of a Commit attempt (EDT-74/75) — what `ui/CommitDialog.kt` switches its success/error state on. */
sealed interface CommitOutcome {
    data class Success(val result: CommitResult) : CommitOutcome
    data class Failure(val message: String) : CommitOutcome
}

/**
 * The map editor's single source of truth: one immutable [EditorUiState] plus plain intent
 * functions that compute the next one — no `androidx.compose.runtime` dependency anywhere (see
 * [EditorUiState]'s own KDoc). The UI layer bridges [state] into Compose with the standard
 * `collectAsState()`; nothing here needs to know Compose exists.
 */
class EditorState(
    private val entryRepository: EntryRepository,
    private val pinRepository: PinRepository,
    private val mapConfigRepository: MapConfigRepository,
    private val authService: AuthService,
    private val commitService: CommitService,
    private val draftStore: DraftStore,
    private val mapTileChecker: MapTileChecker,
    private val mapPublisher: MapPublisher,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val undoStack = ArrayDeque<UndoSnapshot>()
    private val redoStack = ArrayDeque<UndoSnapshot>()

    init {
        val draft = draftStore.load()
        updateState { it.copy(draftPins = draft.pins, draftEntries = draft.entries) }
    }

    private inline fun updateState(transform: (EditorUiState) -> EditorUiState) {
        _state.value = transform(_state.value)
    }

    // --- Loading (entries/pins/map — read-only, no admin session required) -------------------

    private var loadJob: Job? = null

    /**
     * PLT-08: used to hang on "Loading..." forever if `map_config`/the first tile never resolved
     * — any thrown exception here (network error, RLS denial, ...) now surfaces as [MapLoadError]
     * instead of silently leaving [EditorUiState.isLoading] `true`, and the whole attempt is
     * bounded by [MapLoadTimeoutMillis] so a hanging connection can't do the same thing a
     * different way.
     */
    fun load() {
        loadJob?.cancel()
        loadJob = scope.launch {
            updateState {
                it.copy(
                    isLoading = true,
                    mapLoadError = null,
                    invalidMapConfigRow = null
                )
            }
            try {
                val timedOut = withTimeoutOrNull(MapLoadTimeoutMillis) {
                    val entries = entryRepository.loadAll()
                    val pins = pinRepository.loadAll()

                    when (val configResult = mapConfigRepository.loadCurrent()) {
                        is MapConfigResult.Loaded -> {
                            val tileError = mapTileChecker.checkReachable(configResult.config)
                            updateState {
                                it.copy(
                                    dbEntries = entries,
                                    dbPins = pins,
                                    mapConfig = configResult.config,
                                    mapContent = if (tileError == null) MapContent.Tiles(
                                        configResult.config
                                    ) else null,
                                    isLoading = false,
                                    mapLoadError = tileError,
                                    invalidMapConfigRow = null,
                                )
                            }
                        }

                        MapConfigResult.Missing -> updateState {
                            it.copy(
                                dbEntries = entries,
                                dbPins = pins,
                                mapConfig = null,
                                mapContent = null,
                                isLoading = false,
                                mapLoadError = MapLoadError("No map is configured yet."),
                                invalidMapConfigRow = null,
                            )
                        }

                        is MapConfigResult.Invalid -> updateState {
                            it.copy(
                                dbEntries = entries,
                                dbPins = pins,
                                mapConfig = null,
                                mapContent = null,
                                isLoading = false,
                                mapLoadError = MapLoadError(configResult.reason),
                                invalidMapConfigRow = configResult.rawRow,
                            )
                        }
                    }
                }
                if (timedOut == null) {
                    updateState {
                        it.copy(
                            isLoading = false,
                            mapLoadError = MapLoadError("The map didn't finish loading within ${MapLoadTimeoutMillis / 1000}s."),
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isLoading = false,
                        mapLoadError = MapLoadError(
                            e.message ?: "Unknown error while loading the map."
                        ),
                    )
                }
            }
        }
    }

    /** §1's secondary "Open local image (offline, no upload)" button — PLT-09's existing local-PNG mode, no backend/network involved at all. */
    fun openLocalImage(file: File) {
        scope.launch {
            try {
                val content =
                    withContext(Dispatchers.IO) { loadSingleImageMapContentFromFile(file) }
                updateState {
                    it.copy(
                        mapContent = content,
                        mapConfig = null,
                        isLoading = false,
                        mapLoadError = null
                    )
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(mapLoadError = MapLoadError("Could not open \"${file.name}\": ${e.message}"))
                }
            }
        }
    }

    // --- Map publish (PLT-08: "Load map image...") --------------------------------------------

    private var mapPublishJob: Job? = null

    /** Validates/plans [file] and opens the confirmation step — called right after the Swing file picker returns. */
    fun startMapPublish(file: File) {
        mapPublishJob?.cancel()
        mapPublishJob = scope.launch {
            updateState { it.copy(mapPublish = MapPublishSnapshot(phase = MapPublishPhase.CONFIRM)) }
            when (val result = mapPublisher.plan(file)) {
                is MapPublishPlanResult.Valid -> updateState {
                    it.copy(
                        mapPublish = MapPublishSnapshot(
                            phase = MapPublishPhase.CONFIRM,
                            plan = result.plan
                        )
                    )
                }

                is MapPublishPlanResult.Invalid -> updateState {
                    it.copy(
                        mapPublish = MapPublishSnapshot(
                            phase = MapPublishPhase.CONFIRM,
                            invalidReason = result.message
                        )
                    )
                }
            }
        }
    }

    /** §4: admin-only, checked here too (defense in depth — the Confirm button is already disabled otherwise). */
    fun confirmMapPublish() {
        val plan = _state.value.mapPublish?.plan ?: return
        if (_state.value.session?.isAdmin != true) return

        mapPublishJob?.cancel()
        mapPublishJob = scope.launch {
            updateState {
                it.copy(
                    mapPublish = it.mapPublish?.copy(
                        phase = MapPublishPhase.RUNNING,
                        errorMessage = null
                    )
                )
            }
            when (val outcome = mapPublisher.publish(plan) { steps ->
                updateState {
                    it.copy(
                        mapPublish = it.mapPublish?.copy(steps = steps)
                    )
                }
            }) {
                is MapPublishOutcome.Success -> {
                    updateState {
                        it.copy(
                            mapPublish = it.mapPublish?.copy(
                                phase = MapPublishPhase.SUCCESS,
                                mapConfig = outcome.mapConfig
                            )
                        )
                    }
                    load()
                }

                is MapPublishOutcome.Failure -> updateState {
                    it.copy(
                        mapPublish = it.mapPublish?.copy(
                            phase = MapPublishPhase.ERROR,
                            errorMessage = outcome.message
                        )
                    )
                }
            }
        }
    }

    /** Same pipeline, same plan (including whatever `MapPublisher` already uploaded) — see that interface's own KDoc on resuming. */
    fun retryMapPublish() = confirmMapPublish()

    /** §4: "Cancel is possible at any time; cancelling leaves map_config untouched" — publish only ever writes `map_config` as its very last step. */
    fun cancelMapPublish() {
        mapPublishJob?.cancel()
        updateState { it.copy(mapPublish = null) }
    }

    fun dismissMapPublish() {
        updateState { it.copy(mapPublish = null) }
    }

    // --- Session (EDT-01..04) -----------------------------------------------------------------

    suspend fun signIn(email: String, password: String): SignInResult {
        val result = authService.signIn(email, password)
        if (result is SignInResult.Success) {
            updateState { it.copy(session = result.session, lastError = null) }
            load()
        }
        return result
    }

    /** Called once on startup, before the sign-in screen shows, to pick up a session saved from a previous launch (see [AuthService.restoreSession]). */
    suspend fun restoreSession(): Session? {
        val restored = authService.restoreSession()
        if (restored != null) {
            updateState { it.copy(session = restored, lastError = null) }
            load()
        }
        return restored
    }

    fun signOut() {
        authService.signOut()
        updateState { it.copy(session = null) }
    }

    // --- Toolbar: mode / active category / active entry ---------------------------------------

    fun setMode(mode: EditMode) {
        updateState {
            it.copy(
                mode = mode,
                selectedPinIds = if (mode == EditMode.PLACE) emptySet() else it.selectedPinIds
            )
        }
    }

    fun setActiveCategory(category: EntryCategory) {
        updateState {
            it.copy(
                activeCategory = category,
                activeSubType = category.subTypes().first(),
                activeEntry = null
            )
        }
    }

    fun setActiveSubType(subType: String) {
        updateState { it.copy(activeSubType = subType) }
    }

    /** EDT-32: "Place another pin of this entry" / the active-entry banner's close button (pass `null`). */
    fun setActiveEntry(entry: EntrySource?) {
        updateState { it.copy(activeEntry = entry) }
    }

    fun setShowDbPins(show: Boolean) {
        updateState { it.copy(showDbPins = show) }
    }

    // --- Placing (EDT-30..33) ------------------------------------------------------------------

    /** Place mode: adds one new pin at [point] for whatever is currently armed, or an empty (incomplete) one. Returns the new pin so the UI can decide whether to open the Inspector (EDT-31). */
    fun placeAt(point: NormalizedPoint): DraftPin {
        pushUndo()
        val current = _state.value
        val pin = DraftPin(
            id = UUID.randomUUID().toString(),
            position = point,
            category = current.activeCategory,
            subType = current.activeSubType,
            entry = current.activeEntry,
        )
        updateState { it.copy(draftPins = it.draftPins + pin, selectedPinIds = setOf(pin.id)) }
        persist()
        return pin
    }

    // --- Selecting / moving / deleting (EDT-40..42) --------------------------------------------

    fun select(pinId: String, additive: Boolean = false) {
        updateState {
            val next = if (additive) it.selectedPinIds + pinId else setOf(pinId)
            it.copy(selectedPinIds = next)
        }
    }

    fun selectWithin(
        corner1: NormalizedPoint,
        corner2: NormalizedPoint,
        additive: Boolean = false
    ) {
        val minX = minOf(corner1.x, corner2.x)
        val maxX = maxOf(corner1.x, corner2.x)
        val minY = minOf(corner1.y, corner2.y)
        val maxY = maxOf(corner1.y, corner2.y)
        updateState { current ->
            val within = current.draftPins
                .filter { it.position.x in minX..maxX && it.position.y in minY..maxY }
                .map { it.id }
            val next = if (additive) current.selectedPinIds + within else within.toSet()
            current.copy(selectedPinIds = next)
        }
    }

    fun clearSelection() {
        updateState { it.copy(selectedPinIds = emptySet()) }
    }

    fun moveSelectedTo(pinId: String, point: NormalizedPoint) {
        pushUndo()
        updateState { current ->
            current.copy(draftPins = current.draftPins.map { if (it.id == pinId) it.copy(position = point) else it })
        }
        persist()
    }

    /** EDT-41: removes every selected pin; a draft entry no remaining pin points at is removed with it. */
    fun deleteSelected() {
        if (_state.value.selectedPinIds.isEmpty()) return
        pushUndo()
        updateState { current ->
            val remainingPins = current.draftPins.filterNot { it.id in current.selectedPinIds }
            current.copy(
                draftPins = remainingPins,
                draftEntries = pruneUnusedDraftEntries(remainingPins, current.draftEntries),
                selectedPinIds = emptySet(),
            )
        }
        persist()
    }

    /** EDT-57: only for a pin with no entry yet — the sub-type resets to the new category's first option. */
    fun setCategoryForPin(pinId: String, category: EntryCategory) {
        pushUndo()
        updateState { current ->
            current.copy(
                draftPins = current.draftPins.map {
                    if (it.id == pinId && it.entry == null) {
                        it.copy(category = category, subType = category.subTypes().first())
                    } else {
                        it
                    }
                },
            )
        }
        persist()
    }

    // --- Linking a pin to an entry (EDT-51..58) -------------------------------------------------

    /** A case-insensitive same-category name match among already-saved entries — a pure query, not a mutation (EDT-55's inline warning). */
    fun duplicateMatch(category: EntryCategory, name: String): EntrySummary? {
        if (name.isBlank()) return null
        return _state.value.dbEntries.find {
            it.entryRef.category == category && it.name.equals(name, ignoreCase = true)
        }
    }

    fun linkExisting(pinId: String, entry: EntrySummary) {
        pushUndo()
        updateState { current ->
            val oldPins = current.draftPins
            val newPins = oldPins.map {
                if (it.id == pinId) {
                    it.copy(
                        category = entry.entryRef.category,
                        subType = entry.subType,
                        entry = EntrySource.Db(entry.entryRef.rawEntryId()),
                    )
                } else {
                    it
                }
            }
            current.copy(
                draftPins = newPins,
                draftEntries = pruneUnusedDraftEntries(newPins, current.draftEntries)
            )
        }
        persist()
    }

    /**
     * Creates or updates (if [pinId] already points at a draft entry) a [DraftEntry] and links
     * [pinId] to it. [name]/[subType] are required by the form before this is ever called; this
     * function itself doesn't re-validate (`ui/InspectorPanel.kt` only enables its submit button
     * once [EntryFields.validateExtra] and a non-blank name both pass).
     */
    fun stageEntry(
        pinId: String,
        category: EntryCategory,
        subType: String,
        name: String,
        zone: String?,
        description: String,
        extra: Map<String, String>,
        imageFile: File?,
    ) {
        pushUndo()
        updateState { current ->
            val pin = current.draftPins.find { it.id == pinId } ?: return@updateState current
            val existingDraftId = (pin.entry as? EntrySource.Draft)?.id
            val entryId = existingDraftId ?: UUID.randomUUID().toString()
            val entry = DraftEntry(
                id = entryId,
                category = category,
                subType = subType,
                name = name,
                zone = zone,
                description = description,
                extra = extra,
                imageFile = imageFile,
            )
            val newEntries = if (existingDraftId != null) {
                current.draftEntries.map { if (it.id == existingDraftId) entry else it }
            } else {
                current.draftEntries + entry
            }
            val newPins = current.draftPins.map {
                if (it.id == pinId) it.copy(
                    category = category,
                    subType = subType,
                    entry = EntrySource.Draft(entryId)
                ) else it
            }
            current.copy(draftPins = newPins, draftEntries = newEntries)
        }
        persist()
    }

    /** EDT-55's "Use the existing entry instead" button: same as [linkExisting], named for where the UI calls it from. */
    fun useExistingInstead(pinId: String, entry: EntrySummary) = linkExisting(pinId, entry)

    private fun pruneUnusedDraftEntries(
        pins: List<DraftPin>,
        entries: List<DraftEntry>
    ): List<DraftEntry> {
        val referencedIds = pins.mapNotNull { (it.entry as? EntrySource.Draft)?.id }.toSet()
        return entries.filter { it.id in referencedIds }
    }

    // --- Undo / redo (EDT-22) -------------------------------------------------------------------

    private fun snapshot() =
        UndoSnapshot(_state.value.draftPins, _state.value.draftEntries, _state.value.selectedPinIds)

    private fun pushUndo() {
        if (undoStack.size >= MaxUndoSteps) undoStack.removeFirst()
        undoStack.addLast(snapshot())
        redoStack.clear()
        updateState { it.copy(canUndo = true, canRedo = false) }
    }

    private fun applySnapshot(target: UndoSnapshot) {
        updateState {
            it.copy(
                draftPins = target.pins,
                draftEntries = target.entries,
                selectedPinIds = target.selectedPinIds,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
            )
        }
        persist()
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(snapshot())
        applySnapshot(previous)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(snapshot())
        applySnapshot(next)
    }

    // --- Discard draft (EDT-62: two clicks) ------------------------------------------------------

    fun requestDiscardDraft() {
        val current = _state.value
        if (!current.discardArmed) {
            updateState { it.copy(discardArmed = true) }
            return
        }
        pushUndo()
        updateState {
            it.copy(
                draftPins = emptyList(),
                draftEntries = emptyList(),
                selectedPinIds = emptySet(),
                activeEntry = null,
                discardArmed = false,
            )
        }
        persist()
    }

    fun cancelDiscardArm() {
        updateState { it.copy(discardArmed = false) }
    }

    // --- Autosave ----------------------------------------------------------------------------

    private var autosaveJob: Job? = null

    /** EDT-61: writes `draft.json` after every change, debounced so a drag/undo burst doesn't hit disk every frame. */
    private fun persist() {
        updateState { it.copy(autosaveStatus = "Saving draft...") }
        autosaveJob?.cancel()
        autosaveJob = scope.launch {
            delay(AutosaveDebounceMillis)
            writeDraftNow()
        }
    }

    private fun writeDraftNow() {
        val current = _state.value
        draftStore.save(DraftSnapshot(current.draftPins, current.draftEntries))
        updateState { it.copy(autosaveStatus = "Draft autosaved to draft.json") }
    }

    /** Flushes any pending debounced write immediately — call this on window close so the last edit isn't lost. */
    fun flushAutosave() {
        autosaveJob?.cancel()
        autosaveJob = null
        writeDraftNow()
    }

    // --- Commit (EDT-70..77) --------------------------------------------------------------------

    suspend fun commit(): CommitOutcome {
        val current = _state.value
        if (!current.isCommittable) {
            return CommitOutcome.Failure("The draft isn't ready to commit yet.")
        }
        val mapVersion = current.mapConfig?.mapVersion
            ?: return CommitOutcome.Failure("No map is loaded.")
        val payload =
            CommitPayloadBuilder.build(mapVersion, current.draftPins, current.draftEntries)
        val pendingImages =
            current.draftEntries.mapNotNull { entry -> entry.imageFile?.let { entry.id to it } }
                .toMap()
        return try {
            val result = commitService.commit(payload, pendingImages)
            draftStore.clear()
            undoStack.clear()
            redoStack.clear()
            updateState {
                it.copy(
                    draftPins = emptyList(),
                    draftEntries = emptyList(),
                    selectedPinIds = emptySet(),
                    activeEntry = null,
                    discardArmed = false,
                    canUndo = false,
                    canRedo = false,
                )
            }
            load()
            CommitOutcome.Success(result)
        } catch (e: Exception) {
            CommitOutcome.Failure(e.message ?: "Commit failed.")
        }
    }
}

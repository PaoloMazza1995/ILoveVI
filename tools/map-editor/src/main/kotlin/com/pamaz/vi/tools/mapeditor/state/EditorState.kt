package com.pamaz.vi.tools.mapeditor.state

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.pamaz.vi.core.mapview.MapContent
import com.pamaz.vi.core.mapview.loadSingleImageMapContentFromFile
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.tools.mapeditor.auth.AdminAuthRepository
import com.pamaz.vi.tools.mapeditor.auth.AdminSession
import com.pamaz.vi.tools.mapeditor.auth.AdminSignInResult
import com.pamaz.vi.tools.mapeditor.data.EntryRepository
import com.pamaz.vi.tools.mapeditor.data.MapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.MapPinRepository
import com.pamaz.vi.tools.mapeditor.data.pinRowId
import com.pamaz.vi.tools.mapeditor.importer.ImportReport
import com.pamaz.vi.tools.mapeditor.importer.ImportValidator
import com.pamaz.vi.tools.mapeditor.importer.PinCsvIo
import com.pamaz.vi.tools.mapeditor.importer.PinJsonIo
import com.pamaz.vi.tools.mapeditor.importer.ValidatedPinRow
import com.pamaz.vi.tools.mapeditor.importer.toPinRow
import com.pamaz.vi.tools.mapeditor.model.EntrySummary
import com.pamaz.vi.tools.mapeditor.model.rawEntryId
import com.pamaz.vi.tools.mapeditor.model.toEntryRefOrNull
import com.pamaz.vi.tools.mapeditor.offline.OfflinePinStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Everything the left panel can filter the entry list by (PLT-03). */
enum class EntryFilter { ALL, NO_PIN, WEAPON, VEHICLE, ITEM }

/** One line in the always-up-to-date list, combining an [EntrySummary] with whether it already has a pin. */
data class EntryListItem(val summary: EntrySummary, val hasPin: Boolean)

/**
 * The map tool's single source of truth: what's armed, which pins exist and which of them are
 * dirty, whether we're online (signed in) or working offline from a local image (PLT-09). Plain
 * Compose state, not an Android `ViewModel` — this app has no Android lifecycle to tie one to.
 *
 * Every *write* path (`placeOrMovePin`, `deletePin`, `save`, `applyImport`) only ever queues an
 * in-memory change; nothing reaches the backend until [save] is called, and [save] itself no-ops
 * with [lastError] set if [adminSession] is `null` — "nothing is written without an admin
 * session" (acceptance) is enforced here, in addition to the server's own RLS.
 */
@OptIn(ExperimentalTime::class)
class EditorState(
    private val entryRepository: EntryRepository,
    private val mapPinRepository: MapPinRepository,
    private val mapConfigRepository: MapConfigRepository,
    private val authRepository: AdminAuthRepository,
) {
    private val _adminSession = MutableStateFlow<AdminSession?>(null)
    val adminSession = _adminSession.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    val entries = mutableStateListOf<EntrySummary>()

    /** Keyed by `(category, entry_id)` — matches `map_pins`' own unique constraint, so "does this entry have a pin" is a plain map lookup. */
    private val pinsByEntry = linkedMapOf<Pair<String, String>, MapPin>()
    val pins = mutableStateListOf<MapPin>()

    /** Entry ids present in [pinsByEntry] but belonging to a map_version older than [mapConfig]'s — "to reposition" (`:tools:tilegen`'s README). */
    val pinsToReposition = mutableStateListOf<MapPin>()

    private val dirtyPinKeys = mutableSetOf<Pair<String, String>>()
    private val deletedPinIds = mutableSetOf<String>()
    val hasUnsavedChanges get() = dirtyPinKeys.isNotEmpty() || deletedPinIds.isNotEmpty()

    var mapConfig = mutableStateOf<MapConfig?>(null)
        private set
    var mapContent = mutableStateOf<MapContent?>(null)
        private set

    /** `true` once "Open image" (PLT-09) was used instead of signing in — publishing is blocked until [adminSession] is set too. */
    var isOffline = mutableStateOf(false)
        private set
    private var offlineStore: OfflinePinStore? = null
    private var offlineMapVersion = 0

    var armedEntry = mutableStateOf<EntrySummary?>(null)
        private set
    var selectedPinKey = mutableStateOf<Pair<String, String>?>(null)
        private set

    private fun entryKey(ref: EntryRef) = ref.category.name to ref.rawEntryId()

    suspend fun loadEntries() {
        entries.clear()
        entries.addAll(entryRepository.loadAll())
    }

    suspend fun loadOnlineMap() {
        val config = mapConfigRepository.loadCurrent()
        mapConfig.value = config
        if (config != null) {
            mapContent.value = MapContent.Tiles(config)
            val allPins = mapPinRepository.loadPins()
            pinsByEntry.clear()
            allPins.forEach { pinsByEntry[entryKey(it.entryRef)] = it }
            refreshPinLists(config.mapVersion)
        }
    }

    private fun refreshPinLists(currentVersion: Int) {
        pins.clear()
        pins.addAll(pinsByEntry.values)
        pinsToReposition.clear()
        pinsToReposition.addAll(pinsByEntry.values.filter { it.mapVersion != currentVersion })
    }

    fun entryListItems(filter: EntryFilter, query: String): List<EntryListItem> = entries
        .asSequence()
        .filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        .map { EntryListItem(it, pinsByEntry.containsKey(entryKey(it.entryRef))) }
        .filter {
            when (filter) {
                EntryFilter.ALL -> true
                EntryFilter.NO_PIN -> !it.hasPin
                EntryFilter.WEAPON -> it.summary.entryRef.category == EntryCategory.WEAPON
                EntryFilter.VEHICLE -> it.summary.entryRef.category == EntryCategory.VEHICLE
                EntryFilter.ITEM -> it.summary.entryRef.category == EntryCategory.ITEM
            }
        }
        .toList()

    /** PLT-03: selecting an entry arms placement — the next map click/tap places or moves *this* entry's pin. */
    fun armEntry(entry: EntrySummary) {
        armedEntry.value = entry
    }

    /** PLT-04: a click creates the armed entry's pin at [point], or moves it if one already exists. */
    fun placeOrMoveArmedPinAt(point: NormalizedPoint) {
        val entry = armedEntry.value ?: return
        upsertLocalPin(entry.entryRef, point, entry.zone)
    }

    fun moveSelectedPin(point: NormalizedPoint) {
        val key = selectedPinKey.value ?: return
        val existing = pinsByEntry[key] ?: return
        upsertLocalPin(existing.entryRef, point, existing.zone)
    }

    private fun upsertLocalPin(entryRef: EntryRef, point: NormalizedPoint, zone: String?) {
        val key = entryKey(entryRef)
        val version = mapConfig.value?.mapVersion ?: offlineMapVersion
        val existing = pinsByEntry[key]
        val updated = MapPin(
            id = existing?.id ?: MapPinId(pinRowId(entryRef.category.name, entryRef.rawEntryId())),
            entryRef = entryRef,
            position = point,
            zone = zone,
            mapVersion = version,
            updatedAt = Clock.System.now(),
        )
        pinsByEntry[key] = updated
        dirtyPinKeys += key
        deletedPinIds -= updated.id.value
        selectedPinKey.value = key
        refreshPinLists(mapConfig.value?.mapVersion ?: offlineMapVersion)
    }

    fun selectPin(key: Pair<String, String>) {
        selectedPinKey.value = key
    }

    /** [pinId] is a [MapPin.id] value (what `MapViewerPin.id`/`onPinTap` carry) — looked up back to the `(category, entry_id)` key the rest of this class is keyed by. */
    fun selectPinById(pinId: String) {
        pinsByEntry.values.find { it.id.value == pinId }?.let { selectPin(entryKey(it.entryRef)) }
    }

    fun positionOfPin(pinId: String): NormalizedPoint? =
        pinsByEntry.values.find { it.id.value == pinId }?.position

    /** The currently selected [MapPin] (if any) — what drag/arrow-key nudging in [EditorScreen] move. */
    val selectedPin: MapPin? get() = selectedPinKey.value?.let { pinsByEntry[it] }

    /** PLT-04: Delete removes the selected pin — soft delete (backend) / a plain removal from the local offline file. */
    fun deleteSelectedPin() {
        val key = selectedPinKey.value ?: return
        val pin = pinsByEntry.remove(key) ?: return
        deletedPinIds += pin.id.value
        dirtyPinKeys -= key
        selectedPinKey.value = null
        refreshPinLists(mapConfig.value?.mapVersion ?: offlineMapVersion)
    }

    /** PLT-04: arrow-key nudge (and drag) land here once `core:mapview`'s `EditorGeometry` has computed the new point. */
    fun nudgeSelectedPinTo(point: NormalizedPoint) = moveSelectedPin(point)

    // --- Auth -------------------------------------------------------------------------------

    suspend fun signIn(email: String, password: String): AdminSignInResult {
        val result = authRepository.signIn(email, password)
        if (result is AdminSignInResult.Success) {
            _adminSession.value = result.session
            _lastError.value = null
        } else {
            _lastError.value = when (result) {
                is AdminSignInResult.InvalidCredentials -> result.message
                is AdminSignInResult.NotAnAdmin -> "${result.email} signed in but is not an admin."
                is AdminSignInResult.NetworkError -> result.message
                is AdminSignInResult.Success -> null
            }
        }
        return result
    }

    // --- Offline mode (PLT-09) ----------------------------------------------------------------

    fun openLocalImage(imageFile: File, storeFile: File, mapVersion: Int) {
        isOffline.value = true
        offlineMapVersion = mapVersion
        offlineStore = OfflinePinStore(storeFile)
        mapContent.value = loadSingleImageMapContentFromFile(imageFile)
        pinsByEntry.clear()
        offlineStore?.load()?.forEach { row ->
            val ref = row.toEntryRefOrNull() ?: return@forEach
            pinsByEntry[entryKey(ref)] = MapPin(
                id = MapPinId(pinRowId(row.category, row.entryId)),
                entryRef = ref,
                position = NormalizedPoint(row.x, row.y),
                zone = row.zone,
                mapVersion = row.mapVersion ?: mapVersion,
                updatedAt = Clock.System.now(),
            )
        }
        refreshPinLists(mapVersion)
    }

    /** Writes every current pin to the local offline file (not the backend — see [publishOfflinePins]). */
    fun saveOffline() {
        val store = offlineStore ?: return
        store.save(pinsByEntry.values.map { pin ->
            com.pamaz.vi.tools.mapeditor.model.PinRow(
                category = pin.entryRef.category.name,
                entryId = pin.entryRef.rawEntryId(),
                x = pin.position.x,
                y = pin.position.y,
                zone = pin.zone,
                mapVersion = pin.mapVersion,
            )
        })
        dirtyPinKeys.clear()
        deletedPinIds.clear()
        _statusMessage.value = "Saved to the local offline file."
    }

    /** PLT-09: publishes the offline file's pins to the backend once signed in — reuses the exact same validate → confirm → upsert path as a CSV/JSON import. */
    suspend fun publishOfflinePins(): ImportReport? {
        val store = offlineStore ?: return null
        if (_adminSession.value == null) {
            _lastError.value = "Sign in as an admin before publishing offline pins."
            return null
        }
        val rows = store.load()
        val known = entries.map { it.entryRef.category.name to it.entryRef.rawEntryId() }.toSet()
        return ImportValidator.validate(rows, known)
    }

    suspend fun confirmImport(validRows: List<ValidatedPinRow>) {
        val version = mapConfig.value?.mapVersion ?: offlineMapVersion
        val asPins = validRows.map { row ->
            MapPin(
                id = MapPinId(pinRowId(row.entryRef.category.name, row.entryRef.rawEntryId())),
                entryRef = row.entryRef,
                position = row.position,
                zone = row.zone,
                mapVersion = version,
                updatedAt = Clock.System.now(),
            )
        }
        mapPinRepository.upsertAll(asPins)
        asPins.forEach { pinsByEntry[entryKey(it.entryRef)] = it }
        refreshPinLists(version)
        _statusMessage.value = "Imported ${asPins.size} pin(s)."
    }

    // --- Import / export (PLT-06) ------------------------------------------------------------

    fun importReportFor(file: File): ImportReport {
        val rows = if (file.extension.equals("json", ignoreCase = true)) {
            PinJsonIo.read(file)
        } else {
            PinCsvIo.read(file)
        }
        val known = entries.map { it.entryRef.category.name to it.entryRef.rawEntryId() }.toSet()
        return ImportValidator.validate(rows, known)
    }

    fun exportTo(file: File) {
        val rows = pinsByEntry.values.map { pin ->
            ValidatedPinRow(pin.entryRef, pin.position, pin.zone).toPinRow()
        }
        if (file.extension.equals("json", ignoreCase = true)) PinJsonIo.write(
            file,
            rows
        ) else PinCsvIo.write(file, rows)
    }

    // --- New map version (PLT-08) ------------------------------------------------------------

    /** After `:tools:tilegen`'s pipeline registers a new `map_config` row: switch to it and re-flag every pin whose `map_version` isn't [newConfig]'s as "to reposition" — existing pins are never moved/revalidated automatically (`:tools:tilegen`'s README). */
    fun applyNewMapConfig(newConfig: MapConfig) {
        mapConfig.value = newConfig
        mapContent.value = MapContent.Tiles(newConfig)
        refreshPinLists(newConfig.mapVersion)
    }

    // --- Save (PLT-07) ------------------------------------------------------------------------

    /** Upserts every dirty pin and soft-deletes every removed one. No-ops (with [lastError] set) if there is no [adminSession] — "nothing is written without an admin session". */
    suspend fun save() {
        if (_adminSession.value == null) {
            _lastError.value = "Sign in as an admin before saving."
            return
        }
        val dirtyPins = dirtyPinKeys.mapNotNull { pinsByEntry[it] }
        if (dirtyPins.isNotEmpty()) mapPinRepository.upsertAll(dirtyPins)
        deletedPinIds.forEach { mapPinRepository.softDelete(it) }
        dirtyPinKeys.clear()
        deletedPinIds.clear()
        _statusMessage.value = "Saved."
    }
}

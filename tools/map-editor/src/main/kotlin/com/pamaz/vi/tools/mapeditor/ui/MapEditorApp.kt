package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.tools.mapeditor.di.MapEditorBackend
import com.pamaz.vi.tools.mapeditor.error.UncaughtErrorReporter
import com.pamaz.vi.tools.mapeditor.model.EntrySource
import com.pamaz.vi.tools.mapeditor.model.Session
import com.pamaz.vi.tools.mapeditor.model.rawEntryId
import com.pamaz.vi.tools.mapeditor.state.EditorState
import com.pamaz.vi.tools.mapeditor.state.EditorUiState

private enum class Screen { RESTORING_SESSION, SIGN_IN, NOT_ADMIN_NOTICE, EDITOR }

/**
 * Root composable: first a silent attempt to pick up a session saved from a previous launch
 * (`EditorState.restoreSession`), then sign-in, then (if signed in but not admin) the "not an
 * admin" notice, then the editor window (§4). Runs against fake services by default — [backend]
 * only affects the sign-in screen's "Running on fake data" hint (`SignInScreen`'s own KDoc); which
 * services actually get used is decided once, in `di/AppModule.kt`, independently of this
 * parameter.
 */
@Composable
fun MapEditorApp(state: EditorState, backend: MapEditorBackend = MapEditorBackend.FAKE) {
    val uiState by state.state.collectAsState()
    val uncaughtError by UncaughtErrorReporter.current.collectAsState()
    var screen by remember { mutableStateOf(Screen.RESTORING_SESSION) }
    var showCommitDialog by remember { mutableStateOf(false) }
    var showMapInfo by remember { mutableStateOf(false) }
    var justPlacedPinId by remember { mutableStateOf<String?>(null) }
    var cursorPosition by remember { mutableStateOf<NormalizedPoint?>(null) }

    LaunchedEffect(Unit) {
        val restored = state.restoreSession()
        screen = when {
            restored == null -> Screen.SIGN_IN
            restored.isAdmin -> Screen.EDITOR
            else -> Screen.NOT_ADMIN_NOTICE
        }
    }

    IloveViTheme {
        when (screen) {
            Screen.RESTORING_SESSION -> Box(
                Modifier.fillMaxSize().background(IloveViColors.bg900),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading...", color = IloveViColors.inkMuted)
            }

            Screen.SIGN_IN -> SignInScreen(
                onSignIn = { email, password -> state.signIn(email, password) },
                onSuccess = {
                    val session: Session? = state.state.value.session
                    screen =
                        if (session?.isAdmin == true) Screen.EDITOR else Screen.NOT_ADMIN_NOTICE
                },
                isFakeBackend = backend == MapEditorBackend.FAKE,
            )

            Screen.NOT_ADMIN_NOTICE -> NotAdminScreen(
                email = uiState.session?.email ?: "",
                onSignOut = { state.signOut(); screen = Screen.SIGN_IN },
                onContinue = { screen = Screen.EDITOR },
            )

            Screen.EDITOR -> Column(Modifier.fillMaxSize()) {
                Header(
                    uiState = uiState,
                    onCommitClick = { showCommitDialog = true },
                    onSignOutClick = { state.signOut(); screen = Screen.SIGN_IN },
                    onReplaceMapClick = {
                        FilePickers.pickOpenFile("Image", "png", "jpg", "jpeg")
                            ?.let(state::startMapPublish)
                    },
                    onMapInfoClick = { showMapInfo = true },
                )
                CategoryRow(
                    activeCategory = uiState.activeCategory,
                    activeSubType = uiState.activeSubType,
                    activeEntryLabel = activeEntryLabel(uiState),
                    onCategoryChange = state::setActiveCategory,
                    onSubTypeChange = state::setActiveSubType,
                    onClearActiveEntry = { state.setActiveEntry(null) },
                )
                Row(Modifier.fillMaxSize()) {
                    ToolRail(
                        mode = uiState.mode,
                        canUndo = uiState.canUndo,
                        canRedo = uiState.canRedo,
                        showDbPins = uiState.showDbPins,
                        onModeChange = state::setMode,
                        onUndo = state::undo,
                        onRedo = state::redo,
                        onToggleShowDbPins = { state.setShowDbPins(!uiState.showDbPins) },
                    )
                    Column(Modifier.weight(1f).fillMaxSize()) {
                        Row(Modifier.weight(1f)) {
                            MapArea(
                                uiState = uiState,
                                editorState = state,
                                onPinPlaced = { pinId -> justPlacedPinId = pinId },
                                onCursorMove = { cursorPosition = it },
                                modifier = Modifier.weight(1f),
                            )
                            RightPanel(
                                uiState = uiState,
                                editorState = state,
                                onPinPlaced = justPlacedPinId,
                                onConsumePinPlaced = { justPlacedPinId = null },
                            )
                        }
                        StatusBar(uiState = uiState, cursorPosition = cursorPosition)
                    }
                }
            }
        }

        if (showCommitDialog) {
            CommitDialog(
                uiState = uiState,
                editorState = state,
                onClose = { showCommitDialog = false })
        }
        if (showMapInfo) {
            MapInfoDialog(config = uiState.mapConfig, onClose = { showMapInfo = false })
        }
        if (uiState.mapPublish != null) {
            LoadMapDialog(uiState = uiState, editorState = state)
        }
        uncaughtError?.let { throwable ->
            UncaughtErrorDialog(throwable = throwable, onDismiss = UncaughtErrorReporter::dismiss)
        }
    }
}

private fun activeEntryLabel(uiState: EditorUiState): String? {
    val source = uiState.activeEntry ?: return null
    return when (source) {
        is EntrySource.Db -> uiState.dbEntries.find { it.entryRef.rawEntryId() == source.id }?.name
            ?: source.id

        is EntrySource.Draft -> uiState.draftEntriesById[source.id]?.name?.takeIf { it.isNotBlank() }
            ?: "New entry"
    }
}

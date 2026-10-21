package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.tools.mapeditor.importer.ImportReport
import com.pamaz.vi.tools.mapeditor.state.EditorState
import kotlinx.coroutines.launch
import java.io.File

private enum class Screen { SIGN_IN, EDITOR }

/** Root composable: sign-in (or offline "Open image") first, then the editor — PLT-01/09. */
@Composable
fun MapEditorApp(state: EditorState) {
    var screen by remember { mutableStateOf(Screen.SIGN_IN) }
    var importReport by remember { mutableStateOf<ImportReport?>(null) }
    var showNewMapVersion by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    IloveViTheme {
        when (screen) {
            Screen.SIGN_IN -> SignInScreen(
                onSignIn = { email, password -> state.signIn(email, password) },
                onSignedIn = {
                    scope.launch {
                        state.loadEntries()
                        state.loadOnlineMap()
                    }
                    screen = Screen.EDITOR
                },
                onContinueOffline = {
                    val image = FilePickers.pickOpenFile("Map image", "png", "jpg", "jpeg")
                    if (image != null) {
                        scope.launch { state.loadEntries() }
                        state.openLocalImage(
                            imageFile = image,
                            storeFile = File(
                                image.parentFile,
                                "${image.nameWithoutExtension}.pins.json"
                            ),
                            mapVersion = 1,
                        )
                        screen = Screen.EDITOR
                    }
                },
            )

            Screen.EDITOR -> EditorScreen(
                state = state,
                onOpenImport = {
                    FilePickers.pickOpenFile("CSV/JSON", "csv", "json")?.let { file ->
                        importReport = state.importReportFor(file)
                    }
                },
                onOpenExport = {
                    FilePickers.pickSaveFile("CSV/JSON", "csv", "json")
                        ?.let { file -> state.exportTo(file) }
                },
                onNewMapVersion = { showNewMapVersion = true },
            )
        }

        importReport?.let { report ->
            ImportReportDialog(
                report = report,
                onConfirm = {
                    scope.launch { state.confirmImport(report.validRows) }
                    importReport = null
                },
                onDismiss = { importReport = null },
            )
        }

        if (showNewMapVersion) {
            NewMapVersionDialog(
                onDone = { config ->
                    state.applyNewMapConfig(config)
                    showNewMapVersion = false
                },
                onDismiss = { showNewMapVersion = false },
            )
        }
    }
}

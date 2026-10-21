package com.pamaz.vi.tools.mapeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.pamaz.vi.tools.mapeditor.di.appModule
import com.pamaz.vi.tools.mapeditor.state.EditorState
import com.pamaz.vi.tools.mapeditor.ui.MapEditorApp
import com.pamaz.vi.tools.mapeditor.ui.UnsavedChangesDialog
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import kotlin.system.exitProcess

/**
 * PLT-01..09: the desktop pin-placement tool (step 5B). A plain Koin application (see
 * `di/AppModule.kt`) — not a KMP module, so none of `build_logic`'s convention plugins apply here,
 * same as `:tools:tilegen`.
 */
fun main() = application {
    KoinApplication(application = { modules(appModule) }) {
        val state = koinInject<EditorState>()
        var exitRequested by remember { mutableStateOf(false) }
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        Window(
            onCloseRequest = {
                if (state.hasUnsavedChanges) exitRequested = true else exitProcess(0)
            },
            title = "IloveVI Map Editor",
        ) {
            MapEditorApp(state = state)

            if (exitRequested) {
                UnsavedChangesDialog(
                    onSaveAndExit = {
                        scope.launch {
                            state.save()
                            exitProcess(0)
                        }
                    },
                    onDiscardAndExit = { exitProcess(0) },
                    onCancel = { exitRequested = false },
                )
            }
        }
    }
}

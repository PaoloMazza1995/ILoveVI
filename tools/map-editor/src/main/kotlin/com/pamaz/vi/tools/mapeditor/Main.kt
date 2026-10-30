package com.pamaz.vi.tools.mapeditor

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.pamaz.vi.tools.mapeditor.di.MapEditorBackend
import com.pamaz.vi.tools.mapeditor.di.appModule
import com.pamaz.vi.tools.mapeditor.error.UncaughtErrorReporter
import com.pamaz.vi.tools.mapeditor.state.EditorState
import com.pamaz.vi.tools.mapeditor.ui.MapEditorApp
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import java.awt.Dimension

/**
 * The Map Editor Functional Analysis's window: fixed minimum 1280x800 (§4). Runs against
 * **fake** services by default (`di/AppModule.kt`) — no `SUPABASE_URL`/`SUPABASE_ANON_KEY` needed
 * to launch it — or the real `data.supabase` implementations, via `MAP_EDITOR_BACKEND=supabase`.
 */
fun main() = application {
    // Belt-and-suspenders alongside AppModule.kt's CoroutineExceptionHandler: anything thrown
    // directly on a raw thread (not one of EditorState's own coroutines) still reaches
    // UncaughtErrorDialog.kt instead of being silently swallowed or printed to a console no one's
    // watching.
    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
        UncaughtErrorReporter.report(
            throwable
        )
    }

    KoinApplication(application = { modules(appModule) }) {
        val state = koinInject<EditorState>()
        val backend = koinInject<MapEditorBackend>()
        LaunchedEffect(Unit) { state.load() }

        Window(
            onCloseRequest = { state.flushAutosave(); exitApplication() },
            title = "ILOVEVI MAP EDITOR",
            state = rememberWindowState(position = WindowPosition(Alignment.Center)),
        ) {
            window.minimumSize = Dimension(1280, 800)
            MapEditorApp(state = state, backend = backend)
        }
    }
}

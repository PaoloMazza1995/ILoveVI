package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.tools.tilegen.SupabaseAdminConfig
import com.pamaz.vi.tools.tilegen.TileGenPipeline
import com.pamaz.vi.tools.tilegen.TileGenRequest
import com.pamaz.vi.tools.tilegen.TileSource
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

/**
 * PLT-08: runs `:tools:tilegen`'s pipeline end to end for a new source image and registers the
 * version. Uploading/registering goes through `SupabaseAdminConfig` (the **service-role** key from
 * the environment, same as the `tilegen` CLI) — not this tool's admin session/anon key, since
 * `map_config` writes have always been a service-role operation here (see `:tools:tilegen`'s
 * README); this dialog doesn't invent a second way to authorize that write.
 */
@Composable
fun NewMapVersionDialog(onDone: (MapConfig) -> Unit, onDismiss: () -> Unit) {
    var imagePath by remember { mutableStateOf<Path?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New map version") },
        text = {
            Column {
                Text("Source image: ${imagePath?.fileName ?: "none chosen"}")
                TextButton(onClick = {
                    FilePickers.pickOpenFile("Images", "png", "jpg", "jpeg")
                        ?.let { imagePath = it.toPath() }
                }) { Text("Choose image...") }
                if (isRunning) CircularProgressIndicator()
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = imagePath != null && !isRunning,
                onClick = {
                    val path = imagePath ?: return@Button
                    isRunning = true
                    scope.launch {
                        val httpClient = HttpClient(OkHttp) {
                            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                        }
                        try {
                            val outputDir = Files.createTempDirectory("map-editor-tiles")
                            val result = TileGenPipeline.run(
                                TileGenRequest(
                                    source = TileSource.Local(path),
                                    outputDir = outputDir,
                                    upload = true,
                                    supabase = SupabaseAdminConfig.fromEnvironment(),
                                ),
                                httpClient,
                            )
                            val config = result.mapConfig
                            if (config != null) onDone(config) else error =
                                "tilegen did not register a map_config row"
                        } catch (e: Exception) {
                            error = e.message ?: "tilegen run failed"
                        } finally {
                            httpClient.close()
                            isRunning = false
                        }
                    }
                },
            ) { Text("Run tilegen and register") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRunning
            ) { Text("Cancel") }
        },
    )
}

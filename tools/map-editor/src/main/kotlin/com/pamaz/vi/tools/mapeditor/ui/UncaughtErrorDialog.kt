package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Shown for any uncaught exception a background coroutine reports to
 * `error/UncaughtErrorReporter.kt`, instead of it failing silently (the bug this whole task fixes:
 * `MapConfigRepository.loadCurrent` used to throw straight out of a coroutine with nothing to
 * catch it). [onDismiss] just clears the reporter — the app keeps running either way, since
 * `SupervisorJob` on the shared scope already means one failed background task doesn't take any
 * other part of the editor down with it.
 */
@Composable
fun UncaughtErrorDialog(throwable: Throwable, onDismiss: () -> Unit) {
    val clipboard: Clipboard = LocalClipboard.current
    val details = throwable.stackTraceToString()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unexpected error") },
        text = {
            Text(
                text = details,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.widthIn(max = 560.dp).verticalScroll(rememberScrollState()),
            )
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Close") } },
        dismissButton = {
            TextButton(onClick = { clipboard.setText(AnnotatedString(details)) }) { Text("Copy details") }
        },
    )
}

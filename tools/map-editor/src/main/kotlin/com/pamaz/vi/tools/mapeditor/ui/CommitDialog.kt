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
import androidx.compose.ui.tooling.preview.Preview
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.tools.mapeditor.data.CommitResult
import com.pamaz.vi.tools.mapeditor.model.EntrySource
import com.pamaz.vi.tools.mapeditor.state.CommitOutcome
import com.pamaz.vi.tools.mapeditor.state.EditorState
import com.pamaz.vi.tools.mapeditor.state.EditorUiState
import kotlinx.coroutines.launch

internal enum class CommitPhase { CONFIRM, SAVING, SUCCESS, ERROR }

/**
 * EDT-71..76: confirmation (summary rows, Cancel/Commit) → saving (progress, Commit can't be
 * pressed twice) → success (counts, draft emptied) → error (message, Retry, draft intact).
 * [previewPhase] exists only so `@Preview`s below can force each of the 4 states — real callers
 * never pass it, since the phase is otherwise driven entirely by [editorState].
 */
@Composable
internal fun CommitDialog(
    uiState: EditorUiState,
    editorState: EditorState,
    onClose: () -> Unit,
    previewPhase: CommitPhase? = null,
) {
    var phase by remember { mutableStateOf(previewPhase ?: CommitPhase.CONFIRM) }
    var error by remember { mutableStateOf<String?>(if (previewPhase == CommitPhase.ERROR) "The network connection was lost." else null) }
    var result by remember {
        mutableStateOf(
            if (previewPhase == CommitPhase.SUCCESS) CommitResult(
                entriesCreated = 2,
                entriesUpdated = 0,
                pinsCreated = 3,
                pinsMoved = 0,
                imagesUploaded = 1
            ) else null,
        )
    }
    val scope = rememberCoroutineScope()

    val newEntries = uiState.draftEntries.size
    val entriesWithImage = uiState.draftEntries.count { it.imageFile != null }
    val newPinsOnNewEntries = uiState.draftPins.count { it.entry is EntrySource.Draft }
    val pinsOnExistingEntries = uiState.draftPins.count { it.entry is EntrySource.Db }

    fun runCommit() {
        phase = CommitPhase.SAVING
        scope.launch {
            when (val outcome = editorState.commit()) {
                is CommitOutcome.Success -> {
                    result = outcome.result
                    phase = CommitPhase.SUCCESS
                }

                is CommitOutcome.Failure -> {
                    error = outcome.message
                    phase = CommitPhase.ERROR
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (phase != CommitPhase.SAVING) onClose() },
        title = {
            Text(
                when (phase) {
                    CommitPhase.SUCCESS -> "Committed"
                    CommitPhase.ERROR -> "Commit failed"
                    else -> "Commit changes"
                },
            )
        },
        text = {
            when (phase) {
                CommitPhase.CONFIRM -> Column {
                    Text("$newEntries new entries ($entriesWithImage with an image)")
                    Text("$newPinsOnNewEntries new pins")
                    Text("$pinsOnExistingEntries pins on existing entries")
                    Text("Map version ${uiState.mapConfig?.mapVersion ?: "unknown"}")
                    Text("Signed in as ${uiState.session?.email ?: "unknown"}")
                }

                CommitPhase.SAVING -> Column {
                    CircularProgressIndicator()
                    Text("Saving entries, images and pins...")
                }

                CommitPhase.SUCCESS -> result?.let {
                    Text(
                        "Saved ${it.entriesCreated + it.entriesUpdated} entries, " +
                                "${it.pinsCreated + it.pinsMoved} pins and ${it.imagesUploaded} images.",
                    )
                }

                CommitPhase.ERROR -> Text(
                    error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            when (phase) {
                CommitPhase.CONFIRM -> Button(onClick = { runCommit() }) { Text("Commit") }
                CommitPhase.ERROR -> Button(onClick = { runCommit() }) { Text("Retry") }
                CommitPhase.SUCCESS -> Button(onClick = onClose) { Text("Done") }
                CommitPhase.SAVING -> {}
            }
        },
        dismissButton = {
            if (phase == CommitPhase.CONFIRM || phase == CommitPhase.ERROR) {
                TextButton(onClick = onClose) { Text("Cancel") }
            }
        },
    )
}

private val previewUiState = EditorUiState()

@Preview
@Composable
private fun CommitDialogConfirmPreview() {
    IloveViTheme {
        CommitDialog(
            previewUiState,
            previewEditorState(),
            onClose = {},
            previewPhase = CommitPhase.CONFIRM
        )
    }
}

@Preview
@Composable
private fun CommitDialogSavingPreview() {
    IloveViTheme {
        CommitDialog(
            previewUiState,
            previewEditorState(),
            onClose = {},
            previewPhase = CommitPhase.SAVING
        )
    }
}

@Preview
@Composable
private fun CommitDialogSuccessPreview() {
    IloveViTheme {
        CommitDialog(
            previewUiState,
            previewEditorState(),
            onClose = {},
            previewPhase = CommitPhase.SUCCESS
        )
    }
}

@Preview
@Composable
private fun CommitDialogErrorPreview() {
    IloveViTheme {
        CommitDialog(
            previewUiState,
            previewEditorState(),
            onClose = {},
            previewPhase = CommitPhase.ERROR
        )
    }
}

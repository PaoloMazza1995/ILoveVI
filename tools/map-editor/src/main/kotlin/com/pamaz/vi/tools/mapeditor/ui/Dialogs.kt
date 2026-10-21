package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pamaz.vi.tools.mapeditor.importer.ImportReport

/** PLT-06: shows every row's outcome and asks for confirmation before anything is written. */
@Composable
fun ImportReportDialog(report: ImportReport, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import report") },
        text = {
            Column {
                Text("${report.totalRows} row(s): ${report.validRows.size} valid, ${report.issues.size} issue(s).")
                if (report.issues.isNotEmpty()) {
                    LazyColumn(Modifier.width(480.dp).height(240.dp)) {
                        items(report.issues) { issue ->
                            Text(
                                "Row ${issue.rowNumber}: ${issue.reason}",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = report.validRows.isNotEmpty()) {
                Text("Import ${report.validRows.size} row(s)")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** PLT-07: confirm before discarding unsaved pin changes on exit. */
@Composable
fun UnsavedChangesDialog(
    onSaveAndExit: () -> Unit,
    onDiscardAndExit: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Unsaved changes") },
        text = { Text("You have unsaved pin changes. Save before exiting?") },
        confirmButton = { TextButton(onClick = onSaveAndExit) { Text("Save and exit") } },
        dismissButton = {
            Column {
                TextButton(onClick = onDiscardAndExit) { Text("Discard and exit") }
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        },
    )
}

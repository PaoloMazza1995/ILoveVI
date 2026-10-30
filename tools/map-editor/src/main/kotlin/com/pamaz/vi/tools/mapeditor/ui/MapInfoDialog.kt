package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.model.MapConfig

/** §4 "Map info": version, size, zoom levels and the tile address, "for reference" (§4's "no manual commands for me"). */
@Composable
fun MapInfoDialog(config: MapConfig?, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Map info") },
        text = {
            if (config == null) {
                Text("No map is configured yet.")
            } else {
                Column {
                    InfoRow("Version", "v${config.mapVersion}")
                    InfoRow("Size", "${config.widthPx} x ${config.heightPx}px")
                    InfoRow("Zoom levels", "${config.minZoom}-${config.maxZoom}")
                    InfoRow("Tile address", config.tileUrlTemplate)
                    InfoRow("Source image", config.sourceImageUrl)
                }
            }
        },
        confirmButton = { Button(onClick = onClose) { Text("Close") } },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

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
import com.pamaz.vi.core.mapview.TILE_OVERZOOM_LEVELS
import com.pamaz.vi.core.model.MapConfig

/**
 * §4 "Map info": version, size, zoom levels and the tile address, "for reference" (§4's "no
 * manual commands for me"). Task item 7c: also shows the overzoom levels on top of the map's
 * native zoom and the resulting total detail ratio — `config.minZoom`/`maxZoom` are the NATIVE
 * levels only (per task item 7's "map_config keeps min_zoom and max_zoom as the NATIVE levels");
 * the app/editor add [TILE_OVERZOOM_LEVELS] on top of that themselves, so this dialog's own camera
 * ceiling math has to do the same addition rather than reading it off `config` directly.
 */
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
                    InfoRow("Native zoom levels", "${config.minZoom}-${config.maxZoom}")
                    InfoRow(
                        "Overzoom",
                        "+$TILE_OVERZOOM_LEVELS levels (up to ${1 shl TILE_OVERZOOM_LEVELS}x enlargement)"
                    )
                    InfoRow(
                        "Total detail ratio",
                        "zoom ${config.minZoom}-${config.maxZoom + TILE_OVERZOOM_LEVELS} " +
                                "(${1 shl (config.maxZoom - config.minZoom + TILE_OVERZOOM_LEVELS)}x, native to max overzoom)",
                    )
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

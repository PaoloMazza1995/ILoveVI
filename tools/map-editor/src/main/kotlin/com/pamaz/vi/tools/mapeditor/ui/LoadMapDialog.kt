package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.mapview.TILE_OVERZOOM_LEVELS
import com.pamaz.vi.core.mapview.loadSingleImageMapContentFromFile
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.tools.mapeditor.data.MapPublishPlanning
import com.pamaz.vi.tools.mapeditor.model.MapPublishPhase
import com.pamaz.vi.tools.mapeditor.model.MapPublishPlan
import com.pamaz.vi.tools.mapeditor.model.MapPublishStep
import com.pamaz.vi.tools.mapeditor.model.MapPublishStepState
import com.pamaz.vi.tools.mapeditor.model.MapPublishStepStatus
import com.pamaz.vi.tools.mapeditor.model.MapVersionAction
import com.pamaz.vi.tools.mapeditor.state.EditorState
import com.pamaz.vi.tools.mapeditor.state.EditorUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * §2/§3's one-click "Load map image..." dialog: confirm (preview, computed plan, version
 * decision) → running (per-step progress list) → success/error. Driven entirely by
 * [EditorUiState.mapPublish] — this composable has no state of its own beyond the decoded preview
 * thumbnail, same `EditorState`-is-the-only-source-of-truth pattern as [CommitDialog].
 */
@Composable
fun LoadMapDialog(uiState: EditorUiState, editorState: EditorState) {
    val snapshot = uiState.mapPublish ?: return
    val isAdmin = uiState.session?.isAdmin == true

    AlertDialog(
        onDismissRequest = {
            if (snapshot.phase != MapPublishPhase.RUNNING) editorState.cancelMapPublish()
        },
        title = {
            Text(
                when (snapshot.phase) {
                    MapPublishPhase.SUCCESS -> "Map loaded"
                    MapPublishPhase.ERROR -> "Could not load the map"
                    else -> "Load map image"
                },
            )
        },
        text = {
            when (snapshot.phase) {
                MapPublishPhase.CONFIRM -> ConfirmContent(
                    snapshot.plan,
                    snapshot.invalidReason,
                    isAdmin
                )

                MapPublishPhase.RUNNING -> Column {
                    snapshot.steps.forEach { step -> StepRow(step) }
                }

                MapPublishPhase.SUCCESS -> Column {
                    val config = snapshot.mapConfig
                    Text("Map v${config?.mapVersion} is registered and showing on the map.")
                    config?.let {
                        Text(
                            "${it.widthPx}x${it.heightPx}px, zoom ${it.minZoom}-${it.maxZoom}",
                            color = IloveViColors.inkMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = IloveViSpacing.space1),
                        )
                    }
                }

                MapPublishPhase.ERROR -> Column {
                    Text(
                        snapshot.errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "The map wasn't registered — nothing was changed. Retry picks up where this left off.",
                        color = IloveViColors.inkMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = IloveViSpacing.space2),
                    )
                }
            }
        },
        confirmButton = {
            when (snapshot.phase) {
                MapPublishPhase.CONFIRM -> if (snapshot.plan != null) {
                    Button(
                        onClick = editorState::confirmMapPublish,
                        enabled = isAdmin
                    ) { Text("Confirm") }
                }

                MapPublishPhase.ERROR -> Button(onClick = editorState::retryMapPublish) { Text("Retry") }
                MapPublishPhase.SUCCESS -> Button(onClick = editorState::dismissMapPublish) { Text("Done") }
                MapPublishPhase.RUNNING -> {}
            }
        },
        dismissButton = {
            if (snapshot.phase != MapPublishPhase.RUNNING && snapshot.phase != MapPublishPhase.SUCCESS) {
                TextButton(onClick = editorState::cancelMapPublish) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun ConfirmContent(plan: MapPublishPlan?, invalidReason: String?, isAdmin: Boolean) {
    Column {
        if (plan == null) {
            Text(
                invalidReason ?: "Choose a different image.",
                color = MaterialTheme.colorScheme.error
            )
            return@Column
        }

        var preview by remember(plan.sourceImage) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(plan.sourceImage) {
            preview = withContext(Dispatchers.IO) {
                runCatching { loadSingleImageMapContentFromFile(plan.sourceImage).image }.getOrNull()
            }
        }
        preview?.let {
            Image(
                bitmap = it,
                contentDescription = "Preview of ${plan.sourceImage.name}",
                modifier = Modifier.height(120.dp).width(160.dp)
                    .padding(bottom = IloveViSpacing.space2),
            )
        }

        Text("${plan.widthPx}x${plan.heightPx}px")
        Text(
            "Tile size 256, zoom ${plan.minZoom}-${plan.maxZoom}, ${plan.tileCount} tiles",
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            "1 image pixel = 1 screen pixel at zoom ${plan.maxZoom}; beyond it the map is enlarged " +
                    "(up to ${1 shl TILE_OVERZOOM_LEVELS}x, $TILE_OVERZOOM_LEVELS extra zoom levels).",
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = IloveViSpacing.space1),
        )

        val estimatedMb = plan.estimatedStorageBytes / (1024.0 * 1024.0)
        Text(
            "Estimated storage: ~${(estimatedMb * 10).toInt() / 10.0}MB",
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = IloveViSpacing.space1),
        )
        MapPublishPlanning.storageWarning(plan.estimatedStorageBytes)?.let { warning ->
            Text(
                warning,
                color = IloveViColors.warningYellow,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = IloveViSpacing.space1),
            )
        }

        Text(
            when (plan.versionAction) {
                MapVersionAction.UPDATE_IN_PLACE -> "This will update map v${plan.mapVersion} in place — it has no pins on it yet."
                MapVersionAction.NEW_VERSION -> "This will create map v${plan.mapVersion}. " +
                        "${plan.pinsToReposition} pin(s) on the current map will need repositioning."
            },
            color = IloveViColors.ink,
            modifier = Modifier.padding(top = IloveViSpacing.space2),
        )

        Text(
            "Tile address: ${plan.tileUrlTemplate}",
            color = IloveViColors.inkFaint,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = IloveViSpacing.space2),
        )

        if (!isAdmin) {
            Text(
                "Only an admin account can publish a map. Sign in as admin to enable Confirm.",
                color = IloveViColors.warningYellow,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = IloveViSpacing.space2),
            )
        }
    }
}

@Composable
private fun StepRow(step: MapPublishStepState) {
    Column(Modifier.padding(vertical = IloveViSpacing.space1)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                when (step.status) {
                    MapPublishStepStatus.PENDING -> "○"
                    MapPublishStepStatus.RUNNING -> "◐"
                    MapPublishStepStatus.DONE -> "✓"
                    MapPublishStepStatus.FAILED -> "✕"
                },
                color = when (step.status) {
                    MapPublishStepStatus.DONE -> IloveViColors.brandTeal
                    MapPublishStepStatus.FAILED -> MaterialTheme.colorScheme.error
                    MapPublishStepStatus.RUNNING -> IloveViColors.brandPink
                    MapPublishStepStatus.PENDING -> IloveViColors.inkFaint
                },
            )
            Text(step.step.label(), color = IloveViColors.ink)
        }
        if (step.detail.isNotBlank()) {
            Text(
                step.detail,
                color = IloveViColors.inkMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = IloveViSpacing.space5),
            )
        }
        if (step.status == MapPublishStepStatus.RUNNING && step.total > 0) {
            LinearProgressIndicator(
                progress = { step.current.toFloat() / step.total },
                modifier = Modifier.padding(
                    start = IloveViSpacing.space5,
                    top = IloveViSpacing.space1
                ),
            )
        }
    }
}

private fun MapPublishStep.label(): String = when (this) {
    MapPublishStep.VALIDATE -> "Validate image"
    MapPublishStep.COMPUTE -> "Compute tile plan"
    MapPublishStep.GENERATE -> "Generate tiles"
    MapPublishStep.UPLOAD -> "Upload"
    MapPublishStep.VERIFY -> "Verify"
    MapPublishStep.REGISTER -> "Register map"
    MapPublishStep.RELOAD -> "Reload"
}

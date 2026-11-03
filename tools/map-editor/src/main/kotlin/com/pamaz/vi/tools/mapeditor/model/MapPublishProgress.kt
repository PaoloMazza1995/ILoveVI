package com.pamaz.vi.tools.mapeditor.model

import java.io.File

/** PLT-08's one-click "Load map image..." pipeline, in the order the progress list shows them. */
enum class MapPublishStep { VALIDATE, COMPUTE, GENERATE, UPLOAD, VERIFY, REGISTER, RELOAD }

enum class MapPublishStepStatus { PENDING, RUNNING, DONE, FAILED }

data class MapPublishStepState(
    val step: MapPublishStep,
    val status: MapPublishStepStatus = MapPublishStepStatus.PENDING,
    /** e.g. "Uploading tiles" / "Uploaded 212/341 tiles" / "Tile v3/4/0/0.png returned 404". */
    val detail: String = "",
    val current: Int = 0,
    val total: Int = 0,
)

/** Which of §3's two outcomes a publish will produce — decided once, shown in the confirmation step, never silently switched mid-run. */
enum class MapVersionAction { UPDATE_IN_PLACE, NEW_VERSION }

/**
 * Everything computed from the chosen image before any tiling/network work starts — the
 * confirmation step shows this, and it alone decides what [MapPublisher.publish] will do; nothing
 * about the plan can change once this is built (re-picking a file makes a new one instead).
 */
data class MapPublishPlan(
    val sourceImage: File,
    val widthPx: Int,
    val heightPx: Int,
    val tileSize: Int,
    val minZoom: Int,
    val maxZoom: Int,
    val tileCount: Int,
    val mapVersion: Int,
    val versionAction: MapVersionAction,
    /** Only non-zero for [MapVersionAction.NEW_VERSION] — every pin that will now be on an older version than [mapVersion]. */
    val pinsToReposition: Int,
    val tileUrlTemplate: String,
    val format: com.pamaz.vi.tools.tilegen.TileFormat,
    /** From [com.pamaz.vi.tools.tilegen.TilePyramidGenerator.sampleTileByteSizes] — a few real encoded tile sizes, averaged and multiplied by [tileCount], shown in the confirmation dialog *before* any real generation starts. */
    val estimatedStorageBytes: Long,
)

sealed interface MapPublishPlanResult {
    data class Valid(val plan: MapPublishPlan) : MapPublishPlanResult
    data class Invalid(val message: String) : MapPublishPlanResult
}

sealed interface MapPublishOutcome {
    data class Success(val mapConfig: com.pamaz.vi.core.model.MapConfig) : MapPublishOutcome
    data class Failure(val message: String) : MapPublishOutcome
}

enum class MapPublishPhase { CONFIRM, RUNNING, SUCCESS, ERROR }

/**
 * One immutable snapshot of the whole "Load map image..." dialog (`ui/LoadMapDialog.kt`) —
 * [com.pamaz.vi.tools.mapeditor.state.EditorState] is the only thing that produces a new one, same
 * `EditorUiState`-style pattern. `plan == null` with a non-null [invalidReason] means the chosen
 * file failed validation and needs re-picking; a [plan] with [phase] `CONFIRM` means it's valid
 * and awaiting the admin's confirmation.
 */
data class MapPublishSnapshot(
    val phase: MapPublishPhase,
    val plan: MapPublishPlan? = null,
    val invalidReason: String? = null,
    val steps: List<MapPublishStepState> = emptyList(),
    val errorMessage: String? = null,
    val mapConfig: com.pamaz.vi.core.model.MapConfig? = null,
)

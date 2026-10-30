package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.core.model.MapConfig

/**
 * Every raw column `map_config`'s current row actually has, kept around even when it fails to
 * become a [MapConfig] — so [MapConfigResult.Invalid] can show the real
 * `tile_url_template`/`source_image_url` that caused the problem (`ui/MapArea.kt`'s empty state)
 * instead of just a message. Every field is nullable since a row that fails to *decode* at all
 * (malformed JSON, a missing column) leaves nothing to show either.
 */
data class RawMapConfigRow(
    val mapVersion: Int?,
    val sourceImageUrl: String?,
    val widthPx: Int?,
    val heightPx: Int?,
    val tileUrlTemplate: String?,
    val minZoom: Int?,
    val maxZoom: Int?,
)

/**
 * [MapConfigRepository.loadCurrent]'s result — a crash in `map_config`'s current row (bad
 * `tile_url_template`, zero/negative size, `min_zoom > max_zoom`, blank `source_image_url`, ...)
 * is bad *data*, not a reason to kill the loading coroutine (the bug this type exists to fix: that
 * data used to be fed straight into [MapConfig]'s constructor, whose `require(...)` calls throw
 * `IllegalArgumentException` on exactly these rows). [Missing] and [Invalid] are both normal,
 * displayable states for the editor's empty screen, not errors.
 */
sealed interface MapConfigResult {
    data class Loaded(val config: MapConfig) : MapConfigResult
    data object Missing : MapConfigResult
    data class Invalid(val reason: String, val rawRow: RawMapConfigRow) : MapConfigResult
}

/**
 * The one piece of a [MapConfigResult] that §3's version decision
 * ([MapPublishPlanning.decideVersion]) needs — a row's `map_version` is still meaningful even when
 * the rest of that row is invalid (it's how "replace this row in place" vs. "create a new version"
 * is decided for an already-broken row), so this works for all three cases, not just [Loaded].
 */
val MapConfigResult.mapVersionOrNull: Int?
    get() = when (this) {
        is MapConfigResult.Loaded -> config.mapVersion
        is MapConfigResult.Invalid -> rawRow.mapVersion
        MapConfigResult.Missing -> null
    }

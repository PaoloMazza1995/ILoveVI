package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.tools.mapeditor.model.DbPin
import com.pamaz.vi.tools.mapeditor.model.MapPublishPlan
import com.pamaz.vi.tools.mapeditor.model.MapVersionAction
import com.pamaz.vi.tools.tilegen.ImageDimensions
import java.io.File

/**
 * The validation/zoom-math/version-decision logic PLT-08's "Load map image..." needs — shared by
 * [com.pamaz.vi.tools.mapeditor.data.fake.FakeMapPublisher] and
 * [com.pamaz.vi.tools.mapeditor.data.supabase.SupabaseMapPublisher] so the two don't quietly drift
 * apart (the fake backend should compute the exact same plan a real project would for the same
 * image/pin state, since that's the whole point of testing against it first). Only the parts that
 * actually need a backend — tiling to disk, uploading, writing `map_config` — live in each
 * implementation's own class.
 */
object MapPublishPlanning {
    /** §2a: "size at most 16384 px per side." */
    const val MAX_DIMENSION_PX = 16384

    /** §2a: "file size limit with a clear message" — generous for a full-res game map PNG, still well short of anything that would stall a desktop upload. */
    const val MAX_SOURCE_IMAGE_BYTES = 50L * 1024 * 1024

    /** §2b: "tile size 256" — not user-configurable; the Functional Analysis names this one value, not a range. */
    const val TILE_SIZE = 256

    private val ALLOWED_EXTENSIONS = setOf("png", "jpg", "jpeg")

    /** `null` means valid. */
    fun validate(file: File): String? {
        if (!file.exists() || !file.isFile || !file.canRead()) {
            return "Could not read \"${file.name}\"."
        }
        if (file.extension.lowercase() !in ALLOWED_EXTENSIONS) {
            return "Choose a PNG or JPEG image (was \"${file.extension}\")."
        }
        if (file.length() > MAX_SOURCE_IMAGE_BYTES) {
            val mb = file.length() / (1024 * 1024)
            return "Image is ${mb}MB; the limit is ${MAX_SOURCE_IMAGE_BYTES / (1024 * 1024)}MB."
        }
        return null
    }

    /** `null` means valid; a non-null error should stop before any tiling/network work starts. */
    fun validateDimensions(dimensions: ImageDimensions): String? {
        if (dimensions.widthPx > MAX_DIMENSION_PX || dimensions.heightPx > MAX_DIMENSION_PX) {
            return "Image is ${dimensions.widthPx}x${dimensions.heightPx}px; the limit is ${MAX_DIMENSION_PX}px per side."
        }
        return null
    }

    /**
     * §3's version decision: if nothing is on the *current* config's own version yet, it's safe to
     * overwrite that row in place; otherwise bump to a new version and warn how many existing pins
     * (all of them, now on an older version than the new one) need repositioning.
     */
    data class VersionDecision(
        val mapVersion: Int,
        val action: MapVersionAction,
        val pinsToReposition: Int
    )

    /**
     * [currentMapVersion] is [MapConfigResult.mapVersionOrNull], not a `MapConfig` — a row can be
     * [MapConfigResult.Invalid] (bad `tile_url_template`, zero size, ...) and still have a perfectly
     * good `map_version` column, and that version is exactly what "replace this broken row in place"
     * needs to work.
     */
    fun decideVersion(currentMapVersion: Int?, allPins: List<DbPin>): VersionDecision {
        if (currentMapVersion == null) return VersionDecision(1, MapVersionAction.NEW_VERSION, 0)
        val pinsOnCurrentVersion = allPins.count { it.mapVersion == currentMapVersion }
        return if (pinsOnCurrentVersion == 0) {
            VersionDecision(currentMapVersion, MapVersionAction.UPDATE_IN_PLACE, 0)
        } else {
            VersionDecision(currentMapVersion + 1, MapVersionAction.NEW_VERSION, allPins.size)
        }
    }

    /** §2b: tile size 256, zoom/tile-count computed from the image's own size — reuses `core:model`'s [TileScheme], never a second implementation of this math. */
    fun buildPlan(
        file: File,
        dimensions: ImageDimensions,
        tileUrlTemplate: String,
        decision: VersionDecision,
    ): MapPublishPlan {
        val scheme = TileScheme(dimensions.widthPx, dimensions.heightPx, TILE_SIZE)
        val tileCount = (scheme.minZoom..scheme.maxZoom).sumOf { zoom ->
            scheme.tilesWide(zoom) * scheme.tilesHigh(zoom)
        }
        return MapPublishPlan(
            sourceImage = file,
            widthPx = dimensions.widthPx,
            heightPx = dimensions.heightPx,
            tileSize = TILE_SIZE,
            minZoom = scheme.minZoom,
            maxZoom = scheme.maxZoom,
            tileCount = tileCount,
            mapVersion = decision.mapVersion,
            versionAction = decision.action,
            pinsToReposition = decision.pinsToReposition,
            tileUrlTemplate = tileUrlTemplate,
        )
    }
}

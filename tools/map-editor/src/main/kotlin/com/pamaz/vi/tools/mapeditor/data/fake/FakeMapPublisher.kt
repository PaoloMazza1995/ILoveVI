package com.pamaz.vi.tools.mapeditor.data.fake

import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.tools.mapeditor.data.MapPublishPlanning
import com.pamaz.vi.tools.mapeditor.data.MapPublisher
import com.pamaz.vi.tools.mapeditor.data.PinRepository
import com.pamaz.vi.tools.mapeditor.data.mapVersionOrNull
import com.pamaz.vi.tools.mapeditor.model.MapPublishOutcome
import com.pamaz.vi.tools.mapeditor.model.MapPublishPlan
import com.pamaz.vi.tools.mapeditor.model.MapPublishPlanResult
import com.pamaz.vi.tools.mapeditor.model.MapPublishStep
import com.pamaz.vi.tools.mapeditor.model.MapPublishStepState
import com.pamaz.vi.tools.mapeditor.model.MapPublishStepStatus
import com.pamaz.vi.tools.tilegen.TileFormat
import com.pamaz.vi.tools.tilegen.TilePyramidGenerator
import com.pamaz.vi.tools.tilegen.readImageDimensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Demo stand-in for [com.pamaz.vi.tools.mapeditor.data.supabase.SupabaseMapPublisher]: real
 * validation and real [TilePyramidGenerator] tiling (so "Load map image..." is genuinely
 * exercised against the fake backend too, not just skipped), but "uploads"/"registers" by writing
 * straight into [FakeMapConfigRepository] instead of any network call — forced to fail via
 * [forceFailureAtStep] for previews/tests of the error state.
 */
@OptIn(ExperimentalTime::class)
class FakeMapPublisher(
    private val mapConfigRepository: FakeMapConfigRepository,
    private val pinRepository: PinRepository,
    private val forceFailureAtStep: MapPublishStep? = null,
) : MapPublisher {
    override suspend fun plan(file: File): MapPublishPlanResult {
        MapPublishPlanning.validate(file)?.let { return MapPublishPlanResult.Invalid(it) }

        val dimensions = try {
            withContext(Dispatchers.IO) { readImageDimensions(file.toPath()) }
        } catch (e: Exception) {
            return MapPublishPlanResult.Invalid("Could not read this image: ${e.message}")
        }
        MapPublishPlanning.validateDimensions(dimensions)
            ?.let { return MapPublishPlanResult.Invalid(it) }

        val currentConfig = mapConfigRepository.loadCurrent()
        val decision = MapPublishPlanning.decideVersion(
            currentConfig.mapVersionOrNull,
            pinRepository.loadAll()
        )
        val tileUrlTemplate =
            "https://example.com/fake-tiles/v${decision.mapVersion}/{z}/{x}/{y}.${TileFormat.WEBP.extension}"
        // Real sample tiles from the real file (task item 7b: a storage estimate shown *before*
        // any full generation starts) — the fake backend reuses this exact logic too, since the
        // whole point of testing against it first is that it computes the same plan a real
        // project would for the same image.
        val estimatedStorageBytes = withContext(Dispatchers.IO) {
            MapPublishPlanning.estimateStorageBytes(file, dimensions, TileFormat.WEBP)
        }
        return MapPublishPlanResult.Valid(
            MapPublishPlanning.buildPlan(
                file,
                dimensions,
                tileUrlTemplate,
                decision,
                TileFormat.WEBP,
                estimatedStorageBytes,
            )
        )
    }

    override suspend fun publish(
        plan: MapPublishPlan,
        onProgress: (List<MapPublishStepState>) -> Unit,
    ): MapPublishOutcome {
        var steps = MapPublishStep.entries.map { MapPublishStepState(it) }
        suspend fun updateStep(
            targetStep: MapPublishStep,
            status: MapPublishStepStatus,
            detail: String = ""
        ) {
            steps = steps.map {
                if (it.step == targetStep) MapPublishStepState(
                    targetStep,
                    status,
                    detail
                ) else it
            }
            onProgress(steps)
            delay(250)
            if (status == MapPublishStepStatus.RUNNING && targetStep == forceFailureAtStep) {
                error("Simulated failure at ${targetStep.name.lowercase()} (forceFailureAtStep)")
            }
        }

        return try {
            updateStep(MapPublishStep.VALIDATE, MapPublishStepStatus.RUNNING)
            updateStep(
                MapPublishStep.VALIDATE,
                MapPublishStepStatus.DONE,
                "${plan.widthPx}x${plan.heightPx}px"
            )

            updateStep(MapPublishStep.COMPUTE, MapPublishStepStatus.RUNNING)
            updateStep(
                MapPublishStep.COMPUTE,
                MapPublishStepStatus.DONE,
                "Zoom 0-${plan.maxZoom}, ${plan.tileCount} tiles"
            )

            updateStep(MapPublishStep.GENERATE, MapPublishStepStatus.RUNNING, "Generating tiles...")
            val tempDir =
                withContext(Dispatchers.IO) { createTempDirectory("ilovevi-fake-map-publish") }
            val scheme =
                com.pamaz.vi.core.model.map.TileScheme(plan.widthPx, plan.heightPx, plan.tileSize)
            TilePyramidGenerator.generate(
                plan.sourceImage.toPath(),
                scheme,
                tempDir,
                plan.mapVersion,
                format = plan.format,
            )
            updateStep(
                MapPublishStep.GENERATE,
                MapPublishStepStatus.DONE,
                "${plan.tileCount} tiles generated"
            )

            updateStep(
                MapPublishStep.UPLOAD,
                MapPublishStepStatus.RUNNING,
                "Uploading ${plan.tileCount}/${plan.tileCount} tiles (fake)"
            )
            updateStep(
                MapPublishStep.UPLOAD,
                MapPublishStepStatus.DONE,
                "${plan.tileCount} tiles uploaded (fake)"
            )

            updateStep(MapPublishStep.VERIFY, MapPublishStepStatus.RUNNING)
            updateStep(MapPublishStep.VERIFY, MapPublishStepStatus.DONE, "Tiles reachable (fake)")

            updateStep(MapPublishStep.REGISTER, MapPublishStepStatus.RUNNING)
            val newConfig = MapConfig(
                mapVersion = plan.mapVersion,
                sourceImageUrl = plan.sourceImage.toURI().toString(),
                widthPx = plan.widthPx,
                heightPx = plan.heightPx,
                tileUrlTemplate = plan.tileUrlTemplate,
                minZoom = plan.minZoom,
                maxZoom = plan.maxZoom,
                updatedAt = Clock.System.now(),
            )
            mapConfigRepository.publish(newConfig)
            updateStep(
                MapPublishStep.REGISTER,
                MapPublishStepStatus.DONE,
                "map_config v${plan.mapVersion} registered (fake)"
            )

            updateStep(MapPublishStep.RELOAD, MapPublishStepStatus.DONE)
            MapPublishOutcome.Success(newConfig)
        } catch (e: Exception) {
            val failingStep = steps.firstOrNull { it.status == MapPublishStepStatus.RUNNING }?.step
                ?: MapPublishStep.VALIDATE
            steps = steps.map {
                if (it.step == failingStep) it.copy(
                    status = MapPublishStepStatus.FAILED,
                    detail = e.message ?: "Unknown error"
                ) else it
            }
            onProgress(steps)
            MapPublishOutcome.Failure(e.message ?: "Unknown error")
        }
    }
}

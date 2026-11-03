package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.core.model.map.TileUrl
import com.pamaz.vi.tools.mapeditor.data.MapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.MapPublishPlanning
import com.pamaz.vi.tools.mapeditor.data.MapPublisher
import com.pamaz.vi.tools.mapeditor.data.PinRepository
import com.pamaz.vi.tools.mapeditor.data.RemoteTileStorage
import com.pamaz.vi.tools.mapeditor.data.mapVersionOrNull
import com.pamaz.vi.tools.mapeditor.model.MapPublishOutcome
import com.pamaz.vi.tools.mapeditor.model.MapPublishPlan
import com.pamaz.vi.tools.mapeditor.model.MapPublishPlanResult
import com.pamaz.vi.tools.mapeditor.model.MapPublishStep
import com.pamaz.vi.tools.mapeditor.model.MapPublishStepState
import com.pamaz.vi.tools.mapeditor.model.MapPublishStepStatus
import com.pamaz.vi.tools.mapeditor.model.MapVersionAction
import com.pamaz.vi.tools.tilegen.TileFormat
import com.pamaz.vi.tools.tilegen.TilePyramidGenerator
import com.pamaz.vi.tools.tilegen.readImageDimensions
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.extension
import kotlin.io.path.readBytes
import kotlin.random.Random
import kotlin.time.Instant

private const val UPLOAD_PARALLELISM = 4
private const val UPLOAD_MAX_ATTEMPTS = 3
private const val UPLOAD_RETRY_BASE_DELAY_MILLIS = 500L
private const val VERIFY_SAMPLE_SIZE = 5
private const val VERIFY_TIMEOUT_MILLIS = 8_000L

/**
 * PLT-08's real "Load map image..." pipeline: generate (via `:tools:tilegen`'s
 * [TilePyramidGenerator] — this module never re-implements tiling), upload every tile plus the
 * source image to the `map-tiles` bucket under `v<mapVersion>/...` through [storage] (the
 * signed-in admin's own session — never the service-role key; the bucket's
 * `map_tiles_admin_insert`/`_update` policies in `backend/supabase/migrations/
 * 20261011120006_storage_map_tiles.sql` already allow this, confirmed by reading that migration,
 * no new one was needed), verify a sample of the uploaded tiles are actually fetchable at their
 * public URL, then register `map_config` — in that order, so a failure before the last step never
 * leaves a half-registered map (§2f).
 *
 * One instance is a long-lived Koin single, which is what makes "Retry resumes" work across
 * multiple [publish] calls for the same [MapPublishPlan.mapVersion]: [uploadedPaths] remembers
 * every object path already confirmed uploaded (via [RemoteTileStorage.exists] the first time a
 * path is seen, then in-memory after that), so a retry only re-uploads what didn't make it last
 * time. [storage] is [RemoteTileStorage], not supabase-kt's full `BucketApi`, on purpose — see
 * that interface's own KDoc.
 */
class SupabaseMapPublisher(
    private val storage: RemoteTileStorage,
    private val pinRepository: PinRepository,
    private val mapConfigRepository: MapConfigRepository,
    private val mapConfigWriter: MapConfigWriter,
    private val httpClient: HttpClient,
    private val baseUrl: String,
) : MapPublisher {
    private val uploadedPaths = mutableSetOf<String>()
    private var tempDirMapVersion: Int? = null
    private var tempDir: Path? = null

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
        val tileUrlTemplate = tileUrlTemplate(baseUrl, decision.mapVersion, DEFAULT_TILE_FORMAT)
        // Real sample tiles from the real file, before any full tiling/upload starts — task item
        // 7b's "estimate of the storage size from the size of a few sample tiles".
        val estimatedStorageBytes = withContext(Dispatchers.IO) {
            MapPublishPlanning.estimateStorageBytes(file, dimensions, DEFAULT_TILE_FORMAT)
        }
        return MapPublishPlanResult.Valid(
            MapPublishPlanning.buildPlan(
                file,
                dimensions,
                tileUrlTemplate,
                decision,
                DEFAULT_TILE_FORMAT,
                estimatedStorageBytes,
            )
        )
    }

    override suspend fun publish(
        plan: MapPublishPlan,
        onProgress: (List<MapPublishStepState>) -> Unit,
    ): MapPublishOutcome {
        if (tempDirMapVersion != plan.mapVersion) {
            // A genuinely new publish attempt (different image/version), not a retry of this one — start clean.
            uploadedPaths.clear()
            tempDirMapVersion = plan.mapVersion
            tempDir = null
        }

        val stepsLock = Mutex()
        var steps = MapPublishStep.entries.map { MapPublishStepState(it) }
        suspend fun updateStep(
            target: MapPublishStep,
            transform: (MapPublishStepState) -> MapPublishStepState
        ) {
            stepsLock.withLock {
                steps = steps.map { if (it.step == target) transform(it) else it }
                onProgress(steps)
            }
        }

        return try {
            updateStep(MapPublishStep.VALIDATE) { it.copy(status = MapPublishStepStatus.RUNNING) }
            updateStep(MapPublishStep.VALIDATE) {
                it.copy(
                    status = MapPublishStepStatus.DONE,
                    detail = "${plan.widthPx}x${plan.heightPx}px"
                )
            }

            updateStep(MapPublishStep.COMPUTE) { it.copy(status = MapPublishStepStatus.RUNNING) }
            updateStep(MapPublishStep.COMPUTE) {
                it.copy(
                    status = MapPublishStepStatus.DONE,
                    detail = "Zoom 0-${plan.maxZoom}, ${plan.tileCount} tiles"
                )
            }

            updateStep(MapPublishStep.GENERATE) {
                it.copy(
                    status = MapPublishStepStatus.RUNNING,
                    detail = "Generating tiles..."
                )
            }
            val versionDir = generateTiles(plan)
            updateStep(MapPublishStep.GENERATE) {
                it.copy(
                    status = MapPublishStepStatus.DONE,
                    detail = "${plan.tileCount} tiles generated"
                )
            }

            updateStep(MapPublishStep.UPLOAD) { it.copy(status = MapPublishStepStatus.RUNNING) }
            val filesToUpload = collectUploadFiles(plan, versionDir)
            uploadAll(filesToUpload) { current, total ->
                updateStep(MapPublishStep.UPLOAD) {
                    it.copy(
                        status = MapPublishStepStatus.RUNNING,
                        detail = "Uploaded $current/$total files",
                        current = current,
                        total = total
                    )
                }
            }
            updateStep(MapPublishStep.UPLOAD) {
                it.copy(
                    status = MapPublishStepStatus.DONE,
                    detail = "${filesToUpload.size} files uploaded"
                )
            }

            updateStep(MapPublishStep.VERIFY) { it.copy(status = MapPublishStepStatus.RUNNING) }
            verifyTiles(plan)
            updateStep(MapPublishStep.VERIFY) {
                it.copy(
                    status = MapPublishStepStatus.DONE,
                    detail = "Sample tiles reachable"
                )
            }

            updateStep(MapPublishStep.REGISTER) { it.copy(status = MapPublishStepStatus.RUNNING) }
            val sourceImageUrl = storage.publicUrl(sourceImageObjectPath(plan))
            val newConfig = MapConfig(
                mapVersion = plan.mapVersion,
                sourceImageUrl = sourceImageUrl,
                widthPx = plan.widthPx,
                heightPx = plan.heightPx,
                tileUrlTemplate = plan.tileUrlTemplate,
                minZoom = plan.minZoom,
                maxZoom = plan.maxZoom,
                updatedAt = Instant.fromEpochMilliseconds(0),
            )
            when (plan.versionAction) {
                MapVersionAction.UPDATE_IN_PLACE -> mapConfigWriter.updateInPlace(newConfig)
                MapVersionAction.NEW_VERSION -> mapConfigWriter.insertNewVersion(newConfig)
            }
            updateStep(MapPublishStep.REGISTER) {
                it.copy(
                    status = MapPublishStepStatus.DONE,
                    detail = "map_config v${plan.mapVersion} registered"
                )
            }

            updateStep(MapPublishStep.RELOAD) { it.copy(status = MapPublishStepStatus.DONE) }
            MapPublishOutcome.Success(newConfig)
        } catch (e: Exception) {
            val failingStep = steps.firstOrNull { it.status == MapPublishStepStatus.RUNNING }?.step
            if (failingStep != null) {
                updateStep(failingStep) {
                    it.copy(
                        status = MapPublishStepStatus.FAILED,
                        detail = e.message ?: "Unknown error"
                    )
                }
            }
            MapPublishOutcome.Failure(e.message ?: "Unknown error")
        }
    }

    private suspend fun generateTiles(plan: MapPublishPlan): Path {
        val dir = tempDir ?: withContext(Dispatchers.IO) {
            createTempDirectory("ilovevi-map-publish-v${plan.mapVersion}")
        }.also { tempDir = it }
        val scheme = TileScheme(plan.widthPx, plan.heightPx, plan.tileSize)
        TilePyramidGenerator.generate(
            plan.sourceImage.toPath(), scheme, dir, plan.mapVersion, format = plan.format,
        )
        return dir.resolve(plan.mapVersion.toString())
    }

    private fun sourceImageObjectPath(plan: MapPublishPlan): String =
        sourceImageObjectPath(plan.mapVersion, plan.sourceImage.extension)

    private fun collectUploadFiles(
        plan: MapPublishPlan,
        versionDir: Path
    ): List<Pair<String, Path>> {
        val tileFiles = Files.walk(versionDir).use { stream ->
            stream.filter { Files.isRegularFile(it) }.toList()
        }
        val tileUploads = tileFiles.map { file ->
            val relative = versionDir.relativize(file).toString().replace(File.separatorChar, '/')
            tileObjectPath(plan.mapVersion, relative) to file
        }
        return tileUploads + (sourceImageObjectPath(plan) to plan.sourceImage.toPath())
    }

    private suspend fun uploadAll(
        files: List<Pair<String, Path>>,
        onProgress: suspend (current: Int, total: Int) -> Unit
    ) {
        val progressLock = Mutex()
        var completed = 0
        onProgress(0, files.size)

        val semaphore = Semaphore(UPLOAD_PARALLELISM)
        coroutineScope {
            files.map { (objectPath, localPath) ->
                async {
                    semaphore.withPermit {
                        uploadOneWithSkipAndRetry(objectPath, localPath)
                    }
                    progressLock.withLock {
                        completed++
                        onProgress(completed, files.size)
                    }
                }
            }.forEach { it.await() }
        }
    }

    private suspend fun uploadOneWithSkipAndRetry(objectPath: String, localPath: Path) {
        if (objectPath in uploadedPaths) return
        if (storage.exists(objectPath)) {
            uploadedPaths += objectPath
            return
        }

        val contentType = when (localPath.extension.lowercase()) {
            "jpg", "jpeg" -> ContentType.Image.JPEG
            "webp" -> ContentType.Image.WEBP
            else -> ContentType.Image.PNG
        }
        var lastError: Exception? = null
        repeat(UPLOAD_MAX_ATTEMPTS) { attempt ->
            try {
                val bytes = withContext(Dispatchers.IO) { localPath.readBytes() }
                storage.upload(objectPath, bytes, contentType)
                uploadedPaths += objectPath
                return
            } catch (e: Exception) {
                lastError = e
                if (attempt < UPLOAD_MAX_ATTEMPTS - 1) {
                    delay(UPLOAD_RETRY_BASE_DELAY_MILLIS * (1L shl attempt))
                }
            }
        }
        throw IllegalStateException(
            "Failed to upload $objectPath after $UPLOAD_MAX_ATTEMPTS attempts: ${lastError?.message}",
            lastError,
        )
    }

    private suspend fun verifyTiles(plan: MapPublishPlan) {
        val coordinates = buildSet {
            add(Triple(0, 0, 0))
            val scheme = TileScheme(plan.widthPx, plan.heightPx, plan.tileSize)
            val random = Random(plan.mapVersion)
            repeat(VERIFY_SAMPLE_SIZE) {
                val zoom = random.nextInt(scheme.minZoom, scheme.maxZoom + 1)
                val x = random.nextInt(0, scheme.tilesWide(zoom))
                val y = random.nextInt(0, scheme.tilesHigh(zoom))
                add(Triple(zoom, x, y))
            }
        }

        for ((zoom, x, y) in coordinates) {
            val url = TileUrl.build(plan.tileUrlTemplate, zoom, x, y)
            val response = withTimeoutOrNull(VERIFY_TIMEOUT_MILLIS) { httpClient.get(url) }
                ?: throw IllegalStateException("Tile $url didn't respond within ${VERIFY_TIMEOUT_MILLIS / 1000}s.")
            if (!response.status.isSuccess()) {
                throw IllegalStateException("Tile $url returned HTTP ${response.status.value}.")
            }
        }
    }
}

/**
 * The `v<mapVersion>/...` path convention shared by every object this publisher writes — pulled
 * out as pure, parameter-only functions (no `plan`/`this` needed) so a test can assert the
 * `tile_url_template` it builds and the object paths [SupabaseMapPublisher] actually uploads to
 * agree with each other without any network involved.
 */
/** Every "Load map image..." run through this publisher uses this format — see [TileFormat]'s own KDoc for why WebP is the default. */
internal val DEFAULT_TILE_FORMAT = TileFormat.WEBP

internal fun tileUrlTemplate(
    baseUrl: String,
    mapVersion: Int,
    format: TileFormat = DEFAULT_TILE_FORMAT
): String =
    "$baseUrl/storage/v1/object/public/$MAP_TILES_BUCKET/${
        tileObjectPath(
            mapVersion,
            "{z}/{x}/{y}.${format.extension}"
        )
    }"

internal fun tileObjectPath(mapVersion: Int, relativePath: String): String =
    "v$mapVersion/$relativePath"

internal fun sourceImageObjectPath(mapVersion: Int, sourceExtension: String): String =
    "v$mapVersion/source.${sourceExtension.lowercase().ifEmpty { "png" }}"

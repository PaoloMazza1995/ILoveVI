package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.core.model.map.TileUrl
import com.pamaz.vi.tools.mapeditor.data.FakeRemoteTileStorage
import com.pamaz.vi.tools.mapeditor.data.fake.FakeMapConfigRepository
import com.pamaz.vi.tools.mapeditor.data.fake.FakePinRepository
import com.pamaz.vi.tools.mapeditor.model.MapPublishOutcome
import com.pamaz.vi.tools.mapeditor.model.MapPublishPlanResult
import com.pamaz.vi.tools.mapeditor.supabase.SupabaseClientFactory
import com.pamaz.vi.tools.mapeditor.supabase.SupabaseConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupabaseMapPublisherTest {

    // --- tile_url_template <-> uploaded object paths (§2f / §5 "matches the uploaded paths") ----

    @Test
    fun tileUrlTemplate_resolvesToTheSamePathsObjectsAreUploadedUnder() {
        val template = tileUrlTemplate("https://project.supabase.co", mapVersion = 3)
        assertEquals(
            "https://project.supabase.co/storage/v1/object/public/map-tiles/v3/{z}/{x}/{y}.png",
            template,
        )

        val resolvedUrl = TileUrl.build(template, z = 2, x = 1, y = 0)
        val uploadedObjectPath = tileObjectPath(mapVersion = 3, relativePath = "2/1/0.png")

        assertTrue(resolvedUrl.endsWith("/map-tiles/$uploadedObjectPath"))
    }

    @Test
    fun sourceImageObjectPath_livesUnderTheSameVersionFolderAsItsTiles_andDefaultsToPng() {
        assertEquals("v1/source.png", sourceImageObjectPath(mapVersion = 1, sourceExtension = ""))
        assertEquals(
            "v1/source.jpg",
            sourceImageObjectPath(mapVersion = 1, sourceExtension = "JPG")
        )
        assertTrue(sourceImageObjectPath(mapVersion = 5, sourceExtension = "png").startsWith("v5/"))
    }

    // --- retry skips files already uploaded (§2d / §5) -------------------------------------------

    @Test
    fun retryingAFailedPublish_doesNotReUploadAFileThatAlreadySucceeded() = runBlocking {
        val tempDir = createTempDirectory("map-publisher-test-")
        try {
            val sourceFile = tempDir.resolve("source.png").toFile()
            ImageIO.write(syntheticImage(256, 256), "png", sourceFile)

            val storage = FakeRemoteTileStorage()
            val mapConfigRepository = FakeMapConfigRepository()
            val publisher = SupabaseMapPublisher(
                storage = storage,
                pinRepository = FakePinRepository(),
                mapConfigRepository = mapConfigRepository,
                // Never reached: VERIFY always fails first (404 mock below), so REGISTER never
                // runs and this dummy client is never asked to make a network call.
                mapConfigWriter = MapConfigWriter(
                    SupabaseClientFactory.create(
                        SupabaseConfig(
                            "https://example.supabase.co",
                            "anon-key"
                        )
                    ),
                ),
                httpClient = HttpClient(MockEngine {
                    respond(
                        "not found",
                        HttpStatusCode.NotFound
                    )
                }),
                baseUrl = "https://example.supabase.co",
            )

            val plan = (publisher.plan(sourceFile) as MapPublishPlanResult.Valid).plan
            val tilePath = tileObjectPath(plan.mapVersion, "0/0/0.png")
            val sourcePath = sourceImageObjectPath(plan.mapVersion, sourceFile.extension)

            // First attempt: the source image upload fails every time; the tile succeeds.
            storage.failPaths += sourcePath
            val firstOutcome = publisher.publish(plan) { }
            assertTrue(firstOutcome is MapPublishOutcome.Failure)
            assertEquals(1, storage.uploadCallCounts[tilePath])
            assertTrue((storage.uploadCallCounts[sourcePath] ?: 0) >= 1)
            val sourceAttemptsBeforeRetry = storage.uploadCallCounts.getValue(sourcePath)

            // Retry, with the earlier failure now fixed.
            storage.failPaths.clear()
            val secondOutcome = publisher.publish(plan) { }

            // the tile was never re-uploaded...
            assertEquals(1, storage.uploadCallCounts[tilePath])
            // ...but the source image, which never succeeded, is retried exactly once more.
            assertEquals(sourceAttemptsBeforeRetry + 1, storage.uploadCallCounts[sourcePath])
            // VERIFY fails on both attempts (404 mock), by design, so REGISTER is never reached.
            assertTrue(secondOutcome is MapPublishOutcome.Failure)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    private fun syntheticImage(width: Int, height: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                image.setRGB(x, y, ((x * 255 / width) shl 16) or ((y * 255 / height) shl 8))
            }
        }
        return image
    }
}

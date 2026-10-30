package com.pamaz.vi.tools.mapeditor.data.fake

import com.pamaz.vi.tools.mapeditor.model.MapPublishOutcome
import com.pamaz.vi.tools.mapeditor.model.MapPublishPlanResult
import com.pamaz.vi.tools.mapeditor.model.MapPublishStep
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [FakeMapPublisher] mirrors [com.pamaz.vi.tools.mapeditor.data.supabase.SupabaseMapPublisher]'s
 * step ordering (`MapPublishPlanning` is shared by both), so "registration happens only after all
 * uploads" (§5) is exercised here against the fake, which is also what `EditorStateTest`/the UI
 * preview actually run against.
 */
class FakeMapPublisherTest {

    private fun sourceImageFile(): File {
        val tempDir = createTempDirectory("fake-map-publisher-test-")
        val file = tempDir.resolve("source.png").toFile()
        val image = BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB)
        ImageIO.write(image, "png", file)
        return file
    }

    @Test
    fun aFailureBeforeRegister_neverTouchesMapConfig() = runBlocking {
        val mapConfigRepository = FakeMapConfigRepository()
        val before = mapConfigRepository.currentConfig
        val publisher = FakeMapPublisher(
            mapConfigRepository,
            FakePinRepository(),
            forceFailureAtStep = MapPublishStep.UPLOAD
        )

        val plan = (publisher.plan(sourceImageFile()) as MapPublishPlanResult.Valid).plan
        val outcome = publisher.publish(plan) { }

        assertTrue(outcome is MapPublishOutcome.Failure)
        assertEquals(before, mapConfigRepository.currentConfig)
    }

    @Test
    fun aFailureDuringVerify_alsoNeverTouchesMapConfig() = runBlocking {
        val mapConfigRepository = FakeMapConfigRepository()
        val before = mapConfigRepository.currentConfig
        val publisher = FakeMapPublisher(
            mapConfigRepository,
            FakePinRepository(),
            forceFailureAtStep = MapPublishStep.VERIFY
        )

        val plan = (publisher.plan(sourceImageFile()) as MapPublishPlanResult.Valid).plan
        val outcome = publisher.publish(plan) { }

        assertTrue(outcome is MapPublishOutcome.Failure)
        assertEquals(before, mapConfigRepository.currentConfig)
    }

    @Test
    fun onlyAFullySuccessfulPublish_updatesMapConfig() = runBlocking {
        val mapConfigRepository = FakeMapConfigRepository()
        val before = mapConfigRepository.currentConfig
        val publisher = FakeMapPublisher(mapConfigRepository, FakePinRepository())

        val plan = (publisher.plan(sourceImageFile()) as MapPublishPlanResult.Valid).plan
        val outcome = publisher.publish(plan) { }

        assertTrue(outcome is MapPublishOutcome.Success)
        assertTrue(mapConfigRepository.currentConfig != before)
        assertEquals(plan.mapVersion, mapConfigRepository.currentConfig.mapVersion)
    }
}

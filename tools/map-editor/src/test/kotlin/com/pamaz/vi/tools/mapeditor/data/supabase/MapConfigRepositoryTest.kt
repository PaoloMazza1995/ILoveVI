package com.pamaz.vi.tools.mapeditor.data.supabase

import com.pamaz.vi.tools.mapeditor.data.MapConfigResult
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The bug this fixes: a bad `map_config` row used to throw straight out of
 * [MapConfigRepository.loadCurrent] (uncaught `IllegalArgumentException` from [com.pamaz.vi.core.model.MapConfig]'s
 * own `require(...)` checks), killing whatever coroutine called it. Every test here exercises the
 * real `loadCurrent()` — real Postgrest query building and JSON decoding, only the HTTP transport
 * is a [MockEngine] — and asserts it returns a normal [MapConfigResult], never an exception.
 */
class MapConfigRepositoryTest {

    private fun repositoryRespondingWith(json: String): MapConfigRepository {
        val engine = MockEngine {
            respond(
                content = json,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = createSupabaseClient(
            supabaseUrl = "https://example.supabase.co",
            supabaseKey = "anon-key"
        ) {
            install(Postgrest)
            httpEngine = engine
        }
        return MapConfigRepository(client)
    }

    private fun rowJson(
        mapVersion: Int = 1,
        sourceImageUrl: String = "https://example.com/map.png",
        widthPx: Int = 4096,
        heightPx: Int = 4096,
        tileUrlTemplate: String = "https://example.com/{z}/{x}/{y}.png",
        minZoom: Int = 0,
        maxZoom: Int = 4,
    ) = """{"map_version":$mapVersion,"source_image_url":"$sourceImageUrl","width_px":$widthPx,""" +
            """"height_px":$heightPx,"tile_url_template":"$tileUrlTemplate","min_zoom":$minZoom,""" +
            """"max_zoom":$maxZoom,"updated_at":"2026-01-01T00:00:00Z"}"""

    @Test
    fun loadCurrent_noRow_returnsMissing_neverThrows() = runBlocking {
        val repository = repositoryRespondingWith("[]")
        assertEquals(MapConfigResult.Missing, repository.loadCurrent())
    }

    @Test
    fun loadCurrent_validRow_returnsLoaded() = runBlocking {
        val repository = repositoryRespondingWith("[${rowJson()}]")
        val result = repository.loadCurrent()
        assertIs<MapConfigResult.Loaded>(result)
        assertEquals(1, result.config.mapVersion)
        assertEquals(4096, result.config.widthPx)
    }

    @Test
    fun loadCurrent_templateMissingPlaceholders_returnsInvalid_withTheRawRow_neverThrows() =
        runBlocking {
            val badTemplate = "https://example.com/no-placeholders.png"
            val repository = repositoryRespondingWith("[${rowJson(tileUrlTemplate = badTemplate)}]")

            val result = repository.loadCurrent()

            assertIs<MapConfigResult.Invalid>(result)
            assertTrue(result.reason.contains("{z}"))
            assertEquals(badTemplate, result.rawRow.tileUrlTemplate)
            assertEquals(1, result.rawRow.mapVersion)
        }

    @Test
    fun loadCurrent_zeroWidth_returnsInvalid_neverThrows(): Unit = runBlocking {
        val repository = repositoryRespondingWith("[${rowJson(widthPx = 0)}]")
        assertIs<MapConfigResult.Invalid>(repository.loadCurrent())
    }

    @Test
    fun loadCurrent_negativeHeight_returnsInvalid_neverThrows(): Unit = runBlocking {
        val repository = repositoryRespondingWith("[${rowJson(heightPx = -10)}]")
        assertIs<MapConfigResult.Invalid>(repository.loadCurrent())
    }

    @Test
    fun loadCurrent_minZoomGreaterThanMaxZoom_returnsInvalid_neverThrows(): Unit = runBlocking {
        val repository = repositoryRespondingWith("[${rowJson(minZoom = 5, maxZoom = 2)}]")
        assertIs<MapConfigResult.Invalid>(repository.loadCurrent())
    }

    @Test
    fun loadCurrent_emptySourceImageUrl_returnsInvalid_withReason_neverThrows() = runBlocking {
        val repository = repositoryRespondingWith("[${rowJson(sourceImageUrl = "")}]")
        val result = repository.loadCurrent()
        assertIs<MapConfigResult.Invalid>(result)
        assertTrue(result.reason.contains("source_image_url"))
    }

    @Test
    fun loadAll_skipsInvalidRows_insteadOfThrowing() = runBlocking {
        val repository = repositoryRespondingWith(
            "[${rowJson(mapVersion = 2)}, ${rowJson(mapVersion = 1, widthPx = 0)}]",
        )
        val all = repository.loadAll()
        assertEquals(listOf(2), all.map { it.mapVersion })
    }
}

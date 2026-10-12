package com.pamaz.vi.tools.tilegen

import com.pamaz.vi.core.model.MapConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class SupabaseAdminClientTest {

    private val config = SupabaseAdminConfig(
        baseUrl = "https://example.supabase.co",
        serviceRoleKey = "service-role-key"
    )

    private fun jsonClient(
        requests: MutableList<HttpRequestData>,
        respondWith: String = "[]"
    ): HttpClient {
        val engine = MockEngine { request ->
            requests.add(request)
            respond(
                content = respondWith,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
    }

    @Test
    fun nextMapVersion_returnsCurrentMaxPlusOne() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val client = jsonClient(requests, """[{"map_version":5}]""")

        assertEquals(6, SupabaseAdminClient.nextMapVersion(client, config))
        assertEquals(1, requests.size)
        assertTrue(requests.single().url.toString().contains("/rest/v1/map_config"))
    }

    @Test
    fun nextMapVersion_defaultsToOneWhenTableEmpty() = runBlocking {
        val client = jsonClient(mutableListOf(), "[]")
        assertEquals(1, SupabaseAdminClient.nextMapVersion(client, config))
    }

    @Test
    fun insertMapConfig_sendsSnakeCaseBodyAndAuthHeaders() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val client = jsonClient(requests)

        val mapConfig = MapConfig(
            mapVersion = 3,
            sourceImageUrl = "https://example.com/map.png",
            widthPx = 8192,
            heightPx = 6144,
            tileUrlTemplate = "https://example.supabase.co/storage/v1/object/public/map-tiles/3/{z}/{x}/{y}.png",
            minZoom = 0,
            maxZoom = 5,
            updatedAt = Instant.fromEpochMilliseconds(0),
        )

        SupabaseAdminClient.insertMapConfig(client, config, mapConfig, dryRun = false)

        assertEquals(1, requests.size)
        val request = requests.single()
        assertEquals("service-role-key", request.headers["apikey"])
        assertEquals("Bearer service-role-key", request.headers[HttpHeaders.Authorization])

        val body = (request.body as TextContent).text
        assertTrue("\"map_version\":3" in body)
        assertTrue("\"source_image_url\":\"https://example.com/map.png\"" in body)
        assertTrue("\"width_px\":8192" in body)
        assertTrue("\"min_zoom\":0" in body)
        assertTrue("\"max_zoom\":5" in body)
    }

    @Test
    fun insertMapConfig_dryRun_sendsNoRequest() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val client = jsonClient(requests)

        val mapConfig = MapConfig(
            mapVersion = 1,
            sourceImageUrl = "https://example.com/map.png",
            widthPx = 10,
            heightPx = 10,
            tileUrlTemplate = "https://x/{z}/{x}/{y}.png",
            minZoom = 0,
            maxZoom = 0,
            updatedAt = Instant.fromEpochMilliseconds(0),
        )

        val result = SupabaseAdminClient.insertMapConfig(client, config, mapConfig, dryRun = true)

        assertEquals(0, requests.size)
        assertEquals(mapConfig, result)
    }

    @Test
    fun uploadTiles_uploadsEveryFileUnderVersionDirWithUpsertHeader() = runBlocking {
        val tempDir = createTempDirectory("tilegen-upload-test-")
        try {
            val tileDir = tempDir.resolve("3").resolve("0").resolve("0")
            Files.createDirectories(tileDir)
            tileDir.resolve("0.png").writeBytes(byteArrayOf(1, 2, 3))

            val requests = mutableListOf<HttpRequestData>()
            val engine = MockEngine { request ->
                requests.add(request)
                respond(content = "", status = HttpStatusCode.OK)
            }
            val client = HttpClient(engine)

            val count = SupabaseAdminClient.uploadTiles(
                client,
                config,
                tempDir,
                mapVersion = 3,
                dryRun = false
            )

            assertEquals(1, count)
            assertEquals(1, requests.size)
            assertTrue(
                requests.single().url.toString()
                    .endsWith("/storage/v1/object/map-tiles/3/0/0/0.png")
            )
            assertEquals("true", requests.single().headers["x-upsert"])
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun uploadTiles_dryRun_countsFilesWithoutSendingRequests() = runBlocking {
        val tempDir = createTempDirectory("tilegen-upload-test-")
        try {
            val tileDir = tempDir.resolve("3").resolve("0").resolve("0")
            Files.createDirectories(tileDir)
            tileDir.resolve("0.png").writeBytes(byteArrayOf(1, 2, 3))
            tileDir.resolve("1.png").writeBytes(byteArrayOf(4, 5, 6))

            val requests = mutableListOf<HttpRequestData>()
            val engine = MockEngine { request ->
                requests.add(request)
                respond(content = "", status = HttpStatusCode.OK)
            }
            val client = HttpClient(engine)

            val count = SupabaseAdminClient.uploadTiles(
                client,
                config,
                tempDir,
                mapVersion = 3,
                dryRun = true
            )

            assertEquals(2, count)
            assertEquals(0, requests.size)
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}

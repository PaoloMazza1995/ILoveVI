package com.pamaz.vi.tools.tilegen

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SourceImageFetcherTest {

    @Test
    fun rejectsNonHttpsUrl(): Unit = runBlocking {
        val engine =
            MockEngine { error("should never reach the network for a URL rejected before the request") }
        val client = HttpClient(engine)
        val tempDir = createTempDirectory("tilegen-fetch-test-")
        try {
            assertFailsWith<IllegalArgumentException> {
                SourceImageFetcher.fetch("http://example.com/map.png", tempDir, client)
            }
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun rejectsNonSuccessStatus(): Unit = runBlocking {
        val engine = MockEngine { respond(content = "not found", status = HttpStatusCode.NotFound) }
        val client = HttpClient(engine)
        val tempDir = createTempDirectory("tilegen-fetch-test-")
        try {
            assertFailsWith<IllegalArgumentException> {
                SourceImageFetcher.fetch("https://example.com/map.png", tempDir, client)
            }
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun rejectsNonImageContentType(): Unit = runBlocking {
        val engine = MockEngine {
            respond(
                content = "<html></html>",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }
        val client = HttpClient(engine)
        val tempDir = createTempDirectory("tilegen-fetch-test-")
        try {
            assertFailsWith<IllegalArgumentException> {
                SourceImageFetcher.fetch("https://example.com/map.png", tempDir, client)
            }
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun downloadsImageBytesToTempDir() = runBlocking {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val engine = MockEngine {
            respond(
                content = bytes,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "image/png"),
            )
        }
        val client = HttpClient(engine)
        val tempDir = createTempDirectory("tilegen-fetch-test-")
        try {
            val path = SourceImageFetcher.fetch("https://example.com/map.png", tempDir, client)
            assertTrue(path.toString().endsWith(".png"))
            assertContentEquals(bytes, path.readBytes())
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }
}

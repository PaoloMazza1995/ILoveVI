package com.pamaz.vi.core.network.datasource

import com.pamaz.vi.core.network.NetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

private fun clientWith(engine: MockEngine): HttpClient = HttpClient(engine) {
    expectSuccess = true
    install(HttpTimeout)
}

class NewsFeedFetcherTest {
    @Test
    fun fetchXml_returnsTheBodyTextOnA200() = runTest {
        val engine = MockEngine { respond("<rss></rss>", HttpStatusCode.OK) }
        val fetcher = NewsFeedFetcherImpl(clientWith(engine))

        val result = fetcher.fetchXml("https://example.com/feed.xml")

        val success = assertIs<NetworkResult.Success<String>>(result)
        kotlin.test.assertEquals("<rss></rss>", success.data)
    }

    @Test
    fun fetchXml_aNon2xxStatusIsAServerError() = runTest {
        val engine = MockEngine { respond("not found", HttpStatusCode.NotFound) }
        val fetcher = NewsFeedFetcherImpl(clientWith(engine))

        val result = fetcher.fetchXml("https://example.com/missing.xml")

        val error = assertIs<NetworkResult.ServerError>(result)
        kotlin.test.assertEquals(404, error.code)
    }

    @Test
    fun fetchXml_aDeclaredContentLengthOverTheLimitIsRejectedBeforeReadingTheBody() = runTest {
        val tooLarge = (5L * 1024 * 1024 + 1).toString()
        val engine = MockEngine {
            respond(
                content = "irrelevant",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentLength, tooLarge),
            )
        }
        val fetcher = NewsFeedFetcherImpl(clientWith(engine))

        val result = fetcher.fetchXml("https://example.com/huge.xml")

        assertIs<NetworkResult.Offline>(result)
    }

    @Test
    fun fetchXml_aBodyThatStreamsPastTheLimitWithNoHonestContentLengthIsStillRejected() = runTest {
        val tooLargeBody = "x".repeat(5 * 1024 * 1024 + 1)
        val engine = MockEngine { respond(tooLargeBody, HttpStatusCode.OK) }
        val fetcher = NewsFeedFetcherImpl(clientWith(engine))

        val result = fetcher.fetchXml("https://example.com/huge.xml")

        assertIs<NetworkResult.Offline>(result)
    }
}

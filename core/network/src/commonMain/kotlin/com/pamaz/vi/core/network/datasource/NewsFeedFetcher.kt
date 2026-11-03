package com.pamaz.vi.core.network.datasource

import com.pamaz.vi.core.network.NetworkResult
import com.pamaz.vi.core.network.networkHttpClientEngine
import com.pamaz.vi.core.network.safeNetworkCall
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.contentLength
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray

private const val NEWS_FEED_REQUEST_TIMEOUT_MILLIS = 10_000L
private const val NEWS_FEED_CONNECT_TIMEOUT_MILLIS = 10_000L
private const val NEWS_FEED_SOCKET_TIMEOUT_MILLIS = 10_000L

/** Generous for a text feed, small enough to bound one misbehaving feed's memory/time cost on-device. */
private const val NEWS_FEED_MAX_BYTES = 5L * 1024 * 1024

/**
 * Thrown when a feed declares, or turns out to stream, more than [NEWS_FEED_MAX_BYTES]. Not one of
 * [NetworkResult]'s existing variants (`ServerError`/`Timeout`/`ParseError` all mean something more
 * specific) — [safeNetworkCall]'s generic `catch (e: Exception)` classifies it as [NetworkResult.Offline],
 * the same pragmatic "anything unclassified" bucket that module's own KDoc already documents.
 */
internal class FeedTooLargeException(message: String) : Exception(message)

/**
 * Downloads one feed's raw XML text over HTTPS — the on-device replacement for a backend
 * `fetch-news` job: `ContentSyncerImpl.syncNews()` (`:core:data`) is the only caller, once per
 * enabled `FeedSource`, independently per feed so one feed's failure never affects another's.
 */
interface NewsFeedFetcher {
    suspend fun fetchXml(url: String): NetworkResult<String>
}

internal class NewsFeedFetcherImpl(
    private val client: HttpClient = HttpClient(networkHttpClientEngine()) {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = NEWS_FEED_REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = NEWS_FEED_CONNECT_TIMEOUT_MILLIS
            socketTimeoutMillis = NEWS_FEED_SOCKET_TIMEOUT_MILLIS
        }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            exponentialDelay()
        }
    },
) : NewsFeedFetcher {
    override suspend fun fetchXml(url: String): NetworkResult<String> = safeNetworkCall {
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, "IloveVI-NewsSync/1.0")
        }

        // Content-Length isn't guaranteed present or accurate (chunked encoding, a lying
        // server), so it's checked up front as a cheap early-out, but the real limit is enforced
        // below too, streaming, rather than trusting the header alone.
        val declaredLength = response.contentLength()
        if (declaredLength != null && declaredLength > NEWS_FEED_MAX_BYTES) {
            throw FeedTooLargeException("feed declared $declaredLength bytes, over the $NEWS_FEED_MAX_BYTES limit")
        }

        val channel = response.bodyAsChannel()
        val bytes = channel.readBuffer(NEWS_FEED_MAX_BYTES + 1).readByteArray()
        if (bytes.size > NEWS_FEED_MAX_BYTES || !channel.isClosedForRead) {
            throw FeedTooLargeException("feed exceeded the $NEWS_FEED_MAX_BYTES byte limit")
        }
        bytes.decodeToString()
    }
}

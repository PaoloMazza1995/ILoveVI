package com.pamaz.vi.core.network

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class NetworkResultTest {
    @Test
    fun successfulBlock_becomesSuccess() = runTest {
        val result = safeNetworkCall { "weapons" }
        val success = assertIs<NetworkResult.Success<String>>(result)
        assertEquals("weapons", success.data)
    }

    @Test
    fun requestTimeoutException_becomesTimeout() = runTest {
        val result = safeNetworkCall<Unit> {
            throw HttpRequestTimeoutException("https://example.com", 15_000L, null)
        }
        assertIs<NetworkResult.Timeout>(result)
    }

    @Test
    fun connectTimeoutException_becomesTimeout() = runTest {
        val result = safeNetworkCall<Unit> {
            throw ConnectTimeoutException("connect timed out", null)
        }
        assertIs<NetworkResult.Timeout>(result)
    }

    @Test
    fun serializationException_becomesParseError() = runTest {
        val result = safeNetworkCall<Unit> {
            throw SerializationException("unexpected JSON shape")
        }
        assertIs<NetworkResult.ParseError>(result)
    }

    @Test
    fun unclassifiedThrowable_becomesOffline() = runTest {
        val result = safeNetworkCall<Unit> {
            throw RuntimeException("DNS resolution failed")
        }
        assertIs<NetworkResult.Offline>(result)
    }

    @Test
    fun cancellation_isNeverSwallowed() = runTest {
        assertFailsWith<CancellationException> {
            safeNetworkCall<Unit> { throw CancellationException("cancelled") }
        }
    }
}

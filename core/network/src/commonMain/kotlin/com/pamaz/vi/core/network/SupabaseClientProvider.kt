package com.pamaz.vi.core.network

import com.pamaz.vi.core.network.config.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout

/** Request-level timeout: the whole call, including any retries, must finish within this. */
private const val NETWORK_REQUEST_TIMEOUT_MILLIS = 15_000L

/** TCP handshake timeout. */
private const val NETWORK_CONNECT_TIMEOUT_MILLIS = 10_000L

/** Time allowed between two bytes on an already-open socket. */
private const val NETWORK_SOCKET_TIMEOUT_MILLIS = 15_000L

/** Every request this module makes is a plain-read `select` (idempotent), so retrying on a 5xx/network blip is always safe. */
private const val NETWORK_MAX_RETRIES = 2

/**
 * Builds the one [SupabaseClient] every remote data source in this module shares: HTTPS only (no
 * `useHTTPS = false` override), [Postgrest] installed (the only Supabase module this read-only
 * library needs — no [io.github.jan.supabase.auth.Auth], since the app never signs in), with
 * request/connect/socket timeouts and a small exponential-backoff retry for transient failures.
 * [engine] defaults to [networkHttpClientEngine] (OkHttp on Android/JVM, Darwin on iOS); a test
 * passes a [io.ktor.client.engine.mock.MockEngine] instead.
 */
object SupabaseClientProvider {
    @OptIn(SupabaseInternal::class)
    fun create(
        config: SupabaseConfig,
        engine: HttpClientEngine = networkHttpClientEngine(),
    ): SupabaseClient = createSupabaseClient(
        supabaseUrl = config.url,
        supabaseKey = config.anonKey,
    ) {
        useHTTPS = true
        httpEngine = engine
        install(Postgrest)
        httpConfig {
            install(HttpTimeout) {
                requestTimeoutMillis = NETWORK_REQUEST_TIMEOUT_MILLIS
                connectTimeoutMillis = NETWORK_CONNECT_TIMEOUT_MILLIS
                socketTimeoutMillis = NETWORK_SOCKET_TIMEOUT_MILLIS
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = NETWORK_MAX_RETRIES)
                exponentialDelay()
            }
        }
    }
}

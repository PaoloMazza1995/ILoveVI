package com.pamaz.vi.core.network

import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/**
 * Every outcome a remote data source call can produce. No exception from a Ktor/supabase-kt call
 * ever reaches a caller of this module directly — [safeNetworkCall] is the one place that catches
 * and classifies them into one of these.
 */
sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>

    /** No usable connection at all — DNS failure, connection refused, airplane mode, etc. */
    data object Offline : NetworkResult<Nothing>

    /** The request/connect/socket timeout configured in [SupabaseClientProvider] was hit. */
    data object Timeout : NetworkResult<Nothing>

    /** The server answered with an HTTP error status. */
    data class ServerError(val code: Int) : NetworkResult<Nothing>

    /** The response body wasn't the JSON shape a DTO expected. */
    data object ParseError : NetworkResult<Nothing>
}

/**
 * Runs [block], classifying any thrown exception into a [NetworkResult] instead of letting it
 * escape — this is what makes this module's public API exception-free. Order matters: the more
 * specific Ktor/supabase-kt exception types are checked first, since some of them subclass
 * `IOException`/`IllegalStateException` and would otherwise be caught by a broader branch.
 * [CancellationException] is always rethrown, never classified — swallowing it would break
 * structured concurrency (a cancelled caller must stay cancelled).
 *
 * The `else` branch below deliberately maps to [NetworkResult.Offline] rather than a fifth
 * "unknown" variant: this module avoids referencing platform-specific exception types (e.g. a
 * bare `java.net.SocketTimeoutException` thrown below Ktor's own timeout plugin on the
 * OkHttp/Android engine) from `commonMain`, so an unrecognized throwable — overwhelmingly a raw
 * connectivity failure in practice — falls back to the same bucket a DNS/connection-refused error
 * would. See this module's README's "Error mapping" section.
 */
internal suspend fun <T> safeNetworkCall(block: suspend () -> T): NetworkResult<T> = try {
    NetworkResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: RestException) {
    NetworkResult.ServerError(e.statusCode)
} catch (e: ResponseException) {
    NetworkResult.ServerError(e.response.status.value)
} catch (e: HttpRequestTimeoutException) {
    NetworkResult.Timeout
} catch (e: ConnectTimeoutException) {
    NetworkResult.Timeout
} catch (e: SerializationException) {
    NetworkResult.ParseError
} catch (e: Exception) {
    NetworkResult.Offline
}

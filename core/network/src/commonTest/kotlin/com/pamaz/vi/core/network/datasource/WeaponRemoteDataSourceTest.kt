package com.pamaz.vi.core.network.datasource

import com.pamaz.vi.core.network.NetworkResult
import com.pamaz.vi.core.network.SupabaseClientProvider
import com.pamaz.vi.core.network.config.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

private val testConfig =
    SupabaseConfig(url = "https://example.supabase.co", anonKey = "test-anon-key")

private fun clientWith(engine: MockEngine): SupabaseClient =
    SupabaseClientProvider.create(testConfig, engine)

private const val WEAPONS_PAGE_JSON = """
[
  {"id":"weapon-1","name":"Pistol","description":"d","sub_type":"PISTOL","updated_at":"2026-01-01T00:00:00Z"},
  {"id":"weapon-2","name":"Rifle","description":"d","sub_type":"RIFLE","updated_at":"2026-01-02T00:00:00Z"}
]
"""

class WeaponRemoteDataSourceTest {
    @Test
    fun firstSync_omitsUpdatedAtFilter_andSendsOrderAndLimit() = runTest {
        var capturedQuery: String? = null
        val engine = MockEngine { request ->
            capturedQuery = request.url.encodedQuery
            respond(
                content = WEAPONS_PAGE_JSON,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val dataSource = WeaponRemoteDataSourceImpl(clientWith(engine))

        val result = dataSource.fetchChanges(since = null, pageSize = 50)

        val success = assertIs<NetworkResult.Success<List<*>>>(result)
        assertEquals(2, success.data.size)
        val query = requireNotNull(capturedQuery)
        assertTrue(
            "updated_at=gt." !in query,
            "since=null must not add an updated_at filter, got: $query"
        )
        assertTrue(
            "order=updated_at.asc.nullslast%2Cid.asc.nullslast" in query,
            "expected order by updated_at then id, got: $query",
        )
        assertTrue("limit=50" in query, "expected limit=50, got: $query")
    }

    @Test
    fun resync_sendsUpdatedAtGreaterThanSince() = runTest {
        var capturedQuery: String? = null
        val engine = MockEngine { request ->
            capturedQuery = request.url.encodedQuery
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val dataSource = WeaponRemoteDataSourceImpl(clientWith(engine))
        val since = Instant.parse("2026-01-01T00:00:00Z")

        val result = dataSource.fetchChanges(since = since, pageSize = 10)

        assertIs<NetworkResult.Success<List<*>>>(result)
        val query = requireNotNull(capturedQuery)
        assertTrue(
            "updated_at=gt.2026-01-01T00%3A00%3A00Z" in query,
            "expected a gt filter on updated_at, got: $query"
        )
        assertTrue("limit=10" in query, "expected limit=10, got: $query")
    }

    @Test
    fun shortPage_meansNoMoreRows_fullPage_meansMaybeMore() = runTest {
        val shortPageEngine = MockEngine {
            respond(
                content = WEAPONS_PAGE_JSON,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val result = WeaponRemoteDataSourceImpl(clientWith(shortPageEngine)).fetchChanges(
            since = null,
            pageSize = 50
        )
        val success = assertIs<NetworkResult.Success<List<*>>>(result)
        assertTrue(
            success.data.size < 50,
            "a page shorter than pageSize signals the caller there is nothing left to re-fetch"
        )
    }

    @Test
    fun serverError_becomesServerErrorWithStatusCode() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"message":"boom","code":"XX000","details":null,"hint":null}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val result =
            WeaponRemoteDataSourceImpl(clientWith(engine)).fetchChanges(since = null, pageSize = 50)
        val error = assertIs<NetworkResult.ServerError>(result)
        assertEquals(500, error.code)
    }

    @Test
    fun malformedBody_becomesParseError() = runTest {
        val engine = MockEngine {
            respond(
                content = "not json at all",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val result =
            WeaponRemoteDataSourceImpl(clientWith(engine)).fetchChanges(since = null, pageSize = 50)
        assertIs<NetworkResult.ParseError>(result)
    }

    @Test
    fun noConnection_becomesOffline() = runTest {
        val engine = MockEngine { throw RuntimeException("Failed to connect: no network") }
        val result =
            WeaponRemoteDataSourceImpl(clientWith(engine)).fetchChanges(since = null, pageSize = 50)
        assertIs<NetworkResult.Offline>(result)
    }
}

package com.pamaz.vi.tools.mapeditor.data.supabase.auth

import com.pamaz.vi.tools.mapeditor.model.Session
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `install(Auth)` (`supabase/SupabaseClientFactory.kt`) persists the access/refresh token pair
 * via its own [io.github.jan.supabase.auth.SessionManager] and reloads it on init
 * (`AuthConfigDefaults.autoLoadFromStorage`) — these tests swap in a [MemorySessionManager] so
 * that behavior is exercised without touching this machine's real settings storage, and assert
 * [SupabaseAuthService.restoreSession] turns whatever the SDK reloaded into this app's [Session].
 */
class SupabaseAuthServiceTest {

    private fun serviceRestoringSession(
        session: UserSession?,
        scope: CoroutineScope
    ): SupabaseAuthService {
        val client = createSupabaseClient(
            supabaseUrl = "https://example.supabase.co",
            supabaseKey = "anon-key"
        ) {
            install(Auth) { sessionManager = MemorySessionManager(session) }
            httpEngine = MockEngine { respond("not found", HttpStatusCode.NotFound) }
        }
        return SupabaseAuthService(client, scope)
    }

    private fun userSession(email: String, role: String?) = UserSession(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresIn = 3600,
        tokenType = "bearer",
        user = UserInfo(
            aud = "authenticated",
            id = "user-1",
            email = email,
            appMetadata = role?.let { buildJsonObject { put("role", JsonPrimitive(it)) } },
        ),
    )

    @Test
    fun restoreSessionReturnsNullWhenNothingWasPersisted() = runBlocking {
        val service = serviceRestoringSession(session = null, scope = this)

        assertNull(service.restoreSession())
        assertNull(service.session.value)
    }

    @Test
    fun restoreSessionRebuildsAnAdminSessionFromThePersistedToken() = runBlocking {
        val service =
            serviceRestoringSession(userSession("admin@example.com", "admin"), scope = this)

        val restored = service.restoreSession()

        assertEquals(Session("admin@example.com", isAdmin = true), restored)
        assertEquals(restored, service.session.value)
    }

    @Test
    fun restoreSessionRebuildsANonAdminSessionWhenTheRoleClaimIsMissing() = runBlocking {
        val service =
            serviceRestoringSession(userSession("player@example.com", role = null), scope = this)

        val restored = service.restoreSession()

        assertEquals(Session("player@example.com", isAdmin = false), restored)
    }
}

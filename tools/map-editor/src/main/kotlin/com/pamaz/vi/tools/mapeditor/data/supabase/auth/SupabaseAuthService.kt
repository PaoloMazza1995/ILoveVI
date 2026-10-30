package com.pamaz.vi.tools.mapeditor.data.supabase.auth

import com.pamaz.vi.tools.mapeditor.data.AuthService
import com.pamaz.vi.tools.mapeditor.model.Session
import com.pamaz.vi.tools.mapeditor.model.SignInResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** The exact `app_metadata` claim `backend/supabase/migrations/20261011120001_extensions_and_helpers.sql`'s `is_admin()` checks. */
private const val ROLE_CLAIM = "role"
private const val ADMIN_ROLE = "admin"

/**
 * EDT-01/02: real email/password sign-in against Supabase Auth, same credential check
 * [AdminAuthRepository] already does, but a **different** outcome for a non-admin account — this
 * step's [Session]/[SignInResult] (see that class's own KDoc) wants a non-admin to stay signed in
 * (`isAdmin = false`, only Commit disabled), whereas [AdminAuthRepository] signs them out locally
 * and reports [com.pamaz.vi.tools.mapeditor.model.AdminSignInResult.NotAnAdmin] — a genuinely
 * different behavior, not just a different return type, so this does its own `Auth` calls rather
 * than wrapping [AdminAuthRepository] and trying to undo its sign-out.
 */
class SupabaseAuthService(
    private val client: SupabaseClient,
    private val scope: CoroutineScope,
) : AuthService {
    private val _session = MutableStateFlow<Session?>(null)
    override val session: StateFlow<Session?> = _session.asStateFlow()

    override suspend fun signIn(email: String, password: String): SignInResult {
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val newSession = Session(email, isAdmin = client.auth.currentUserOrNull().isAdmin())
            _session.value = newSession
            SignInResult.Success(newSession)
        } catch (e: AuthRestException) {
            SignInResult.InvalidCredentials(e.errorDescription)
        } catch (e: HttpRequestException) {
            SignInResult.NetworkError(e.message ?: "Could not reach the backend.")
        }
    }

    /**
     * `install(Auth)` (`supabase/SupabaseClientFactory.kt`) already persists the GoTrue
     * access/refresh token pair to this machine's local settings storage and reloads it the next
     * time the plugin initializes — [AuthConfigDefaults.autoLoadFromStorage]/`SettingsSessionManager`,
     * the SDK's own mechanism, not anything this class writes itself. [awaitInitialization] just
     * waits for that reload to finish before this reads [currentUserOrNull]; no password is ever
     * part of what's stored.
     */
    override suspend fun restoreSession(): Session? {
        client.auth.awaitInitialization()
        val user = client.auth.currentUserOrNull() ?: return null
        val email = user.email ?: return null
        val restored = Session(email, isAdmin = user.isAdmin())
        _session.value = restored
        return restored
    }

    private fun UserInfo?.isAdmin(): Boolean =
        this?.appMetadata?.get(ROLE_CLAIM)?.jsonPrimitive?.contentOrNull == ADMIN_ROLE

    override fun signOut() {
        _session.value = null
        // Fire-and-forget: AuthService.signOut() isn't suspend (EditorState calls it from plain
        // UI code), and a dropped connection here shouldn't block clearing the local session,
        // which is what actually controls this tool's UI state.
        scope.launch { runCatching { client.auth.signOut(SignOutScope.LOCAL) } }
    }
}

package com.pamaz.vi.tools.mapeditor.data.supabase.auth

import com.pamaz.vi.tools.mapeditor.model.AdminSession
import com.pamaz.vi.tools.mapeditor.model.AdminSignInResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** The exact `app_metadata` claim `backend/supabase/migrations/20261011120001_extensions_and_helpers.sql`'s `is_admin()` checks. */
private const val ROLE_CLAIM = "role"
private const val ADMIN_ROLE = "admin"

/**
 * PLT-01: real email/password sign-in against Supabase Auth, then a client-side check that the
 * signed-in user actually has the `app_metadata.role = "admin"` claim `is_admin()` reads — a
 * non-admin user can authenticate fine (GoTrue doesn't know about this app's roles), they just get
 * [AdminSignInResult.NotAnAdmin] here instead of a session the UI treats as usable. This is a
 * *courtesy* check for a clear error message, not the real enforcement: every write still goes
 * through RLS/`is_admin()` on the server regardless of what this method decides.
 */
class AdminAuthRepository(private val client: SupabaseClient) {
    suspend fun signIn(email: String, password: String): AdminSignInResult {
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val role =
                client.auth.currentUserOrNull()?.appMetadata?.get(ROLE_CLAIM)?.jsonPrimitive?.contentOrNull
            if (role == ADMIN_ROLE) {
                AdminSignInResult.Success(AdminSession(email))
            } else {
                client.auth.signOut(SignOutScope.LOCAL)
                AdminSignInResult.NotAnAdmin(email)
            }
        } catch (e: AuthRestException) {
            AdminSignInResult.InvalidCredentials(
                e.errorDescription
            )
        } catch (e: HttpRequestException) {
            AdminSignInResult.NetworkError(e.message ?: "could not reach the backend")
        }
    }

    suspend fun signOut() {
        client.auth.signOut(SignOutScope.LOCAL)
    }
}

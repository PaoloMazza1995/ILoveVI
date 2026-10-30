package com.pamaz.vi.tools.mapeditor.data.fake

import com.pamaz.vi.tools.mapeditor.data.AuthService
import com.pamaz.vi.tools.mapeditor.model.Session
import com.pamaz.vi.tools.mapeditor.model.SignInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * `admin@example.com` (any password) signs in as admin; any other well-formed email signs in but
 * `isAdmin = false` (EDT-02 — the non-admin screenshot's state); a blank email or password is
 * [SignInResult.InvalidCredentials]. [forceNonAdmin] is for `@Preview`s that need the non-admin
 * banner regardless of which email is typed.
 */
class FakeAuthService(private val forceNonAdmin: Boolean = false) : AuthService {
    private val _session = MutableStateFlow<Session?>(null)
    override val session: StateFlow<Session?> = _session.asStateFlow()

    override suspend fun signIn(email: String, password: String): SignInResult {
        if (email.isBlank() || password.isBlank()) {
            return SignInResult.InvalidCredentials("Email and password are required.")
        }
        val isAdmin = !forceNonAdmin && email.equals("admin@example.com", ignoreCase = true)
        val newSession = Session(email, isAdmin)
        _session.value = newSession
        return SignInResult.Success(newSession)
    }

    /** No real backend to persist a session against, so there's nothing to restore here — always `null`. */
    override suspend fun restoreSession(): Session? = null

    override fun signOut() {
        _session.value = null
    }
}

package com.pamaz.vi.tools.mapeditor.data

import com.pamaz.vi.tools.mapeditor.model.Session
import com.pamaz.vi.tools.mapeditor.model.SignInResult
import kotlinx.coroutines.flow.StateFlow

/**
 * Sign-in (EDT-01). The password itself is never held anywhere past the [signIn] call that uses
 * it, but the resulting session is allowed to survive a restart (see [restoreSession]) so the
 * tool doesn't ask for credentials again every launch.
 */
interface AuthService {
    val session: StateFlow<Session?>
    suspend fun signIn(email: String, password: String): SignInResult

    /**
     * Restores a previously established session with no new credentials, e.g. on app startup —
     * `null` if there is none (or it no longer checks out). Also updates [session] when a session
     * is found, same as a successful [signIn].
     */
    suspend fun restoreSession(): Session?
    fun signOut()
}

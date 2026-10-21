package com.pamaz.vi.tools.mapeditor.auth

/**
 * An established, confirmed-admin session (PLT-01). Lives only in memory for this run — closing
 * the tool forgets it, there is no "remember me"/refresh-across-launches, and the password itself
 * is never held anywhere past the sign-in call that uses it.
 */
data class AdminSession(val email: String)

/** The outcome of a sign-in attempt — a sealed result rather than throwing, so the UI can show a *specific* message for "wrong role" vs "wrong password" (PLT-01's "clear error when the user is not admin"). */
sealed interface AdminSignInResult {
    data class Success(val session: AdminSession) : AdminSignInResult
    data class InvalidCredentials(val message: String) : AdminSignInResult
    data class NotAnAdmin(val email: String) : AdminSignInResult
    data class NetworkError(val message: String) : AdminSignInResult
}

package com.pamaz.vi.tools.mapeditor.model

/**
 * An established session — [isAdmin] false means "signed in, but `app_metadata.role` isn't
 * `admin`," not "signed out": Map Editor Functional Analysis EDT-02 explicitly wants a non-admin
 * user to keep looking at the map and preparing a draft, only with Commit disabled. This is why
 * [Session] isn't the older, real-auth-only [AdminSession]/[AdminSignInResult] (which signs a
 * non-admin out locally and treats it as a sign-in failure, `data.supabase.auth.AdminAuthRepository`'s
 * own documented "courtesy check" behavior) — the two models describe genuinely different
 * behavior, not just a naming difference, so reusing one for the other would be wrong, not just
 * inconvenient.
 */
data class Session(val email: String, val isAdmin: Boolean)

/** The outcome of a sign-in attempt. [SignInResult.Success] covers both admin and non-admin accounts — see [Session]'s own note on why. */
sealed interface SignInResult {
    data class Success(val session: Session) : SignInResult
    data class InvalidCredentials(val message: String) : SignInResult
    data class NetworkError(val message: String) : SignInResult
}

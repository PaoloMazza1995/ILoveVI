package com.pamaz.vi.core.data.auth

import com.pamaz.vi.core.database.PlatformContext

data class GoogleIdTokenResult(val idToken: String, val rawNonce: String)

/**
 * Android-only real implementation: system Credential Manager + Google ID token, per
 * `docs/auth-notes.md` §1 (the Web client ID as `setServerClientId`, a random nonce SHA-256-hashed
 * for Google, the raw nonce passed to Supabase separately). iOS/JVM `actual`s always
 * [Result.failure] — `AccountServiceImpl` only ever constructs/calls this on Android; every other
 * platform signs in with Google via the generic web-OAuth path instead (same class, different
 * method).
 */
expect class GoogleIdTokenRequester(context: PlatformContext) {
    suspend fun requestIdToken(webClientId: String): Result<GoogleIdTokenResult>
}

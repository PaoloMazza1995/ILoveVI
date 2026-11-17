package com.pamaz.vi.core.data.auth

import com.pamaz.vi.core.database.PlatformContext

data class AppleIdTokenResult(val identityToken: String, val rawNonce: String)

/**
 * iOS-only real implementation: `AuthenticationServices` (`ASAuthorizationAppleIDProvider`), a
 * system framework with no CocoaPods/SPM dependency — written directly in Kotlin against it, this
 * repo's established convention for iOS platform `actual`s (see `docs/auth-notes.md` §1/§3).
 * Android's `actual` always [Result.failure]: Apple has no Android SDK, and `AuthProviderId.APPLE`
 * is never in `availableProviders` there, so `AccountServiceImpl` never actually calls this on
 * Android — the type still needs an `actual` to compile.
 */
expect class AppleIdTokenRequester(context: PlatformContext) {
    suspend fun requestIdToken(): Result<AppleIdTokenResult>
}

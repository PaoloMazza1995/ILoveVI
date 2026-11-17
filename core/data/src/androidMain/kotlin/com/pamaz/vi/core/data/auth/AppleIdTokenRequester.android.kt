package com.pamaz.vi.core.data.auth

import com.pamaz.vi.core.data.repository.AccountErrorReason
import com.pamaz.vi.core.database.PlatformContext

/** Apple has no Android SDK, and `AuthProviderId.APPLE` is never in `availableProviders` on Android — see this class's commonMain KDoc. */
actual class AppleIdTokenRequester actual constructor(context: PlatformContext) {
    actual suspend fun requestIdToken(): Result<AppleIdTokenResult> =
        Result.failure(NativeSignInException(AccountErrorReason.UNKNOWN))
}

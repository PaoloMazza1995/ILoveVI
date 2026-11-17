package com.pamaz.vi.core.data.auth

import com.pamaz.vi.core.data.repository.AccountErrorReason
import com.pamaz.vi.core.database.PlatformContext

/** Not a shipped sign-in surface — see this class's commonMain KDoc. */
actual class AppleIdTokenRequester actual constructor(context: PlatformContext) {
    actual suspend fun requestIdToken(): Result<AppleIdTokenResult> =
        Result.failure(NativeSignInException(AccountErrorReason.UNKNOWN))
}

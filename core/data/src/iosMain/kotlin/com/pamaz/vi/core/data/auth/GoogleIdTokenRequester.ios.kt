package com.pamaz.vi.core.data.auth

import com.pamaz.vi.core.data.repository.AccountErrorReason
import com.pamaz.vi.core.database.PlatformContext

/** iOS ships Google sign-in via the web-OAuth path (`AccountServiceImpl`), not this native requester — see this class's commonMain KDoc. */
actual class GoogleIdTokenRequester actual constructor(context: PlatformContext) {
    actual suspend fun requestIdToken(webClientId: String): Result<GoogleIdTokenResult> =
        Result.failure(NativeSignInException(AccountErrorReason.UNKNOWN))
}

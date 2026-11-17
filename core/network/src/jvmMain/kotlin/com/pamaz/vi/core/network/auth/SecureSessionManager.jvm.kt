package com.pamaz.vi.core.network.auth

import com.pamaz.vi.core.database.PlatformContext
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession

/** In-memory only — see this class's commonMain KDoc for why the JVM target doesn't need real secure storage. */
actual class SecureSessionManager actual constructor(context: PlatformContext) : SessionManager {
    private var cached: UserSession? = null

    actual override suspend fun saveSession(session: UserSession) {
        cached = session
    }

    actual override suspend fun loadSession(): UserSession? = cached

    actual override suspend fun deleteSession() {
        cached = null
    }
}

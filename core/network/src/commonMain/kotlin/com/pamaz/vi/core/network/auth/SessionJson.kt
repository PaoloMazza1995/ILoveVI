package com.pamaz.vi.core.network.auth

import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

/** Shared by every [SecureSessionManager] `actual` so the serialization shape is identical across platforms. */
internal val sessionJson = Json { ignoreUnknownKeys = true }

internal fun encodeSession(session: UserSession): String =
    sessionJson.encodeToString(UserSession.serializer(), session)

internal fun decodeSessionOrNull(raw: String?): UserSession? {
    if (raw.isNullOrBlank()) return null
    return try {
        sessionJson.decodeFromString(UserSession.serializer(), raw)
    } catch (e: Exception) {
        null
    }
}

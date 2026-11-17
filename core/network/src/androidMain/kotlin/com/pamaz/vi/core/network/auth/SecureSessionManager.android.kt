package com.pamaz.vi.core.network.auth

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pamaz.vi.core.database.PlatformContext
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREFS_FILE_NAME = "ilovevi_secure_session"
private const val SESSION_KEY = "supabase_session"

/** Android Keystore-backed via `EncryptedSharedPreferences` (AES-256-GCM, the master key itself held in the Keystore, never written to disk in the clear). */
actual class SecureSessionManager actual constructor(private val context: PlatformContext) :
    SessionManager {

    private val preferences by lazy {
        val masterKey = MasterKey.Builder(context.context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    actual override suspend fun saveSession(session: UserSession) = withContext(Dispatchers.IO) {
        preferences.edit().putString(SESSION_KEY, encodeSession(session)).apply()
    }

    actual override suspend fun loadSession(): UserSession? = withContext(Dispatchers.IO) {
        decodeSessionOrNull(preferences.getString(SESSION_KEY, null))
    }

    actual override suspend fun deleteSession() = withContext(Dispatchers.IO) {
        preferences.edit().remove(SESSION_KEY).apply()
    }
}

package com.pamaz.vi.core.network.auth

import com.pamaz.vi.core.database.PlatformContext
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession

/**
 * Backs [io.github.jan.supabase.auth.Auth]'s session persistence with real platform-secure
 * storage — `androidMain`'s `actual` uses `EncryptedSharedPreferences` (Android Keystore-backed,
 * `androidx.security:security-crypto`); `iosMain`'s uses the Keychain (`platform.Security`
 * cinterop, this module's established "write iOS platform code directly against system
 * frameworks" convention — see `core/network/README.md`); `jvmMain`'s is a plain in-memory
 * fallback (this app never ships a JVM build users sign into; the target exists for tooling/tests
 * only). This step's "session stored securely" requirement is why this exists at all instead of
 * [io.github.jan.supabase.auth.SettingsSessionManager], the plugin's own default — that one backs
 * onto plain `SharedPreferences`/`NSUserDefaults`, acceptable for most apps but not what this step
 * asks for.
 *
 * [context] is unused on iOS/JVM (see those `actual`s) — only Android's `EncryptedSharedPreferences`
 * needs a real `android.content.Context` to open the keystore-backed file.
 */
expect class SecureSessionManager(context: PlatformContext) : SessionManager {
    override suspend fun saveSession(session: UserSession)
    override suspend fun loadSession(): UserSession?
    override suspend fun deleteSession()
}

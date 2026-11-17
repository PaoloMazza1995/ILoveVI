package com.pamaz.vi.core.network.config

/**
 * Whether the Account section (Google/Apple sign-in) should be visible at all — read from the
 * `ACCOUNT_LOGIN_ENABLED` `local.properties`/environment key the same way [GoogleAuthConfig] reads
 * `GOOGLE_WEB_CLIENT_ID` (real environment variable first, falling back to the git-ignored root
 * `local.properties`; see `docs/auth-notes.md` §6). Unlike [GoogleAuthConfig.fromBuildConfig], this
 * never throws — it's a visibility toggle, not a "this flow is broken without it" guard, so a
 * missing key just means "off," the same default as before this flag existed.
 */
object AccountFeatureConfig {
    fun isLoginEnabled(): Boolean = ACCOUNT_LOGIN_ENABLED
}

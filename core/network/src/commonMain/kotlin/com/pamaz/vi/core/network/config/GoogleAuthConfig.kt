package com.pamaz.vi.core.network.config

/**
 * Same build-time-config shape as [SupabaseConfig] (see that class's KDoc), for the two Google
 * sign-in client IDs `GOOGLE_WEB_CLIENT_ID`/`GOOGLE_IOS_CLIENT_ID` — real environment variables
 * first, falling back to the git-ignored root `local.properties`; see `docs/auth-notes.md` §6 for
 * exactly which flow reads which value. A missing [webClientId] fails loudly via [require] rather
 * than silently shipping a broken sign-in button — [iosClientId] is allowed to stay blank (it's
 * read but not used by the shipped iOS flow, see §1/§6).
 */
data class GoogleAuthConfig(val webClientId: String, val iosClientId: String) {
    companion object {
        fun fromBuildConfig(): GoogleAuthConfig {
            require(GOOGLE_WEB_CLIENT_ID.isNotBlank()) {
                "GOOGLE_WEB_CLIENT_ID is not set — add it to the environment or the root local.properties."
            }
            return GoogleAuthConfig(
                webClientId = GOOGLE_WEB_CLIENT_ID,
                iosClientId = GOOGLE_IOS_CLIENT_ID
            )
        }

        /**
         * Lets `:core:data`'s `AccountService.availableProviders` hide the Google sign-in option
         * entirely on a machine without `GOOGLE_WEB_CLIENT_ID` set, instead of showing a button
         * that always fails [fromBuildConfig]'s [require] the moment it's tapped — `GOOGLE_WEB_CLIENT_ID`
         * itself is `internal` to this module, so this is the only way another module can check.
         */
        fun isConfigured(): Boolean = GOOGLE_WEB_CLIENT_ID.isNotBlank()
    }
}

package com.pamaz.vi.core.data

import com.pamaz.vi.core.network.config.AccountFeatureConfig

/**
 * Compile-time feature gates. [spotify] stays `false` until step 13A ships a real
 * `SpotifyConnection` implementation — `:feature:settings` only renders the Spotify card when
 * this is `true`. A plain `const val`, not a remote/runtime flag: nothing in this product needs a
 * flag to change without a new build.
 *
 * [accountLogin] is different on purpose: a `val`, not a `const val`, since it reads
 * [AccountFeatureConfig.isLoginEnabled] — a real `local.properties`/environment-driven build-time
 * flag (`ACCOUNT_LOGIN_ENABLED`, see `docs/auth-notes.md` §6), not a hardcoded one, so trying the
 * Google/Apple sign-in flows doesn't need a code change, just a `local.properties` line. Same
 * "`:feature:settings`'s screen checks it, not the `ViewModel`" gating [spotify] already
 * establishes — the Account section simply doesn't render when it's off.
 */
object FeatureFlags {
    const val spotify: Boolean = false
    val accountLogin: Boolean = AccountFeatureConfig.isLoginEnabled()
}

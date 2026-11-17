package com.pamaz.vi

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.mp.KoinPlatform
import platform.Foundation.NSURL

/**
 * Called from `iOSApp.swift`'s `.onOpenURL { url in ... }` whenever the
 * `com.pamaz.vi://login-callback` redirect (step 13B's web-OAuth paths) opens the app — forwards
 * it to supabase-kt's `Auth` plugin, which completes the pending sign-in and updates
 * [AccountService][com.pamaz.vi.core.data.repository.AccountService]'s state.
 *
 * A no-op if Koin hasn't started yet — the same cold-start edge case
 * `handleAndroidAuthDeepLink`'s KDoc explains, and for the same reason doesn't actually occur in
 * the real OAuth flow.
 */
fun handleIosAuthDeepLink(url: NSURL) {
    val koin = KoinPlatform.getKoinOrNull() ?: return
    koin.get<SupabaseClient>().handleDeeplinks(url)
}

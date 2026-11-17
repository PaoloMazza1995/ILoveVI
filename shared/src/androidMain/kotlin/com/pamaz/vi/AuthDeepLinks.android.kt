package com.pamaz.vi

import android.content.Intent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.mp.KoinPlatform

/**
 * Called from `androidApp`'s `MainActivity.onNewIntent` whenever the `com.pamaz.vi://login-callback`
 * redirect (step 13B's web-OAuth paths) delivers a new [Intent] to the already-running Activity
 * (`android:launchMode="singleTop"`, see the manifest) — forwards it to supabase-kt's `Auth`
 * plugin, which completes the pending sign-in and updates [AccountService][com.pamaz.vi.core.data.repository.AccountService]'s
 * state.
 *
 * A no-op if Koin hasn't started yet: this can only matter on a true cold start via the deep link
 * itself (the app wasn't running, `App()`'s first composition — where Koin starts — hasn't
 * happened yet), which doesn't actually occur in the real OAuth flow (the browser only ever
 * redirects back to an app that's still alive in the background, since the user left it to sign
 * in and is coming right back) — flagged here rather than engineered around, since engineering
 * around it would mean threading the cold-start `Intent` through `App()`'s own signature, which
 * step 8 deliberately keeps parameter-free across every platform.
 */
fun handleAndroidAuthDeepLink(intent: Intent) {
    val koin = KoinPlatform.getKoinOrNull() ?: return
    koin.get<SupabaseClient>().handleDeeplinks(intent)
}

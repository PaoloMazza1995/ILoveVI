package com.pamaz.vi.tools.mapeditor.supabase

/**
 * The project URL + **publishable/anon key** this tool talks to — read from the environment only,
 * never from a file in this repo, same convention `:tools:tilegen`'s `SupabaseAdminConfig` uses.
 * Unlike that config, this is deliberately *not* the service-role key: every write this tool makes
 * goes through RLS as whichever admin signed in (PLT-01), not around it — `backend/README.md`'s
 * `is_admin()` policy is what actually authorizes writes, not a key that bypasses it.
 */
data class SupabaseConfig(val url: String, val anonKey: String) {
    companion object {
        fun fromEnvironment(env: (String) -> String? = System::getenv): SupabaseConfig {
            val url = env("SUPABASE_URL") ?: error("SUPABASE_URL environment variable is not set")
            val anonKey = env("SUPABASE_ANON_KEY")
                ?: error("SUPABASE_ANON_KEY environment variable is not set")
            return SupabaseConfig(url.trimEnd('/'), anonKey)
        }
    }
}

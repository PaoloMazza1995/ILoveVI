package com.pamaz.vi.core.network.config

/**
 * The project URL + **publishable/anon key** this module talks to — never the service-role key.
 * The app ships with no sign-in (read-only public content), so every request this module makes
 * goes through `anon`-role RLS, exactly the same policies `backend/README.md`'s `SELECT`-for-
 * anyone rules already grant.
 *
 * [url]/[anonKey] are backed by [SUPABASE_URL]/[SUPABASE_ANON_KEY], two `const val`s generated at
 * *build* time by this module's `generateNetworkBuildConfig` Gradle task (see `build.gradle.kts`)
 * from the `SUPABASE_URL`/`SUPABASE_ANON_KEY` environment variables, falling back to the
 * git-ignored root `local.properties` — the same two keys `:tools:map-editor`/`:tools:tilegen`
 * already read for their own Supabase clients. Never hardcoded in source, never committed: a build
 * run with neither set produces blank constants that compile fine but make
 * [fromBuildConfig] throw immediately, so a missing config fails loudly instead of silently
 * pointing at nothing.
 */
data class SupabaseConfig(val url: String, val anonKey: String) {
    companion object {
        fun fromBuildConfig(): SupabaseConfig {
            require(SUPABASE_URL.isNotBlank()) {
                "SUPABASE_URL is not set — add it to the environment or the root local.properties."
            }
            require(SUPABASE_ANON_KEY.isNotBlank()) {
                "SUPABASE_ANON_KEY is not set — add it to the environment or the root local.properties."
            }
            return SupabaseConfig(url = SUPABASE_URL.trimEnd('/'), anonKey = SUPABASE_ANON_KEY)
        }
    }
}

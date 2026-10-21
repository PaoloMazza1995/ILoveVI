package com.pamaz.vi.tools.mapeditor.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * One [SupabaseClient] with [Auth] and [Postgrest] installed. Having both on the *same* client is
 * what makes every `postgrest` call after a successful sign-in automatically carry that session's
 * JWT — the mechanism `is_admin()` reads `app_metadata.role` from (see `backend/README.md`) — with
 * no manual header plumbing, unlike `:tools:tilegen`'s raw-Ktor service-role calls.
 */
object SupabaseClientFactory {
    fun create(config: SupabaseConfig): SupabaseClient = createSupabaseClient(
        supabaseUrl = config.url,
        supabaseKey = config.anonKey,
    ) {
        install(Auth)
        install(Postgrest)
    }
}

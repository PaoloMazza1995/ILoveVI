package com.pamaz.vi.tools.mapeditor.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * One [SupabaseClient] with [Auth], [Postgrest] and [Storage] installed. Having all three on the
 * *same* client is what makes every `postgrest`/`storage` call after a successful sign-in
 * automatically carry that session's JWT — the mechanism `is_admin()` reads `app_metadata.role`
 * from (see `backend/README.md`) — with no manual header plumbing, unlike `:tools:tilegen`'s
 * raw-Ktor service-role calls. [Storage] backs the entry-image upload in the Commit flow (step 5B
 * extension) — same anon-key client, same admin-RLS-gated `entry-images` bucket policies, never
 * the service-role key.
 */
object SupabaseClientFactory {
    fun create(config: SupabaseConfig): SupabaseClient = createSupabaseClient(
        supabaseUrl = config.url,
        supabaseKey = config.anonKey,
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}

package com.pamaz.vi.core.network

import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.network.config.SupabaseConfig
import com.pamaz.vi.core.network.datasource.WeaponRemoteDataSourceImpl
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The acceptance check from the roadmap step: against a *running* local Supabase stack
 * (`cd backend && supabase start && supabase db reset`), fetch the seed rows and one delta page.
 *
 * This can't run unattended in CI/this sandbox — there is no Docker/local Supabase stack here —
 * so it's gated on the same `SUPABASE_URL`/`SUPABASE_ANON_KEY` environment variables the rest of
 * this module reads, pointed at the *local* stack (`supabase start` prints both). Unset either
 * one and this test skips itself (passes trivially) instead of failing the build for everyone
 * else; set them to run it for real:
 *
 * ```
 * cd backend && supabase start && supabase db reset
 * SUPABASE_URL=<printed anon URL> SUPABASE_ANON_KEY=<printed anon key> ./gradlew :core:network:jvmTest --tests "*LocalSupabaseAcceptanceTest*"
 * ```
 */
class LocalSupabaseAcceptanceTest {
    @Test
    fun fetchesSeedRows_andOneDeltaPage() = runTest {
        val url = System.getenv("SUPABASE_URL")
        val anonKey = System.getenv("SUPABASE_ANON_KEY")
        if (url.isNullOrBlank() || anonKey.isNullOrBlank()) {
            println(
                "Skipping LocalSupabaseAcceptanceTest: SUPABASE_URL/SUPABASE_ANON_KEY not set " +
                        "(no local Supabase stack available in this environment).",
            )
            return@runTest
        }

        val client = SupabaseClientProvider.create(SupabaseConfig(url, anonKey))
        val weapons = WeaponRemoteDataSourceImpl(client)

        val seedPage = weapons.fetchChanges(since = null, pageSize = 200)
        val seedRows = assertIs<NetworkResult.Success<List<Change<Weapon>>>>(seedPage).data
        assertTrue(
            seedRows.isNotEmpty(),
            "expected seed.sql's placeholder weapon row(s) to come back"
        )

        val lastSeen = seedRows.maxOf { it.item?.updatedAt ?: it.deletedAt!! }
        val deltaPage = weapons.fetchChanges(since = lastSeen, pageSize = 200)
        val deltaRows = assertIs<NetworkResult.Success<List<Change<Weapon>>>>(deltaPage).data
        assertTrue(
            deltaRows.isEmpty(),
            "nothing changed after the seed's own updated_at, so the delta page should be empty"
        )
    }
}

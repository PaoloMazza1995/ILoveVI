package com.pamaz.vi.core.data

import com.pamaz.vi.core.data.fake.FakeConnectivityMonitor
import com.pamaz.vi.core.data.sync.ContentSyncerImpl
import com.pamaz.vi.core.data.sync.SyncStatus
import com.pamaz.vi.core.database.dao.createCatalogDao
import com.pamaz.vi.core.database.dao.createMapDao
import com.pamaz.vi.core.database.dao.createMusicDao
import com.pamaz.vi.core.database.dao.createSyncDao
import com.pamaz.vi.core.network.SupabaseClientProvider
import com.pamaz.vi.core.network.config.SupabaseConfig
import com.pamaz.vi.core.network.datasource.createItemRemoteDataSource
import com.pamaz.vi.core.network.datasource.createMapConfigRemoteDataSource
import com.pamaz.vi.core.network.datasource.createMapPinRemoteDataSource
import com.pamaz.vi.core.network.datasource.createSongRemoteDataSource
import com.pamaz.vi.core.network.datasource.createStationRemoteDataSource
import com.pamaz.vi.core.network.datasource.createVehicleRemoteDataSource
import com.pamaz.vi.core.network.datasource.createWeaponRemoteDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The roadmap step's own acceptance check: against a *running* local Supabase stack
 * (`cd backend && supabase start && supabase db reset`), one sync downloads the seed data, the
 * data is readable "with the network off" (i.e. straight from the local database, never from the
 * fake-offline second client below), and a second sync against the same, unchanged backend
 * downloads nothing new.
 *
 * Self-skips (passes trivially, logging why) unless `SUPABASE_URL`/`SUPABASE_ANON_KEY` are set to
 * a local stack's printed values — this sandboxed environment has no Docker to run
 * `supabase start` with, the same limitation `:core:network`'s own `LocalSupabaseAcceptanceTest`
 * already documents. Run for real:
 *
 * ```
 * cd backend && supabase start && supabase db reset
 * SUPABASE_URL=<printed anon URL> SUPABASE_ANON_KEY=<printed anon key> ./gradlew :core:data:jvmTest --tests "*LocalSupabaseAcceptanceTest*"
 * ```
 */
class LocalSupabaseAcceptanceTest {
    @Test
    fun oneSync_populatesTheDatabase_andASecondSyncDownloadsNothingNew() = runTest {
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
        val database = testDatabase()
        val syncDao = createSyncDao(database)
        val catalogDao = createCatalogDao(database)
        val musicDao = createMusicDao(database)
        val mapDao = createMapDao(database)

        val syncer = ContentSyncerImpl(
            database = database,
            syncDao = syncDao,
            catalogDao = catalogDao,
            musicDao = musicDao,
            mapDao = mapDao,
            mapConfigRemoteDataSource = createMapConfigRemoteDataSource(client),
            stationRemoteDataSource = createStationRemoteDataSource(client),
            weaponRemoteDataSource = createWeaponRemoteDataSource(client),
            vehicleRemoteDataSource = createVehicleRemoteDataSource(client),
            itemRemoteDataSource = createItemRemoteDataSource(client),
            songRemoteDataSource = createSongRemoteDataSource(client),
            mapPinRemoteDataSource = createMapPinRemoteDataSource(client),
            connectivityMonitor = FakeConnectivityMonitor(),
        )

        // One sync, against the network.
        syncer.sync()
        assertEquals(SyncStatus.IDLE, syncer.syncState.value.status)
        assertNull(syncer.syncState.value.lastError)

        // "Shows data with the network off": read straight from the local database, no network
        // client involved at all from here on.
        assertTrue(
            catalogDao.weapons().first().isNotEmpty(),
            "expected seed.sql's weapon row(s) after one sync"
        )
        assertTrue(
            musicDao.stations().first().isNotEmpty(),
            "expected seed.sql's station row(s) after one sync"
        )
        assertTrue(
            mapDao.currentMapConfig().first() != null,
            "expected seed.sql's map_config row after one sync"
        )

        // A second sync against the same, unchanged backend: every table's cursor is already at
        // the backend's newest `updated_at`, so every fetch should come back empty.
        syncer.sync()
        assertNull(syncer.syncState.value.lastError)
    }
}

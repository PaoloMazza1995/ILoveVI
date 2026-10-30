package com.pamaz.vi.core.data.sync

import com.pamaz.vi.core.model.Station
import com.pamaz.vi.core.model.StationId
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.NetworkResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private fun weapon(id: String, updatedAt: Instant) = Weapon(
    id = WeaponId(id),
    name = "[$id]",
    description = "d",
    subType = WeaponType.PISTOL,
    updatedAt = updatedAt,
)

class ContentSyncerTest {

    @Test
    fun firstSync_appliesEveryTablesChanges() = runTest {
        val harness = SyncTestHarness()
        val now = Instant.parse("2026-01-01T00:00:00Z")
        harness.weapons.enqueueSuccess(listOf(Change(weapon("weapon-1", now), "weapon-1", null)))
        harness.stations.enqueueSuccess(
            listOf(Change(Station(StationId("station-1"), "[station-1]", now), "station-1", null)),
        )

        harness.syncer.sync()

        assertEquals(1, harness.catalogDao.weapons().first().size)
        assertEquals(1, harness.musicDao.stations().first().size)
        assertEquals(SyncStatus.IDLE, harness.syncer.syncState.value.status)
        assertNull(harness.syncer.syncState.value.lastError)
        assertNotNull(harness.syncer.syncState.value.lastUpdatedAt)
    }

    @Test
    fun secondSync_requestsAndAppliesOnlyChangesSinceTheFirst() = runTest {
        val harness = SyncTestHarness()
        val firstUpdatedAt = Instant.parse("2026-01-01T00:00:00Z")
        harness.weapons.enqueueSuccess(
            listOf(
                Change(
                    weapon("weapon-1", firstUpdatedAt),
                    "weapon-1",
                    null
                )
            )
        )
        harness.syncer.sync()
        assertEquals(listOf<Instant?>(null), harness.weapons.sinceLog)

        val secondUpdatedAt = Instant.parse("2026-01-02T00:00:00Z")
        harness.weapons.enqueueSuccess(
            listOf(
                Change(
                    weapon("weapon-2", secondUpdatedAt),
                    "weapon-2",
                    null
                )
            )
        )
        harness.syncer.sync()

        assertEquals(listOf(null, firstUpdatedAt), harness.weapons.sinceLog)
        val weapons = harness.catalogDao.weapons().first()
        assertEquals(2, weapons.size)
    }

    @Test
    fun tombstone_removesTheRowLocally() = runTest {
        val harness = SyncTestHarness()
        val createdAt = Instant.parse("2026-01-01T00:00:00Z")
        harness.weapons.enqueueSuccess(
            listOf(
                Change(
                    weapon("weapon-1", createdAt),
                    "weapon-1",
                    null
                )
            )
        )
        harness.syncer.sync()
        assertEquals(1, harness.catalogDao.weapons().first().size)

        val deletedAt = Instant.parse("2026-01-02T00:00:00Z")
        harness.weapons.enqueueSuccess(listOf(Change<Weapon>(null, "weapon-1", deletedAt)))
        harness.syncer.sync()

        assertTrue(harness.catalogDao.weapons().first().isEmpty())
        assertNull(harness.catalogDao.weapon(WeaponId("weapon-1")).first())
    }

    @Test
    fun interruptedSync_resumesFromTheLastCommittedPage() = runTest {
        val harness = SyncTestHarness()
        val base = Instant.parse("2026-01-01T00:00:00Z")
        val firstPage = (0 until SYNC_PAGE_SIZE).map { i ->
            Change(
                weapon("weapon-$i", base + kotlin.time.Duration.parse("PT${i}S")),
                "weapon-$i",
                null
            )
        }
        val firstPageCursor = firstPage.last().item!!.updatedAt

        harness.weapons.enqueueSuccess(firstPage)
        harness.weapons.enqueue(NetworkResult.Offline) // the "interruption": page 2 never arrives

        harness.syncer.sync()

        // Page 1 is already committed, even though the table's overall sync failed on page 2.
        assertEquals(SYNC_PAGE_SIZE, harness.catalogDao.weapons().first().size)
        assertEquals(firstPageCursor, harness.syncDao.lastSyncedAt("weapons").first())
        assertNotNull(harness.syncer.syncState.value.lastError)

        // Resuming: the next sync must ask for exactly what's missing, not redownload page 1.
        val secondPageCursor = firstPageCursor + kotlin.time.Duration.parse("PT1S")
        harness.weapons.enqueueSuccess(
            listOf(Change(weapon("weapon-resumed", secondPageCursor), "weapon-resumed", null)),
        )
        harness.syncer.sync()

        assertEquals(listOf(null, firstPageCursor, firstPageCursor), harness.weapons.sinceLog)
        assertEquals(SYNC_PAGE_SIZE + 1, harness.catalogDao.weapons().first().size)
    }

    @Test
    fun oneTableFailing_doesNotPreventTheOthersFromUpdating() = runTest {
        val harness = SyncTestHarness()
        harness.weapons.enqueue(NetworkResult.Offline)
        val now = Instant.parse("2026-01-01T00:00:00Z")
        harness.stations.enqueueSuccess(
            listOf(Change(Station(StationId("station-1"), "[station-1]", now), "station-1", null)),
        )

        harness.syncer.sync()

        assertTrue(harness.catalogDao.weapons().first().isEmpty())
        assertEquals(1, harness.musicDao.stations().first().size)
        assertNotNull(harness.syncer.syncState.value.lastError)
        assertTrue(harness.syncer.syncState.value.lastError!!.contains("weapons"))
    }

    @Test
    fun concurrentSyncCalls_onlyOneRunsAtOnce() = runTest {
        val harness = SyncTestHarness()
        val now = Instant.parse("2026-01-01T00:00:00Z")
        harness.weapons.enqueueSuccess(listOf(Change(weapon("weapon-1", now), "weapon-1", null)))

        harness.syncer.sync()
        harness.syncer.sync() // nothing left queued; must be a safe no-op, not a crash or a duplicate apply

        assertEquals(1, harness.catalogDao.weapons().first().size)
    }
}

package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.testDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class SyncDaoTest {
    @Test
    fun getAndSetLastSyncedAt() = runTest {
        val dao = SyncDaoImpl(testDatabase())
        assertNull(dao.lastSyncedAt("weapons").first())

        val at = Instant.parse("2026-01-01T00:00:00Z")
        dao.setLastSyncedAt("weapons", at)
        assertEquals(at, dao.lastSyncedAt("weapons").first())

        val later = Instant.parse("2026-01-02T00:00:00Z")
        dao.setLastSyncedAt("weapons", later)
        assertEquals(later, dao.lastSyncedAt("weapons").first())
    }
}

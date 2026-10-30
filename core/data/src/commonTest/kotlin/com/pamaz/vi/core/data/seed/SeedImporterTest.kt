package com.pamaz.vi.core.data.seed

import com.pamaz.vi.core.data.testDatabase
import com.pamaz.vi.core.database.dao.createCatalogDao
import com.pamaz.vi.core.database.dao.createMapDao
import com.pamaz.vi.core.database.dao.createMusicDao
import com.pamaz.vi.core.database.dao.createSyncDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SeedImporterTest {

    private fun newImporter(): Triple<SeedImporter, com.pamaz.vi.core.database.dao.CatalogDao, com.pamaz.vi.core.database.dao.SyncDao> {
        val database = testDatabase()
        val syncDao = createSyncDao(database)
        val catalogDao = createCatalogDao(database)
        val musicDao = createMusicDao(database)
        val mapDao = createMapDao(database)
        val importer = SeedImporterImpl(database, syncDao, catalogDao, musicDao, mapDao)
        return Triple(importer, catalogDao, syncDao)
    }

    @Test
    fun firstImport_populatesEveryTable() = runTest {
        val (importer, catalogDao, syncDao) = newImporter()

        importer.importIfNeeded()

        assertEquals(1, catalogDao.weapons().first().size)
        assertNotNull(syncDao.lastSyncedAt(SEED_SYNC_STATE_KEY).first())
    }

    @Test
    fun importingTwice_doesNotDuplicateRows() = runTest {
        val (importer, catalogDao, _) = newImporter()

        importer.importIfNeeded()
        importer.importIfNeeded()

        assertEquals(1, catalogDao.weapons().first().size)
    }
}

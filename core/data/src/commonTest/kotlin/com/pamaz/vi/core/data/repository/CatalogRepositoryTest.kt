package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.data.testDatabase
import com.pamaz.vi.core.database.dao.createCatalogDao
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class CatalogRepositoryTest {

    @Test
    fun weaponsAndEntryLookup_readWhatWasWrittenToTheDatabase() = runTest {
        val catalogDao = createCatalogDao(testDatabase())
        val repository = CatalogRepositoryImpl(catalogDao)
        val weapon = Weapon(
            id = WeaponId("weapon-1"),
            name = "[Weapon 1]",
            description = "d",
            subType = WeaponType.PISTOL,
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        catalogDao.upsertWeapon(weapon)

        assertEquals(listOf(weapon), repository.weapons().first())
        assertEquals(weapon, repository.weapon(weapon.id).first())
        assertEquals(weapon, repository.entry(EntryCategory.WEAPON, "weapon-1").first())
        assertEquals(listOf(weapon), repository.searchWeapons("weapon 1").first())
    }
}

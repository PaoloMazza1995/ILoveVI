package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.testDatabase
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class CatalogDaoTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun weapon(id: String, name: String, zone: String? = null) = Weapon(
        id = WeaponId(id),
        name = name,
        description = "desc",
        subType = WeaponType.PISTOL,
        zone = zone,
        updatedAt = now,
    )

    @Test
    fun listByCategory() = runTest {
        val dao = CatalogDaoImpl(testDatabase())
        dao.upsertWeapon(weapon("w1", "Pistol"))
        dao.upsertWeapon(weapon("w2", "Rifle"))

        val weapons = dao.weapons().first()
        assertEquals(setOf("w1", "w2"), weapons.map { it.id.value }.toSet())
    }

    @Test
    fun getById() = runTest {
        val dao = CatalogDaoImpl(testDatabase())
        dao.upsertWeapon(weapon("w1", "Pistol"))

        assertEquals("Pistol", dao.weapon(WeaponId("w1")).first()?.name)
        assertNull(dao.weapon(WeaponId("missing")).first())
    }

    @Test
    fun entryLevelLookupDispatchesByCategory() = runTest {
        val dao = CatalogDaoImpl(testDatabase())
        dao.upsertWeapon(weapon("w1", "Pistol"))

        val entry = dao.entry(EntryCategory.WEAPON, "w1").first()
        assertEquals("Pistol", entry?.name)
        assertEquals(EntryCategory.WEAPON, entry?.category)
    }

    @Test
    fun searchIsCaseAndAccentInsensitive() = runTest {
        val dao = CatalogDaoImpl(testDatabase())
        dao.upsertWeapon(weapon("w1", "Épée Dorée", zone = "Café Central"))

        assertEquals(1, dao.searchWeapons("epee").first().size)
        assertEquals(1, dao.searchWeapons("EPEE").first().size)
        assertEquals(1, dao.searchWeapons("cafe").first().size)
        assertTrue(dao.searchWeapons("nomatch").first().isEmpty())
    }

    @Test
    fun upsertIsIdempotent() = runTest {
        val dao = CatalogDaoImpl(testDatabase())
        dao.upsertWeapon(weapon("w1", "Pistol"))
        dao.upsertWeapon(weapon("w1", "Pistol"))
        dao.upsertWeapon(weapon("w1", "Pistol Mk2"))

        val weapons = dao.weapons().first()
        assertEquals(1, weapons.size)
        assertEquals("Pistol Mk2", weapons.single().name)
    }

    @Test
    fun tombstoneExcludesFromQueries() = runTest {
        val dao = CatalogDaoImpl(testDatabase())
        dao.upsertWeapon(weapon("w1", "Pistol"))

        dao.tombstoneWeapon(WeaponId("w1"), deletedAt = now, updatedAt = now)

        assertTrue(dao.weapons().first().isEmpty())
        assertNull(dao.weapon(WeaponId("w1")).first())
    }
}

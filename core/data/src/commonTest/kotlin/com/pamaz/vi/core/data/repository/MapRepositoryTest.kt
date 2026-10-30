package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.data.testDatabase
import com.pamaz.vi.core.database.dao.createCatalogDao
import com.pamaz.vi.core.database.dao.createMapDao
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MapRepositoryTest {

    @Test
    fun currentMapConfigAndPins_readWhatWasWrittenToTheDatabase() = runTest {
        val database = testDatabase()
        val mapDao = createMapDao(database)
        val catalogDao = createCatalogDao(database)
        val repository = MapRepositoryImpl(mapDao)
        val now = kotlin.time.Instant.parse("2026-01-01T00:00:00Z")

        val weapon =
            Weapon(WeaponId("weapon-1"), "[Weapon 1]", "d", WeaponType.PISTOL, updatedAt = now)
        catalogDao.upsertWeapon(weapon)
        val config = MapConfig(
            1,
            "https://example.com/map.png",
            1024,
            1024,
            "https://example.com/{z}/{x}/{y}.png",
            0,
            3,
            now
        )
        mapDao.upsertMapConfig(config)
        val pin = MapPin(
            MapPinId("pin-1"),
            EntryRef.ToWeapon(weapon.id),
            NormalizedPoint(0.5f, 0.5f),
            mapVersion = 1,
            updatedAt = now
        )
        mapDao.upsertPin(pin)

        assertEquals(config, repository.currentMapConfig().first())
        val pins = repository.pins(setOf(EntryCategory.WEAPON)).first()
        assertEquals(1, pins.size)
        assertEquals(pin, pins.first().pin)
        assertEquals(weapon.name, pins.first().entryName)
        assertEquals(pin, repository.pinByEntryId(EntryCategory.WEAPON, "weapon-1").first())
    }
}

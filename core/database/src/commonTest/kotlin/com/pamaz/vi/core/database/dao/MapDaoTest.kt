package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.testDatabase
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.MapPinId
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Instant

class MapDaoTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun weapon(id: String, name: String, subType: WeaponType = WeaponType.PISTOL) = Weapon(
        id = WeaponId(id), name = name, description = "d", subType = subType, updatedAt = now,
    )

    private fun vehicle(id: String, name: String, subType: VehicleType = VehicleType.CAR) = Vehicle(
        id = VehicleId(id), name = name, description = "d", subType = subType, updatedAt = now,
    )

    private fun item(id: String, name: String, subType: ItemType = ItemType.AMMO) = Item(
        id = ItemId(id), name = name, description = "d", subType = subType, updatedAt = now,
    )

    private fun pin(id: String, entryRef: EntryRef, x: Float = 0.5f, y: Float = 0.5f) = MapPin(
        id = MapPinId(id),
        entryRef = entryRef,
        position = NormalizedPoint(x, y),
        mapVersion = 1,
        updatedAt = now,
    )

    @Test
    fun mapConfigRoundTrips() = runTest {
        val dao = MapDaoImpl(testDatabase())
        val config = MapConfig(
            mapVersion = 1,
            sourceImageUrl = "https://example.com/map.png",
            widthPx = 1000,
            heightPx = 2000,
            tileUrlTemplate = "https://example.com/{z}/{x}/{y}.png",
            minZoom = 0,
            maxZoom = 5,
            updatedAt = now,
        )
        dao.upsertMapConfig(config)

        assertEquals(config, dao.currentMapConfig().first())
    }

    @Test
    fun pinJoinReturnsEntryNameAndRightEntryRef() = runTest {
        val database = testDatabase()
        val catalogDao = CatalogDaoImpl(database)
        val mapDao = MapDaoImpl(database)

        catalogDao.upsertWeapon(weapon("w1", "Pistol"))
        mapDao.upsertPin(pin("p1", EntryRef.ToWeapon(WeaponId("w1"))))

        val pins = mapDao.pins(setOf(EntryCategory.WEAPON)).first()
        assertEquals(1, pins.size)
        assertEquals("Pistol", pins.single().entryName)
        assertEquals(EntryCategory.WEAPON, pins.single().pin.category)
        assertIs<EntryRef.ToWeapon>(pins.single().pin.entryRef)
        assertEquals(WeaponId("w1"), (pins.single().pin.entryRef as EntryRef.ToWeapon).id)
    }

    @Test
    fun pinsFilteredBySubTypes() = runTest {
        val database = testDatabase()
        val catalogDao = CatalogDaoImpl(database)
        val mapDao = MapDaoImpl(database)

        catalogDao.upsertWeapon(weapon("w1", "Pistol", WeaponType.PISTOL))
        catalogDao.upsertWeapon(weapon("w2", "Rifle", WeaponType.RIFLE))
        mapDao.upsertPin(pin("p1", EntryRef.ToWeapon(WeaponId("w1"))))
        mapDao.upsertPin(pin("p2", EntryRef.ToWeapon(WeaponId("w2"))))

        val pins = mapDao.pins(setOf(EntryCategory.WEAPON), subTypes = setOf("RIFLE")).first()
        assertEquals(1, pins.size)
        assertEquals("Rifle", pins.single().entryName)
    }

    @Test
    fun pinsAcrossMultipleCategories() = runTest {
        val database = testDatabase()
        val catalogDao = CatalogDaoImpl(database)
        val mapDao = MapDaoImpl(database)

        catalogDao.upsertWeapon(weapon("w1", "Pistol"))
        catalogDao.upsertVehicle(vehicle("v1", "Car"))
        catalogDao.upsertItem(item("i1", "Armor"))
        mapDao.upsertPin(pin("p1", EntryRef.ToWeapon(WeaponId("w1"))))
        mapDao.upsertPin(pin("p2", EntryRef.ToVehicle(VehicleId("v1"))))
        mapDao.upsertPin(pin("p3", EntryRef.ToItem(ItemId("i1"))))

        val pins =
            mapDao.pins(setOf(EntryCategory.WEAPON, EntryCategory.VEHICLE, EntryCategory.ITEM))
                .first()
        assertEquals(setOf("Pistol", "Car", "Armor"), pins.map { it.entryName }.toSet())
    }

    @Test
    fun pinByEntryId() = runTest {
        val database = testDatabase()
        val catalogDao = CatalogDaoImpl(database)
        val mapDao = MapDaoImpl(database)

        catalogDao.upsertWeapon(weapon("w1", "Pistol"))
        mapDao.upsertPin(pin("p1", EntryRef.ToWeapon(WeaponId("w1"))))

        val found = mapDao.pinByEntryId(EntryCategory.WEAPON, "w1").first()
        assertEquals(MapPinId("p1"), found?.id)
        assertNull(mapDao.pinByEntryId(EntryCategory.WEAPON, "missing").first())
    }

    @Test
    fun tombstonePinExcludesFromJoinAndLookup() = runTest {
        val database = testDatabase()
        val catalogDao = CatalogDaoImpl(database)
        val mapDao = MapDaoImpl(database)

        catalogDao.upsertWeapon(weapon("w1", "Pistol"))
        mapDao.upsertPin(pin("p1", EntryRef.ToWeapon(WeaponId("w1"))))
        mapDao.tombstonePin(MapPinId("p1"), deletedAt = now, updatedAt = now)

        assertEquals(0, mapDao.pins(setOf(EntryCategory.WEAPON)).first().size)
        assertNull(mapDao.pinByEntryId(EntryCategory.WEAPON, "w1").first())
    }
}

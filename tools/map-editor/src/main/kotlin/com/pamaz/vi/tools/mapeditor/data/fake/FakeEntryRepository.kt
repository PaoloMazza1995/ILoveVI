package com.pamaz.vi.tools.mapeditor.data.fake

import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.tools.mapeditor.data.EntryRepository
import com.pamaz.vi.tools.mapeditor.model.EntrySummary

/**
 * 8 fixed entries across the 3 categories — deterministic ids/names/zones so `@Preview`s and tests
 * see the exact same data every time (default [EditorState][com.pamaz.vi.tools.mapeditor.state.EditorState]
 * wiring, see `di/AppModule.kt`).
 */
class FakeEntryRepository : EntryRepository {
    companion object {
        val ALL: List<EntrySummary> = listOf(
            EntrySummary(
                EntryRef.ToWeapon(WeaponId("weapon-1")),
                "Falcon .45",
                "Vice Beach",
                WeaponType.PISTOL.name
            ),
            EntrySummary(
                EntryRef.ToWeapon(WeaponId("weapon-2")),
                "Street Sweeper",
                "Downtown",
                WeaponType.MACHINE_GUN.name
            ),
            EntrySummary(
                EntryRef.ToWeapon(WeaponId("weapon-3")),
                "Vice Blade",
                null,
                WeaponType.MELEE.name
            ),
            EntrySummary(
                EntryRef.ToVehicle(VehicleId("vehicle-1")),
                "Comet",
                "Vice Beach",
                VehicleType.CAR.name
            ),
            EntrySummary(
                EntryRef.ToVehicle(VehicleId("vehicle-2")),
                "Nightshade",
                "Downtown",
                VehicleType.MOTORCYCLE.name
            ),
            EntrySummary(
                EntryRef.ToVehicle(VehicleId("vehicle-3")),
                "Marlin",
                "Marina",
                VehicleType.BOAT.name
            ),
            EntrySummary(EntryRef.ToItem(ItemId("item-1")), "Spare Mag", null, ItemType.AMMO.name),
            EntrySummary(
                EntryRef.ToItem(ItemId("item-2")),
                "Riot Vest",
                "Downtown",
                ItemType.ARMOR.name
            ),
        )
    }

    override suspend fun loadAll(): List<EntrySummary> = ALL
}

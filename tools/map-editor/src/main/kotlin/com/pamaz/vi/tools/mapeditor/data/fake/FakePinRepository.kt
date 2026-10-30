package com.pamaz.vi.tools.mapeditor.data.fake

import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.tools.mapeditor.data.PinRepository
import com.pamaz.vi.tools.mapeditor.model.DbPin

/** 15 fixed pins (across the 8 [FakeEntryRepository] entries, some shared by several pins) — the read-only "already in the database" overlay (EDT-11). */
class FakePinRepository : PinRepository {
    companion object {
        val ALL: List<DbPin> = listOf(
            DbPin(
                "db-pin-1",
                EntryCategory.WEAPON,
                "weapon-1",
                NormalizedPoint(0.22f, 0.31f),
                "Vice Beach"
            ),
            DbPin(
                "db-pin-2",
                EntryCategory.WEAPON,
                "weapon-1",
                NormalizedPoint(0.27f, 0.36f),
                "Vice Beach"
            ),
            DbPin(
                "db-pin-3",
                EntryCategory.WEAPON,
                "weapon-1",
                NormalizedPoint(0.19f, 0.41f),
                "Vice Beach"
            ),
            DbPin(
                "db-pin-4",
                EntryCategory.WEAPON,
                "weapon-2",
                NormalizedPoint(0.58f, 0.22f),
                "Downtown"
            ),
            DbPin(
                "db-pin-5",
                EntryCategory.WEAPON,
                "weapon-2",
                NormalizedPoint(0.63f, 0.27f),
                "Downtown"
            ),
            DbPin(
                "db-pin-6",
                EntryCategory.WEAPON,
                "weapon-3",
                NormalizedPoint(0.45f, 0.55f),
                null
            ),
            DbPin(
                "db-pin-7",
                EntryCategory.VEHICLE,
                "vehicle-1",
                NormalizedPoint(0.24f, 0.28f),
                "Vice Beach"
            ),
            DbPin(
                "db-pin-8",
                EntryCategory.VEHICLE,
                "vehicle-1",
                NormalizedPoint(0.30f, 0.33f),
                "Vice Beach"
            ),
            DbPin(
                "db-pin-9",
                EntryCategory.VEHICLE,
                "vehicle-1",
                NormalizedPoint(0.21f, 0.37f),
                "Vice Beach"
            ),
            DbPin(
                "db-pin-10",
                EntryCategory.VEHICLE,
                "vehicle-2",
                NormalizedPoint(0.60f, 0.24f),
                "Downtown"
            ),
            DbPin(
                "db-pin-11",
                EntryCategory.VEHICLE,
                "vehicle-2",
                NormalizedPoint(0.65f, 0.30f),
                "Downtown"
            ),
            DbPin(
                "db-pin-12",
                EntryCategory.VEHICLE,
                "vehicle-3",
                NormalizedPoint(0.78f, 0.68f),
                "Marina"
            ),
            DbPin("db-pin-13", EntryCategory.ITEM, "item-1", NormalizedPoint(0.40f, 0.60f), null),
            DbPin("db-pin-14", EntryCategory.ITEM, "item-1", NormalizedPoint(0.43f, 0.64f), null),
            DbPin(
                "db-pin-15",
                EntryCategory.ITEM,
                "item-2",
                NormalizedPoint(0.61f, 0.20f),
                "Downtown"
            ),
        )
    }

    override suspend fun loadAll(): List<DbPin> = ALL
}

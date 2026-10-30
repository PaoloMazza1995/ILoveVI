package com.pamaz.vi.core.network.mapper

import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.dto.ItemDto
import com.pamaz.vi.core.network.dto.VehicleDto
import com.pamaz.vi.core.network.dto.WeaponDto
import kotlin.time.Instant

/** Maps a `weapons` row. An unrecognized [WeaponDto.subType] (e.g. a future constant this build doesn't know yet) makes the row unmappable — see [rowChange]. */
internal fun WeaponDto.toChange(): Change<Weapon> = rowChange(id, deletedAt) {
    val type = WeaponType.entries.firstOrNull { it.name == subType }
        ?: error("Unknown WeaponType \"$subType\" for weapon \"$id\".")
    Weapon(
        id = WeaponId(id),
        name = name,
        description = description,
        subType = type,
        zone = zone,
        imageUrl = imageUrl,
        price = price,
        ammoType = ammoType,
        attributes = attributes,
        updatedAt = Instant.parse(updatedAt),
    )
}

/** Maps a `vehicles` row. An unrecognized [VehicleDto.subType] makes the row unmappable — see [rowChange]. */
internal fun VehicleDto.toChange(): Change<Vehicle> = rowChange(id, deletedAt) {
    val type = VehicleType.entries.firstOrNull { it.name == subType }
        ?: error("Unknown VehicleType \"$subType\" for vehicle \"$id\".")
    Vehicle(
        id = VehicleId(id),
        name = name,
        description = description,
        subType = type,
        zone = zone,
        imageUrl = imageUrl,
        attributes = attributes,
        updatedAt = Instant.parse(updatedAt),
    )
}

/** Maps an `items` row. An unrecognized [ItemDto.subType] makes the row unmappable — see [rowChange]. */
internal fun ItemDto.toChange(): Change<Item> = rowChange(id, deletedAt) {
    val type = ItemType.entries.firstOrNull { it.name == subType }
        ?: error("Unknown ItemType \"$subType\" for item \"$id\".")
    Item(
        id = ItemId(id),
        name = name,
        description = description,
        subType = type,
        zone = zone,
        imageUrl = imageUrl,
        attributes = attributes,
        updatedAt = Instant.parse(updatedAt),
    )
}

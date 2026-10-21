package com.pamaz.vi.core.database.mapper

import com.pamaz.vi.core.model.Item
import com.pamaz.vi.core.model.ItemId
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.Vehicle
import com.pamaz.vi.core.model.VehicleId
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.Weapon
import com.pamaz.vi.core.model.WeaponId
import com.pamaz.vi.core.model.WeaponType
import kotlin.time.Instant

internal fun weaponRowToWeapon(
    id: String,
    name: String,
    description: String,
    subType: String,
    zone: String?,
    imageUrl: String?,
    attributes: String,
    price: Long?,
    ammoType: String?,
    searchKey: String,
    updatedAt: Long,
    deletedAt: Long?,
): Weapon = Weapon(
    id = WeaponId(id),
    name = name,
    description = description,
    subType = WeaponType.valueOf(subType),
    zone = zone,
    imageUrl = imageUrl,
    price = price?.toInt(),
    ammoType = ammoType,
    attributes = decodeAttributes(attributes),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

internal fun vehicleRowToVehicle(
    id: String,
    name: String,
    description: String,
    subType: String,
    zone: String?,
    imageUrl: String?,
    attributes: String,
    searchKey: String,
    updatedAt: Long,
    deletedAt: Long?,
): Vehicle = Vehicle(
    id = VehicleId(id),
    name = name,
    description = description,
    subType = VehicleType.valueOf(subType),
    zone = zone,
    imageUrl = imageUrl,
    attributes = decodeAttributes(attributes),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

internal fun itemRowToItem(
    id: String,
    name: String,
    description: String,
    subType: String,
    zone: String?,
    imageUrl: String?,
    attributes: String,
    searchKey: String,
    updatedAt: Long,
    deletedAt: Long?,
): Item = Item(
    id = ItemId(id),
    name = name,
    description = description,
    subType = ItemType.valueOf(subType),
    zone = zone,
    imageUrl = imageUrl,
    attributes = decodeAttributes(attributes),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

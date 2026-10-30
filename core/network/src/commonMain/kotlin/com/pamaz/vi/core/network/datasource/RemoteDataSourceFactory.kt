package com.pamaz.vi.core.network.datasource

import io.github.jan.supabase.SupabaseClient

/**
 * Plain public factories for each data source's `internal` `*Impl` class, for a caller that
 * already holds a [SupabaseClient] (e.g. `:core:data`'s own acceptance test, pointed at a local
 * Supabase stack rather than the build-time-configured production one `networkModule` wires up)
 * but can't construct `WeaponRemoteDataSourceImpl` etc. itself, since those stay `internal` to
 * this module. `networkModule` could use these too but doesn't need to; both forms produce the
 * exact same instance.
 */
fun createWeaponRemoteDataSource(client: SupabaseClient): WeaponRemoteDataSource =
    WeaponRemoteDataSourceImpl(client)

fun createVehicleRemoteDataSource(client: SupabaseClient): VehicleRemoteDataSource =
    VehicleRemoteDataSourceImpl(client)

fun createItemRemoteDataSource(client: SupabaseClient): ItemRemoteDataSource =
    ItemRemoteDataSourceImpl(client)

fun createStationRemoteDataSource(client: SupabaseClient): StationRemoteDataSource =
    StationRemoteDataSourceImpl(client)

fun createSongRemoteDataSource(client: SupabaseClient): SongRemoteDataSource =
    SongRemoteDataSourceImpl(client)

fun createMapConfigRemoteDataSource(client: SupabaseClient): MapConfigRemoteDataSource =
    MapConfigRemoteDataSourceImpl(client)

fun createMapPinRemoteDataSource(client: SupabaseClient): MapPinRemoteDataSource =
    MapPinRemoteDataSourceImpl(client)

fun createFeedSourceRemoteDataSource(client: SupabaseClient): FeedSourceRemoteDataSource =
    FeedSourceRemoteDataSourceImpl(client)

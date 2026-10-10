package com.pamaz.vi.core.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** All typed ids must serialize as a plain JSON string, not as a `{"value": ...}` object. */
class ValueClassSerializationTest {

    @Test
    fun weaponIdSerializesAsPlainString() =
        assertEquals("\"w1\"", Json.encodeToString(WeaponId("w1")))

    @Test
    fun vehicleIdSerializesAsPlainString() =
        assertEquals("\"v1\"", Json.encodeToString(VehicleId("v1")))

    @Test
    fun itemIdSerializesAsPlainString() = assertEquals("\"i1\"", Json.encodeToString(ItemId("i1")))

    @Test
    fun songIdSerializesAsPlainString() = assertEquals("\"s1\"", Json.encodeToString(SongId("s1")))

    @Test
    fun stationIdSerializesAsPlainString() =
        assertEquals("\"st1\"", Json.encodeToString(StationId("st1")))

    @Test
    fun mapPinIdSerializesAsPlainString() =
        assertEquals("\"p1\"", Json.encodeToString(MapPinId("p1")))

    @Test
    fun feedSourceIdSerializesAsPlainString() =
        assertEquals("\"f1\"", Json.encodeToString(FeedSourceId("f1")))

    @Test
    fun newsItemIdSerializesAsPlainString() =
        assertEquals("\"n1\"", Json.encodeToString(NewsItemId("n1")))
}

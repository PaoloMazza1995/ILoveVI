package com.pamaz.vi.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

/** JSON round trip for every model in this module, via kotlinx.serialization. */
class JsonRoundTripTest {
    private val json = Json
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    /** [kotlin.time.Instant] itself round trips through the kotlinx-serialization version in the catalog. */
    @Test
    fun instantRoundTrips() {
        val encoded = json.encodeToString(now)
        assertEquals("\"2026-01-01T00:00:00Z\"", encoded)
        assertEquals(now, json.decodeFromString<Instant>(encoded))
    }

    private inline fun <reified T> assertRoundTrips(value: T) {
        val encoded = json.encodeToString(value)
        assertEquals(value, json.decodeFromString<T>(encoded))
    }

    @Test
    fun weaponRoundTrips() {
        assertRoundTrips(
            Weapon(
                id = WeaponId("w1"),
                name = "Pistol",
                description = "A reliable handgun.",
                subType = WeaponType.PISTOL,
                zone = "Vinewood",
                imageUrl = "https://example.com/pistol.png",
                price = 500,
                ammoType = "9mm",
                attributes = mapOf("magazine" to "12"),
                updatedAt = now,
            ),
        )
    }

    @Test
    fun weapon_nullableFieldsAbsentFromJson_decodeToDefaults() {
        val jsonText = """
            {"id":"w1","name":"Pistol","description":"A reliable handgun.","subType":"PISTOL","updatedAt":"2026-01-01T00:00:00Z"}
        """.trimIndent()
        val decoded = json.decodeFromString<Weapon>(jsonText)
        assertNull(decoded.zone)
        assertNull(decoded.imageUrl)
        assertNull(decoded.price)
        assertNull(decoded.ammoType)
        assertEquals(emptyMap(), decoded.attributes)
    }

    @Test
    fun vehicleRoundTrips() {
        assertRoundTrips(
            Vehicle(
                id = VehicleId("v1"),
                name = "Sedan",
                description = "A city car.",
                subType = VehicleType.CAR,
                zone = "Downtown",
                attributes = mapOf("seats" to "4"),
                updatedAt = now,
            ),
        )
    }

    @Test
    fun vehicle_nullableFieldsAbsentFromJson_decodeToDefaults() {
        val jsonText = """
            {"id":"v1","name":"Sedan","description":"A city car.","subType":"CAR","updatedAt":"2026-01-01T00:00:00Z"}
        """.trimIndent()
        val decoded = json.decodeFromString<Vehicle>(jsonText)
        assertNull(decoded.zone)
        assertNull(decoded.imageUrl)
        assertEquals(emptyMap(), decoded.attributes)
    }

    @Test
    fun itemRoundTrips() {
        assertRoundTrips(
            Item(
                id = ItemId("i1"),
                name = "Medkit",
                description = "Restores health.",
                subType = ItemType.HEALTH,
                zone = "Hospital",
                imageUrl = "https://example.com/medkit.png",
                updatedAt = now,
            ),
        )
    }

    @Test
    fun itemRoundTripsWithNoOptionalFields() {
        assertRoundTrips(
            Item(
                id = ItemId("i2"),
                name = "Ammo crate",
                description = "A box of ammo.",
                subType = ItemType.AMMO,
                updatedAt = now,
            ),
        )
    }

    @Test
    fun stationRoundTrips() {
        assertRoundTrips(Station(id = StationId("s1"), name = "Flash FM", updatedAt = now))
    }

    @Test
    fun songRoundTrips() {
        assertRoundTrips(
            Song(
                id = SongId("song1"),
                title = "In the Wind",
                artist = "The Dead Boys",
                stationId = StationId("s1"),
                spotifyTrackId = "spotify123",
                updatedAt = now,
            ),
        )
    }

    @Test
    fun song_nullableSpotifyTrackIdAbsentFromJson_decodesToNull() {
        val jsonText = """
            {"id":"song1","title":"In the Wind","artist":"The Dead Boys","stationId":"s1","updatedAt":"2026-01-01T00:00:00Z"}
        """.trimIndent()
        val decoded = json.decodeFromString<Song>(jsonText)
        assertNull(decoded.spotifyTrackId)
    }

    @Test
    fun mapPinRoundTrips_weaponRef() {
        val pin = MapPin(
            id = MapPinId("pin1"),
            entryRef = EntryRef.ToWeapon(WeaponId("w1")),
            position = NormalizedPoint(0.5f, 0.5f),
            zone = "Vinewood",
            mapVersion = 3,
            updatedAt = now,
        )
        assertRoundTrips(pin)
        assertEquals(3, json.decodeFromString<MapPin>(json.encodeToString(pin)).mapVersion)
    }

    @Test
    fun mapPinRoundTrips_vehicleRef() {
        assertRoundTrips(
            MapPin(
                id = MapPinId("pin2"),
                entryRef = EntryRef.ToVehicle(VehicleId("v1")),
                position = NormalizedPoint(0.1f, 0.9f),
                mapVersion = 1,
                updatedAt = now,
            ),
        )
    }

    @Test
    fun mapPinRoundTrips_itemRef() {
        assertRoundTrips(
            MapPin(
                id = MapPinId("pin3"),
                entryRef = EntryRef.ToItem(ItemId("i1")),
                position = NormalizedPoint(0f, 1f),
                mapVersion = 1,
                updatedAt = now,
            ),
        )
    }

    /** [EntryRef]'s discriminator is a stable [kotlinx.serialization.SerialName], not the class's name/package. */
    private fun entryRefDiscriminator(ref: EntryRef): String =
        json.parseToJsonElement(json.encodeToString(ref)).jsonObject.getValue("type").jsonPrimitive.content

    @Test
    fun entryRefDiscriminator_weaponIsStable() {
        assertEquals("weapon", entryRefDiscriminator(EntryRef.ToWeapon(WeaponId("w1"))))
    }

    @Test
    fun entryRefDiscriminator_vehicleIsStable() {
        assertEquals("vehicle", entryRefDiscriminator(EntryRef.ToVehicle(VehicleId("v1"))))
    }

    @Test
    fun entryRefDiscriminator_itemIsStable() {
        assertEquals("item", entryRefDiscriminator(EntryRef.ToItem(ItemId("i1"))))
    }

    @Test
    fun mapConfigRoundTrips() {
        assertRoundTrips(
            MapConfig(
                mapVersion = 3,
                sourceImageUrl = "https://example.com/map-v3.png",
                widthPx = 4096,
                heightPx = 2048,
                tileUrlTemplate = "https://example.com/tiles/{z}/{x}/{y}.png",
                minZoom = 0,
                maxZoom = 5,
                updatedAt = now,
            ),
        )
    }

    @Test
    fun feedSourceRoundTrips() {
        assertRoundTrips(
            FeedSource(
                id = FeedSourceId("f1"),
                name = "IloveVI Community",
                url = "https://example.com/feed.xml",
                topic = NewsTopic.NEWS,
                enabled = false,
                updatedAt = now,
            ),
        )
    }

    @Test
    fun newsItemRoundTrips() {
        assertRoundTrips(
            NewsItem(
                id = NewsItemId("n1"),
                feedId = FeedSourceId("f1"),
                title = "Leonida map leak: new district spotted",
                link = "https://example.com/leak",
                publishedAt = now,
                excerpt = "A short teaser, never the full article.",
                imageUrl = "https://example.com/leak.jpg",
                topic = NewsTopic.NEWS,
                fetchedAt = now,
            ),
        )
    }

    @Test
    fun newsItem_nullableFieldsAbsentFromJson_decodeToNull() {
        val jsonText = """
            {"id":"n1","feedId":"f1","title":"Leonida map leak","link":"https://example.com/leak","publishedAt":"2026-01-01T00:00:00Z","topic":"MISSIONS","fetchedAt":"2026-01-01T00:00:00Z"}
        """.trimIndent()
        val decoded = json.decodeFromString<NewsItem>(jsonText)
        assertNull(decoded.excerpt)
        assertNull(decoded.imageUrl)
        assertEquals(NewsTopic.MISSIONS, decoded.topic)
    }
}

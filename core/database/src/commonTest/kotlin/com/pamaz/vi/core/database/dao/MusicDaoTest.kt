package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.testDatabase
import com.pamaz.vi.core.model.Song
import com.pamaz.vi.core.model.SongId
import com.pamaz.vi.core.model.Station
import com.pamaz.vi.core.model.StationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class MusicDaoTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun station(id: String, name: String) = Station(StationId(id), name, now)

    private fun song(id: String, title: String, stationId: String, artist: String = "Artist") =
        Song(
            id = SongId(id),
            title = title,
            artist = artist,
            stationId = StationId(stationId),
            updatedAt = now,
        )

    private suspend fun MusicDao.seedStationAndSongs() {
        upsertStation(station("s1", "Station One"))
        upsertStation(station("s2", "Station Two"))
        upsertSong(song("song1", "Alpha", "s1"))
        upsertSong(song("song2", "Beta", "s2"))
    }

    @Test
    fun songsJoinedWithStation() = runTest {
        val dao = MusicDaoImpl(testDatabase())
        dao.seedStationAndSongs()

        val songs = dao.songs().first()
        assertEquals(setOf("Alpha", "Beta"), songs.map { it.title }.toSet())
    }

    @Test
    fun filterByStation() = runTest {
        val dao = MusicDaoImpl(testDatabase())
        dao.seedStationAndSongs()

        val songs = dao.songsByStation(StationId("s1")).first()
        assertEquals(listOf("Alpha"), songs.map { it.title })
    }

    @Test
    fun searchByStationAndText() = runTest {
        val dao = MusicDaoImpl(testDatabase())
        dao.seedStationAndSongs()

        assertEquals(
            listOf("Alpha"),
            dao.searchSongs(StationId("s1"), null).first().map { it.title })
        assertEquals(listOf("Alpha"), dao.searchSongs(null, "alpha").first().map { it.title })
        assertTrue(dao.searchSongs(StationId("s2"), "alpha").first().isEmpty())
    }

    @Test
    fun compilationCrud() = runTest {
        val dao = MusicDaoImpl(testDatabase())
        dao.createCompilation("c1", "My Mix", now)

        assertEquals("My Mix", dao.compilation("c1").first()?.name)

        dao.renameCompilation("c1", "Renamed")
        assertEquals("Renamed", dao.compilation("c1").first()?.name)

        dao.setCompilationSpotifyPlaylistId("c1", "spotify:playlist:123")
        assertEquals("spotify:playlist:123", dao.compilation("c1").first()?.spotifyPlaylistId)

        dao.deleteCompilation("c1")
        assertNull(dao.compilation("c1").first())
    }

    @Test
    fun addRemoveAndReorderCompilationSongs() = runTest {
        val dao = MusicDaoImpl(testDatabase())
        dao.seedStationAndSongs()
        dao.createCompilation("c1", "Mix", now)

        dao.addSongToCompilation("c1", SongId("song1"))
        dao.addSongToCompilation("c1", SongId("song2"))

        val ordered = dao.songsForCompilation("c1").first()
        assertEquals(
            listOf("song1" to 0, "song2" to 1),
            ordered.map { it.song.id.value to it.position })

        dao.reorderCompilation("c1", listOf(SongId("song2"), SongId("song1")))
        val reordered = dao.songsForCompilation("c1").first()
        assertEquals(
            listOf("song2" to 0, "song1" to 1),
            reordered.map { it.song.id.value to it.position })

        dao.removeSongFromCompilation("c1", SongId("song1"))
        assertEquals(
            listOf("song2"),
            dao.songsForCompilation("c1").first().map { it.song.id.value })
    }

    @Test
    fun tombstoneSongAndStation() = runTest {
        val dao = MusicDaoImpl(testDatabase())
        dao.seedStationAndSongs()

        dao.tombstoneSong(SongId("song1"), deletedAt = now, updatedAt = now)
        assertEquals(listOf("Beta"), dao.songs().first().map { it.title })

        dao.tombstoneStation(StationId("s2"), deletedAt = now, updatedAt = now)
        assertTrue(dao.songs().first().isEmpty())
    }
}

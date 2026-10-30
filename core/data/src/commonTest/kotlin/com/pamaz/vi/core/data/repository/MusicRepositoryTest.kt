package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.data.testDatabase
import com.pamaz.vi.core.database.dao.MusicDao
import com.pamaz.vi.core.database.dao.createMusicDao
import com.pamaz.vi.core.model.Song
import com.pamaz.vi.core.model.SongId
import com.pamaz.vi.core.model.Station
import com.pamaz.vi.core.model.StationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MusicRepositoryTest {

    private fun newMusicDao(): MusicDao = createMusicDao(testDatabase())

    @Test
    fun compilationReorder_persists() = runTest {
        val musicDao = newMusicDao()
        val repository = MusicRepositoryImpl(musicDao)
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val station = Station(StationId("station-1"), "[station-1]", now)
        musicDao.upsertStation(station)
        val songs = (1..3).map { i ->
            Song(
                SongId("song-$i"),
                "[song-$i]",
                "[artist-$i]",
                station.id,
                updatedAt = now
            )
        }
        songs.forEach { musicDao.upsertSong(it) }

        val compilationId = repository.createCompilation("[My Compilation]")
        songs.forEach { repository.addSongToCompilation(compilationId, it.id) }

        repository.reorderCompilation(compilationId, listOf(songs[2].id, songs[0].id, songs[1].id))

        val ordered = repository.songsForCompilation(compilationId).first()
        assertEquals(listOf("song-3", "song-1", "song-2"), ordered.map { it.song.id.value })
        assertEquals(listOf(0, 1, 2), ordered.map { it.position })
    }

    @Test
    fun renameAndDeleteCompilation_work() = runTest {
        val repository = MusicRepositoryImpl(newMusicDao())
        val id = repository.createCompilation("[Original name]")

        repository.renameCompilation(id, "[Renamed]")
        assertEquals("[Renamed]", repository.compilation(id).first()?.name)

        repository.deleteCompilation(id)
        assertEquals(null, repository.compilation(id).first())
    }
}

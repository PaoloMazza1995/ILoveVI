package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.core.model.Song
import kotlin.time.Instant

/**
 * This module's own equivalents of `:core:database`'s small DAO-level view types
 * (`MapPinWithEntryName`/`Compilation`/`CompilationSongEntry`/`Preferences`) — plain data, not
 * reused directly from that module. An earlier version of this file *did* reuse them as-is,
 * reasoning they were "already a clean abstraction, not raw persistence detail"; that turned out
 * to be wrong in practice: `:core:database` is an `implementation` (not `api`) dependency of this
 * module, so none of its types are visible to a consumer that only depends on `:core:data` — a
 * feature module implementing [MapRepository]/[MusicRepository]/[SettingsRepository] (e.g. in a
 * test fake) would need a direct `:core:database` dependency just to spell the return type,
 * exactly the leak this module exists to prevent. Found and fixed while wiring `:feature:catalog`
 * (step 9), which hit it immediately on [MapPinWithEntryName].
 */
data class MapPinWithEntryName(val pin: MapPin, val entryName: String)

data class Compilation(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val spotifyPlaylistId: String?,
)

data class CompilationSongEntry(val song: Song, val position: Int)

data class Preferences(
    val showLabels: Boolean,
    val language: String,
    val selectedNewsTopic: String?,
    val lastContentUpdate: Instant?,
)

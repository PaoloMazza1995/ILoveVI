package com.pamaz.vi.core.network.mapper

import com.pamaz.vi.core.model.FeedSource
import com.pamaz.vi.core.model.FeedSourceId
import com.pamaz.vi.core.model.NewsTopic
import com.pamaz.vi.core.network.Change
import com.pamaz.vi.core.network.dto.FeedSourceDto
import kotlin.time.Instant

/** Maps a `feed_sources` row. An unrecognized [FeedSourceDto.topic] makes the row unmappable — see [rowChange]. */
internal fun FeedSourceDto.toChange(): Change<FeedSource> = rowChange(id, deletedAt) {
    val topic = NewsTopic.entries.firstOrNull { it.name == this.topic }
        ?: error("Unknown NewsTopic \"${this.topic}\" for feed source \"$id\".")
    FeedSource(
        id = FeedSourceId(id),
        name = name,
        url = url,
        topic = topic,
        enabled = enabled,
        updatedAt = Instant.parse(updatedAt),
    )
}

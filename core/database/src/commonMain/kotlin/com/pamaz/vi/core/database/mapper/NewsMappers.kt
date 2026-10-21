package com.pamaz.vi.core.database.mapper

import com.pamaz.vi.core.model.FeedSource
import com.pamaz.vi.core.model.FeedSourceId
import com.pamaz.vi.core.model.NewsItem
import com.pamaz.vi.core.model.NewsItemId
import com.pamaz.vi.core.model.NewsTopic
import kotlin.time.Instant

internal fun feedSourceRowToFeedSource(
    id: String,
    name: String,
    url: String,
    topic: String,
    enabled: Long,
    updatedAt: Long,
    deletedAt: Long?,
): FeedSource = FeedSource(
    id = FeedSourceId(id),
    name = name,
    url = url,
    topic = NewsTopic.valueOf(topic),
    enabled = enabled != 0L,
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

internal fun newsItemRowToNewsItem(
    id: String,
    feedId: String,
    title: String,
    link: String,
    publishedAt: Long,
    excerpt: String?,
    imageUrl: String?,
    topic: String,
    fetchedAt: Long,
): NewsItem = NewsItem(
    id = NewsItemId(id),
    feedId = FeedSourceId(feedId),
    title = title,
    link = link,
    publishedAt = Instant.fromEpochMilliseconds(publishedAt),
    excerpt = excerpt,
    imageUrl = imageUrl,
    topic = NewsTopic.valueOf(topic),
    fetchedAt = Instant.fromEpochMilliseconds(fetchedAt),
)

package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.asListFlow
import com.pamaz.vi.core.database.mapper.feedSourceRowToFeedSource
import com.pamaz.vi.core.database.mapper.newsItemRowToNewsItem
import com.pamaz.vi.core.model.FeedSource
import com.pamaz.vi.core.model.FeedSourceId
import com.pamaz.vi.core.model.NewsItem
import com.pamaz.vi.core.model.NewsTopic
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface NewsDao {
    fun feedSources(): Flow<List<FeedSource>>
    fun enabledFeedSources(topic: NewsTopic): Flow<List<FeedSource>>

    /** The latest [limit] items for [topic], newest first. */
    fun newsItems(topic: NewsTopic, limit: Int = 200): Flow<List<NewsItem>>

    suspend fun upsertFeedSource(feedSource: FeedSource)
    suspend fun tombstoneFeedSource(id: FeedSourceId, deletedAt: Instant, updatedAt: Instant)

    /** [NewsItem] has no backend table (device-local cache), so it's hard-deleted via [trimToLatest] rather than tombstoned. */
    suspend fun upsertNewsItem(newsItem: NewsItem)

    /** Deletes every row for [topic] past the newest [keep] (by publishedAt), e.g. after a fetch. */
    suspend fun trimToLatest(topic: NewsTopic, keep: Int = 200)
}

internal class NewsDaoImpl(database: IloveViDatabase) : NewsDao {
    private val feedSourcesQueries = database.feedSourcesQueries
    private val newsItemsQueries = database.newsItemsQueries

    override fun feedSources(): Flow<List<FeedSource>> =
        feedSourcesQueries.selectAll(::feedSourceRowToFeedSource).asListFlow()

    override fun enabledFeedSources(topic: NewsTopic): Flow<List<FeedSource>> =
        feedSourcesQueries.selectEnabledByTopic(topic.name, ::feedSourceRowToFeedSource)
            .asListFlow()

    override fun newsItems(topic: NewsTopic, limit: Int): Flow<List<NewsItem>> =
        newsItemsQueries.selectByTopic(topic.name, limit.toLong(), ::newsItemRowToNewsItem)
            .asListFlow()

    override suspend fun upsertFeedSource(feedSource: FeedSource) {
        feedSourcesQueries.upsert(
            id = feedSource.id.value,
            name = feedSource.name,
            url = feedSource.url,
            topic = feedSource.topic.name,
            enabled = if (feedSource.enabled) 1L else 0L,
            updatedAt = feedSource.updatedAt.toEpochMilliseconds(),
            deletedAt = null,
        )
    }

    override suspend fun tombstoneFeedSource(
        id: FeedSourceId,
        deletedAt: Instant,
        updatedAt: Instant
    ) {
        feedSourcesQueries.tombstone(
            deletedAt.toEpochMilliseconds(),
            updatedAt.toEpochMilliseconds(),
            id.value
        )
    }

    override suspend fun upsertNewsItem(newsItem: NewsItem) {
        newsItemsQueries.upsert(
            id = newsItem.id.value,
            feedId = newsItem.feedId.value,
            title = newsItem.title,
            link = newsItem.link,
            publishedAt = newsItem.publishedAt.toEpochMilliseconds(),
            excerpt = newsItem.excerpt,
            imageUrl = newsItem.imageUrl,
            topic = newsItem.topic.name,
            fetchedAt = newsItem.fetchedAt.toEpochMilliseconds(),
        )
    }

    override suspend fun trimToLatest(topic: NewsTopic, keep: Int) {
        newsItemsQueries.deleteOlderThanLatest(topic.name, keep.toLong())
    }
}

package com.pamaz.vi.core.database.dao

import com.pamaz.vi.core.database.IloveViDatabase
import com.pamaz.vi.core.database.asListFlow
import com.pamaz.vi.core.database.mapper.feedSourceRowToFeedSource
import com.pamaz.vi.core.database.mapper.newsItemRowToNewsItem
import com.pamaz.vi.core.model.FeedSource
import com.pamaz.vi.core.model.FeedSourceId
import com.pamaz.vi.core.model.NewsItem
import com.pamaz.vi.core.model.NewsItemId
import com.pamaz.vi.core.model.NewsTopic
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

private const val DEFAULT_NEWS_ITEM_LIMIT = 300

interface NewsDao {
    fun feedSources(): Flow<List<FeedSource>>
    fun enabledFeedSources(topic: NewsTopic): Flow<List<FeedSource>>

    /** The latest [limit] live items, newest first, merged across every [NewsTopic] — the News screen filters client-side. */
    fun newsItems(limit: Int = DEFAULT_NEWS_ITEM_LIMIT): Flow<List<NewsItem>>

    suspend fun upsertFeedSource(feedSource: FeedSource)
    suspend fun tombstoneFeedSource(id: FeedSourceId, deletedAt: Instant, updatedAt: Instant)

    suspend fun upsertNewsItem(newsItem: NewsItem)
    suspend fun tombstoneNewsItem(id: NewsItemId, deletedAt: Instant, updatedAt: Instant)

    /** Tombstones every live [topic] item past the newest [keep] (by `publishedAt` desc) — see `NewsItems.sq`'s `selectStaleIds`. */
    suspend fun trimNewsItems(topic: NewsTopic, keep: Int, deletedAt: Instant, updatedAt: Instant)
}

internal class NewsDaoImpl(database: IloveViDatabase) : NewsDao {
    private val feedSourcesQueries = database.feedSourcesQueries
    private val newsItemsQueries = database.newsItemsQueries

    override fun feedSources(): Flow<List<FeedSource>> =
        feedSourcesQueries.selectAll(::feedSourceRowToFeedSource).asListFlow()

    override fun enabledFeedSources(topic: NewsTopic): Flow<List<FeedSource>> =
        feedSourcesQueries.selectEnabledByTopic(topic.name, ::feedSourceRowToFeedSource)
            .asListFlow()

    override fun newsItems(limit: Int): Flow<List<NewsItem>> =
        newsItemsQueries.selectAll(limit.toLong(), ::newsItemRowToNewsItem)
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
            updatedAt = newsItem.updatedAt.toEpochMilliseconds(),
            deletedAt = null,
        )
    }

    override suspend fun tombstoneNewsItem(id: NewsItemId, deletedAt: Instant, updatedAt: Instant) {
        newsItemsQueries.tombstone(
            deletedAt.toEpochMilliseconds(),
            updatedAt.toEpochMilliseconds(),
            id.value
        )
    }

    override suspend fun trimNewsItems(
        topic: NewsTopic,
        keep: Int,
        deletedAt: Instant,
        updatedAt: Instant
    ) {
        val staleIds = newsItemsQueries.selectStaleIds(topic.name, keep.toLong()).executeAsList()
        staleIds.forEach { id ->
            newsItemsQueries.tombstone(
                deletedAt.toEpochMilliseconds(),
                updatedAt.toEpochMilliseconds(),
                id
            )
        }
    }
}

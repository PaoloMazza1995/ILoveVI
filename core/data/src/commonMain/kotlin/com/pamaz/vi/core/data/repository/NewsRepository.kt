package com.pamaz.vi.core.data.repository

import com.pamaz.vi.core.database.dao.NewsDao
import com.pamaz.vi.core.model.FeedSource
import com.pamaz.vi.core.model.NewsItem
import kotlinx.coroutines.flow.Flow

/** News/Missions content (read-only, synced), wrapping `:core:database`'s [NewsDao] — see `MusicRepository`'s KDoc for why a screen depends on this rather than the DAO directly. */
interface NewsRepository {
    fun feedSources(): Flow<List<FeedSource>>

    /** The latest [limit] live items, newest first, merged across topics — see [NewsDao.newsItems]. */
    fun newsItems(limit: Int = 300): Flow<List<NewsItem>>
}

internal class NewsRepositoryImpl(private val newsDao: NewsDao) : NewsRepository {
    override fun feedSources(): Flow<List<FeedSource>> = newsDao.feedSources()
    override fun newsItems(limit: Int): Flow<List<NewsItem>> = newsDao.newsItems(limit)
}

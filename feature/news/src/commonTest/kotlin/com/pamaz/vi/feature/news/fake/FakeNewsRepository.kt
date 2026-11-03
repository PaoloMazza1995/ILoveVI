package com.pamaz.vi.feature.news.fake

import com.pamaz.vi.core.data.repository.NewsRepository
import com.pamaz.vi.core.model.FeedSource
import com.pamaz.vi.core.model.NewsItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** A tiny in-memory stand-in for the real `:core:database`-backed [NewsRepository], for [com.pamaz.vi.feature.news.news.NewsViewModel] tests. */
internal class FakeNewsRepository : NewsRepository {
    private val feedSourcesFlow = MutableStateFlow<List<FeedSource>>(emptyList())
    private val newsItemsFlow = MutableStateFlow<List<NewsItem>>(emptyList())

    fun setFeedSources(feedSources: List<FeedSource>) {
        feedSourcesFlow.value = feedSources
    }

    fun setNewsItems(items: List<NewsItem>) {
        newsItemsFlow.value = items
    }

    override fun feedSources(): Flow<List<FeedSource>> = feedSourcesFlow
    override fun newsItems(limit: Int): Flow<List<NewsItem>> = newsItemsFlow
}

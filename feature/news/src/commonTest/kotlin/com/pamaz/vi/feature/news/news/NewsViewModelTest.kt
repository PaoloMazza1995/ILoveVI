package com.pamaz.vi.feature.news.news

import com.pamaz.vi.core.data.sync.SyncState
import com.pamaz.vi.core.data.sync.SyncStatus
import com.pamaz.vi.core.model.FeedSource
import com.pamaz.vi.core.model.FeedSourceId
import com.pamaz.vi.core.model.NewsItem
import com.pamaz.vi.core.model.NewsItemId
import com.pamaz.vi.core.model.NewsTopic
import com.pamaz.vi.design.IloveViNewsTopic
import com.pamaz.vi.design.screens.IloveViNewsFilter
import com.pamaz.vi.feature.news.fake.FakeContentSyncer
import com.pamaz.vi.feature.news.fake.FakeNewsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

private val NOW = Instant.parse("2026-01-01T00:00:00Z")

private fun newsItem(id: String, feedId: String, topic: NewsTopic = NewsTopic.NEWS) = NewsItem(
    id = NewsItemId(id),
    feedId = FeedSourceId(feedId),
    title = "Title $id",
    link = "https://example.com/$id",
    publishedAt = NOW,
    topic = topic,
    fetchedAt = NOW,
    updatedAt = NOW,
)

private fun feedSource(id: String, name: String) = FeedSource(
    id = FeedSourceId(id),
    name = name,
    url = "https://example.com/$id.xml",
    topic = NewsTopic.NEWS,
    updatedAt = NOW,
)

/** See `CatalogViewModelTest`'s KDoc for why `Dispatchers.Main` must be a test dispatcher before the view model is constructed. */
@OptIn(ExperimentalCoroutinesApi::class)
class NewsViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUpMainDispatcher() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun articles_resolveSourceNameFromFeedId() = runTest(mainDispatcher) {
        val repository = FakeNewsRepository()
        repository.setFeedSources(listOf(feedSource("feed-1", "IloveVI Community")))
        repository.setNewsItems(listOf(newsItem("n1", "feed-1")))
        val viewModel = NewsViewModel(repository, FakeContentSyncer())
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        val article = viewModel.uiState.value.articles.single()
        assertEquals("IloveVI Community", article.source)
        assertEquals(IloveViNewsTopic.News, article.topic)

        collector.cancel()
    }

    @Test
    fun missingFeedName_fallsBackToFeedId() = runTest(mainDispatcher) {
        val repository = FakeNewsRepository()
        repository.setNewsItems(listOf(newsItem("n1", "unknown-feed")))
        val viewModel = NewsViewModel(repository, FakeContentSyncer())
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("unknown-feed", viewModel.uiState.value.articles.single().source)

        collector.cancel()
    }

    @Test
    fun syncError_marksStateOffline() = runTest(mainDispatcher) {
        val syncer = FakeContentSyncer(
            SyncState(
                status = SyncStatus.IDLE,
                lastUpdatedAt = NOW,
                lastError = "news_items: offline"
            )
        )
        val viewModel = NewsViewModel(FakeNewsRepository(), syncer)
        val collector = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isOffline)

        collector.cancel()
    }

    @Test
    fun onRefresh_triggersContentSyncerSync() = runTest(mainDispatcher) {
        val syncer = FakeContentSyncer()
        val viewModel = NewsViewModel(FakeNewsRepository(), syncer)

        viewModel.onRefresh()
        advanceUntilIdle()

        assertEquals(1, syncer.syncCallCount)
    }

    @Test
    fun onFilterSelect_updatesSelectedFilter() {
        val viewModel = NewsViewModel(FakeNewsRepository(), FakeContentSyncer())
        assertEquals(IloveViNewsFilter.All, viewModel.selectedFilter.value)

        viewModel.onFilterSelect(IloveViNewsFilter.Missions)
        assertEquals(IloveViNewsFilter.Missions, viewModel.selectedFilter.value)
    }

    @Test
    fun beforeAnyCollection_stateIsLoading() {
        val viewModel = NewsViewModel(FakeNewsRepository(), FakeContentSyncer())
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.articles.firstOrNull())
    }
}

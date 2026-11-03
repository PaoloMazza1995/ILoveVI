package com.pamaz.vi.feature.news.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pamaz.vi.core.data.repository.NewsRepository
import com.pamaz.vi.core.data.sync.ContentSyncer
import com.pamaz.vi.core.data.sync.SyncStatus
import com.pamaz.vi.core.model.NewsTopic
import com.pamaz.vi.design.IloveViNewsTopic
import com.pamaz.vi.design.screens.IloveViNewsArticle
import com.pamaz.vi.design.screens.IloveViNewsFilter
import com.pamaz.vi.design.screens.IloveViNewsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Backs the News tab (step 12C). [NewsRepository] is itself backed by `:core:database`, kept in
 * sync with the backend by [ContentSyncer] (step 12B) — this class never talks to the network
 * directly. `state.articles` always holds the *full*, unfiltered list; [IloveViNewsScreen] (the
 * design-system composable this feeds) applies [IloveViNewsFilter] client-side, exactly like its
 * own preview data does.
 */
class NewsViewModel(
    private val newsRepository: NewsRepository,
    private val contentSyncer: ContentSyncer,
) : ViewModel() {

    private val selectedFilterState = MutableStateFlow(IloveViNewsFilter.All)
    val selectedFilter: StateFlow<IloveViNewsFilter> = selectedFilterState.asStateFlow()

    val uiState: StateFlow<IloveViNewsUiState> = combine(
        newsRepository.newsItems(),
        newsRepository.feedSources(),
        contentSyncer.syncState,
    ) { items, feedSources, syncState ->
        val feedNameById = feedSources.associate { it.id to it.name }
        val articles = items.map { item ->
            IloveViNewsArticle(
                id = item.id.value,
                title = item.title,
                source = feedNameById[item.feedId] ?: item.feedId.value,
                publishedAtText = relativeTimeText(item.publishedAt),
                topic = item.topic.toDesignTopic(),
                imageUrl = item.imageUrl,
                url = item.link,
            )
        }
        IloveViNewsUiState(
            articles = articles,
            isLoading = syncState.status == SyncStatus.RUNNING && articles.isEmpty(),
            isOffline = syncState.lastError != null,
            updatedAtText = syncState.lastUpdatedAt?.let(::relativeTimeText) ?: "never",
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = IloveViNewsUiState(isLoading = true),
    )

    fun onFilterSelect(filter: IloveViNewsFilter) {
        selectedFilterState.value = filter
    }

    fun onRefresh() {
        viewModelScope.launch { contentSyncer.sync() }
    }

    fun onRetry() = onRefresh()
}

private fun NewsTopic.toDesignTopic(): IloveViNewsTopic = when (this) {
    NewsTopic.NEWS -> IloveViNewsTopic.News
    NewsTopic.MISSIONS -> IloveViNewsTopic.Missions
}

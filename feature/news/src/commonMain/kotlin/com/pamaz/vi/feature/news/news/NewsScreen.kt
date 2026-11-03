package com.pamaz.vi.feature.news.news

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pamaz.vi.design.screens.IloveViNewsScreen
import org.koin.compose.viewmodel.koinViewModel

/** The News tab (step 12C) — wires [NewsViewModel] into `:core:design`'s [IloveViNewsScreen], the only place this module owns a platform launch API (opening an article in the system browser). */
@Composable
fun NewsScreen(viewModel: NewsViewModel = koinViewModel(), modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    IloveViNewsScreen(
        state = state,
        selectedFilter = selectedFilter,
        onFilterSelect = viewModel::onFilterSelect,
        onRefresh = viewModel::onRefresh,
        onRetry = viewModel::onRetry,
        onArticleClick = { article -> uriHandler.openUri(article.url) },
        modifier = modifier,
    )
}

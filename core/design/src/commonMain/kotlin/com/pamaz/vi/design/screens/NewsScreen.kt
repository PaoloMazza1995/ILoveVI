package com.pamaz.vi.design.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViNewsTopic
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.design.components.IconRefresh
import com.pamaz.vi.design.components.IloveViButton
import com.pamaz.vi.design.components.IloveViButtonVariant
import com.pamaz.vi.design.components.IloveViIconButton
import com.pamaz.vi.design.components.IloveViNewsRow
import com.pamaz.vi.design.components.IloveViNewsRowSkeleton

/** One RSS item, as rendered by [IloveViNewsRow]. `url` is the article's original link. */
data class IloveViNewsArticle(
    val id: String,
    val title: String,
    val source: String,
    val publishedAtText: String,
    val topic: IloveViNewsTopic,
    val imageUrl: String?,
    val url: String,
)

/** Which topic the list is filtered to; `All` is not an [IloveViNewsTopic] (no article carries it). */
enum class IloveViNewsFilter { All, News, Missions }

/**
 * NewsScreen's state (preview-only in this module: no networking lives here). `isOffline` and
 * `articles` are independent on purpose: per the News README, a failed refresh keeps the last
 * known-good stories on screen behind a banner instead of clearing the list.
 */
data class IloveViNewsUiState(
    val articles: List<IloveViNewsArticle> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val updatedAtText: String = "",
)

/**
 * design/components/NewsRow/README.md (v1.1): the News screen. Header (title + refresh) ->
 * "Updated [time]" caption -> topic filter row -> list. States: loading shows
 * [IloveViNewsRowSkeleton] rows, empty shows "No stories yet", offline shows a banner above the
 * (still visible) stored stories with a Retry action.
 */
@Composable
fun IloveViNewsScreen(
    state: IloveViNewsUiState,
    selectedFilter: IloveViNewsFilter,
    onFilterSelect: (IloveViNewsFilter) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onArticleClick: (IloveViNewsArticle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredArticles = when (selectedFilter) {
        IloveViNewsFilter.All -> state.articles
        IloveViNewsFilter.News -> state.articles.filter { it.topic == IloveViNewsTopic.News }
        IloveViNewsFilter.Missions -> state.articles.filter { it.topic == IloveViNewsTopic.Missions }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IloveViColors.bg900),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = IloveViSpacing.space6, vertical = IloveViSpacing.space4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "News".uppercase(),
                color = IloveViColors.ink,
                style = MaterialTheme.typography.displayMedium,
            )
            IloveViIconButton(contentDescription = "Refresh", onClick = onRefresh) { tint -> IconRefresh(tint = tint) }
        }

        Text(
            text = "Updated ${state.updatedAtText}",
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = IloveViSpacing.space6),
        )

        Row(
            modifier = Modifier
                .padding(horizontal = IloveViSpacing.space6, vertical = IloveViSpacing.space4),
            horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space2),
        ) {
            NewsFilterChip(label = "All", active = selectedFilter == IloveViNewsFilter.All, accentColor = IloveViColors.ink, onClick = { onFilterSelect(IloveViNewsFilter.All) })
            NewsFilterChip(label = "News", active = selectedFilter == IloveViNewsFilter.News, accentColor = IloveViNewsTopic.News.color, onClick = { onFilterSelect(IloveViNewsFilter.News) })
            NewsFilterChip(label = "Missions", active = selectedFilter == IloveViNewsFilter.Missions, accentColor = IloveViNewsTopic.Missions.color, onClick = { onFilterSelect(IloveViNewsFilter.Missions) })
        }

        if (state.isOffline) {
            OfflineBanner(onRetry = onRetry, modifier = Modifier.padding(horizontal = IloveViSpacing.space6, vertical = IloveViSpacing.space2))
        }

        when {
            state.isLoading && state.articles.isEmpty() -> {
                LazyColumn(modifier = Modifier.padding(horizontal = IloveViSpacing.space6)) {
                    items(6) { IloveViNewsRowSkeleton() }
                }
            }
            filteredArticles.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No stories yet", color = IloveViColors.inkMuted, style = MaterialTheme.typography.bodyLarge)
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.padding(horizontal = IloveViSpacing.space6)) {
                    items(filteredArticles, key = { it.id }) { article ->
                        IloveViNewsRow(
                            title = article.title,
                            source = article.source,
                            publishedAtText = article.publishedAtText,
                            topic = article.topic,
                            imageUrl = article.imageUrl,
                            onClick = { onArticleClick(article) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(IloveViShapes.radiusMd)
            .background(IloveViColors.bg800, IloveViShapes.radiusMd)
            .border(1.dp, IloveViColors.border, IloveViShapes.radiusMd)
            .padding(IloveViSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space3),
    ) {
        Text(
            text = "You're offline. Showing the last saved stories.",
            color = IloveViColors.ink,
            style = MaterialTheme.typography.bodyLarge,
        )
        IloveViButton(text = "Retry", onClick = onRetry, variant = IloveViButtonVariant.Secondary)
    }
}

/**
 * Visual twin of [com.pamaz.vi.design.components.IloveViChip], kept local to NewsScreen: the
 * Chip README reserves category-colored fills for the three map/catalog categories ("never for
 * anything else"), so the News/Missions topic filters can't reuse Chip's `category` branch
 * as-is. Inactive uses the `border` token (not `bg-800`, unlike the generic Chip) per the
 * News mockup.
 */
@Composable
private fun NewsFilterChip(label: String, active: Boolean, accentColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val background = if (active) accentColor else IloveViColors.border
    val textColor = if (active) IloveViColors.inkOnBrand else IloveViColors.ink
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(IloveViShapes.radiusPill)
            .background(background, IloveViShapes.radiusPill)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = textColor, style = MaterialTheme.typography.titleMedium)
    }
}

private val previewArticles = listOf(
    IloveViNewsArticle(
        id = "1",
        title = "Leonida map leak: new district spotted in the trailer",
        source = "IloveVI Community",
        publishedAtText = "2h ago",
        topic = IloveViNewsTopic.News,
        imageUrl = "https://example.com/leonida.jpg",
        url = "https://example.com/leonida",
    ),
    IloveViNewsArticle(
        id = "2",
        title = "Weekly mission rotation: what changed this week",
        source = "IloveVI Community",
        publishedAtText = "1d ago",
        topic = IloveViNewsTopic.Missions,
        imageUrl = null,
        url = "https://example.com/missions",
    ),
    IloveViNewsArticle(
        id = "3",
        title = "Soundtrack teaser breakdown: every station confirmed so far",
        source = "Radio Leonida Fans",
        publishedAtText = "3d ago",
        topic = IloveViNewsTopic.News,
        imageUrl = null,
        url = "https://example.com/soundtrack",
    ),
)

@Preview
@Composable
private fun IloveViNewsScreenLoadedPreview() {
    IloveViTheme {
        IloveViNewsScreen(
            state = IloveViNewsUiState(articles = previewArticles, updatedAtText = "5 minutes ago"),
            selectedFilter = IloveViNewsFilter.All,
            onFilterSelect = {},
            onRefresh = {},
            onRetry = {},
            onArticleClick = {},
        )
    }
}

@Preview
@Composable
private fun IloveViNewsScreenLoadingPreview() {
    IloveViTheme {
        IloveViNewsScreen(
            state = IloveViNewsUiState(isLoading = true),
            selectedFilter = IloveViNewsFilter.All,
            onFilterSelect = {},
            onRefresh = {},
            onRetry = {},
            onArticleClick = {},
        )
    }
}

@Preview
@Composable
private fun IloveViNewsScreenEmptyPreview() {
    IloveViTheme {
        IloveViNewsScreen(
            state = IloveViNewsUiState(updatedAtText = "just now"),
            selectedFilter = IloveViNewsFilter.All,
            onFilterSelect = {},
            onRefresh = {},
            onRetry = {},
            onArticleClick = {},
        )
    }
}

@Preview
@Composable
private fun IloveViNewsScreenOfflinePreview() {
    IloveViTheme {
        IloveViNewsScreen(
            state = IloveViNewsUiState(articles = previewArticles, isOffline = true, updatedAtText = "2 hours ago"),
            selectedFilter = IloveViNewsFilter.All,
            onFilterSelect = {},
            onRefresh = {},
            onRetry = {},
            onArticleClick = {},
        )
    }
}

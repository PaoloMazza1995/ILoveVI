package com.pamaz.vi.feature.catalog.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pamaz.vi.core.model.Entry
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.components.IloveViChip
import com.pamaz.vi.design.components.IloveViChipGroup
import com.pamaz.vi.design.components.IloveViListRow
import com.pamaz.vi.design.components.IloveViListRowTrailing
import com.pamaz.vi.design.components.IloveViSearchField
import com.pamaz.vi.design.components.icon
import com.pamaz.vi.design.components.labelRes
import com.pamaz.vi.design.components.toIloveViCategory
import com.pamaz.vi.feature.catalog.image.EntryThumbnail
import ilovevi.feature.catalog.generated.resources.Res
import ilovevi.feature.catalog.generated.resources.catalog_category_items
import ilovevi.feature.catalog.generated.resources.catalog_category_vehicles
import ilovevi.feature.catalog.generated.resources.catalog_category_weapons
import ilovevi.feature.catalog.generated.resources.catalog_empty_hint
import ilovevi.feature.catalog.generated.resources.catalog_empty_title
import ilovevi.feature.catalog.generated.resources.catalog_row_subtitle_with_zone
import ilovevi.feature.catalog.generated.resources.catalog_search_placeholder
import ilovevi.feature.catalog.generated.resources.catalog_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Catalog tab (CAT-01..05): title, three category chips (exactly one active), a debounced
 * search field, and the matching list — or a loading/empty state. [onOpenDetail] opens Detail
 * (step 8's navigation contract: `(category, entryId)`, exactly [EntryRef][com.pamaz.vi.core.model.EntryRef]'s shape).
 */
@Composable
fun CatalogScreen(
    onOpenDetail: (category: String, entryId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CatalogScreenContent(
        state = state,
        onCategorySelected = viewModel::onCategorySelected,
        onQueryChanged = viewModel::onQueryChanged,
        onOpenDetail = onOpenDetail,
        modifier = modifier,
    )
}

/** The stateless half of [CatalogScreen] — driven entirely by [state], so a test can exercise it without Koin/a real repository. */
@Composable
internal fun CatalogScreenContent(
    state: CatalogUiState,
    onCategorySelected: (EntryCategory) -> Unit,
    onQueryChanged: (String) -> Unit,
    onOpenDetail: (category: String, entryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = IloveViSpacing.space5, vertical = IloveViSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space4),
    ) {
        Text(
            text = stringResource(Res.string.catalog_title),
            color = IloveViColors.ink,
            style = MaterialTheme.typography.displaySmall,
        )

        IloveViChipGroup {
            IloveViChip(
                label = stringResource(Res.string.catalog_category_weapons),
                active = state.category == EntryCategory.WEAPON,
                onClick = { onCategorySelected(EntryCategory.WEAPON) },
                category = EntryCategory.WEAPON.toIloveViCategory(),
                icon = EntryCategory.WEAPON.icon(),
            )
            IloveViChip(
                label = stringResource(Res.string.catalog_category_vehicles),
                active = state.category == EntryCategory.VEHICLE,
                onClick = { onCategorySelected(EntryCategory.VEHICLE) },
                category = EntryCategory.VEHICLE.toIloveViCategory(),
                icon = EntryCategory.VEHICLE.icon(),
            )
            IloveViChip(
                label = stringResource(Res.string.catalog_category_items),
                active = state.category == EntryCategory.ITEM,
                onClick = { onCategorySelected(EntryCategory.ITEM) },
                category = EntryCategory.ITEM.toIloveViCategory(),
                icon = EntryCategory.ITEM.icon(),
            )
        }

        IloveViSearchField(
            value = state.query,
            onValueChange = onQueryChanged,
            placeholder = stringResource(Res.string.catalog_search_placeholder),
        )

        when {
            state.isLoading -> CatalogLoading()
            state.isEmpty -> CatalogEmptyState()
            else -> CatalogList(entries = state.entries, onOpenDetail = onOpenDetail)
        }
    }
}

@Composable
private fun CatalogLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = IloveViColors.brandPink)
    }
}

@Composable
private fun CatalogEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = IloveViSpacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space2),
    ) {
        Text(
            text = stringResource(Res.string.catalog_empty_title),
            color = IloveViColors.ink,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.catalog_empty_hint),
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CatalogList(
    entries: List<Entry>,
    onOpenDetail: (category: String, entryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(entries, key = { it.entryId }) { entry ->
            val category = entry.category.toIloveViCategory()
            IloveViListRow(
                modifier = Modifier.testTag("catalog-row-${entry.entryId}"),
                title = entry.name,
                subtitle = entry.zone?.let { zone ->
                    stringResource(
                        Res.string.catalog_row_subtitle_with_zone,
                        stringResource(entry.labelRes()),
                        zone,
                    )
                } ?: stringResource(entry.labelRes()),
                trailing = IloveViListRowTrailing.Chevron,
                onClick = { onOpenDetail(entry.category.name, entry.entryId) },
                thumbnail = {
                    EntryThumbnail(
                        imageUrl = entry.imageUrl,
                        icon = entry.icon(),
                        tint = category.color,
                    )
                },
            )
        }
    }
}

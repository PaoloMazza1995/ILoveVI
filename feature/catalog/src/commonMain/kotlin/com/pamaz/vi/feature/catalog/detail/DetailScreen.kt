package com.pamaz.vi.feature.catalog.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.pamaz.vi.core.model.MapConfig
import com.pamaz.vi.core.model.MapPin
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.components.IconArrowBack
import com.pamaz.vi.design.components.IloveViButton
import com.pamaz.vi.design.components.IloveViCategoryBadge
import com.pamaz.vi.design.components.IloveViIconButton
import com.pamaz.vi.design.components.icon
import com.pamaz.vi.design.components.labelRes
import com.pamaz.vi.design.components.toIloveViCategory
import com.pamaz.vi.feature.catalog.image.DetailImage
import com.pamaz.vi.feature.catalog.map.EntryMiniMap
import ilovevi.feature.catalog.generated.resources.Res
import ilovevi.feature.catalog.generated.resources.detail_back_content_description
import ilovevi.feature.catalog.generated.resources.detail_location_not_available
import ilovevi.feature.catalog.generated.resources.detail_not_found
import ilovevi.feature.catalog.generated.resources.detail_show_on_map
import ilovevi.feature.catalog.generated.resources.detail_where_to_find_it
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * An entry's full detail (DET-01..04). [onShowOnMap] matches step 8's navigation contract exactly
 * (`(entryId) -> Unit`, switching to the Map tab with this entry selected) — this screen never
 * navigates itself.
 */
@Composable
fun DetailScreen(
    category: String,
    entryId: String,
    onBack: () -> Unit,
    onShowOnMap: (entryId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = koinViewModel(key = "$category/$entryId") {
        parametersOf(category, entryId)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DetailScreenContent(
        state = state,
        onBack = onBack,
        onShowOnMap = onShowOnMap,
        modifier = modifier,
    )
}

/** The stateless half of [DetailScreen] — see `CatalogScreenContent`'s KDoc for why this split exists. */
@Composable
internal fun DetailScreenContent(
    state: DetailUiState,
    onBack: () -> Unit,
    onShowOnMap: (entryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DetailHeader(onBack = onBack)
        when (state) {
            is DetailUiState.Loading -> DetailLoading()
            is DetailUiState.NotFound -> DetailNotFound()
            is DetailUiState.Loaded -> DetailBody(
                entry = state.entry,
                pin = state.pin,
                mapConfig = state.mapConfig,
                onShowOnMap = onShowOnMap,
            )
        }
    }
}

@Composable
private fun DetailHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(IloveViSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IloveViIconButton(
            contentDescription = stringResource(Res.string.detail_back_content_description),
            onClick = onBack,
        ) { tint -> IconArrowBack(tint = tint) }
    }
}

@Composable
private fun DetailLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = IloveViColors.brandPink)
    }
}

@Composable
private fun DetailNotFound(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().padding(IloveViSpacing.space5),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.detail_not_found),
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun DetailBody(
    entry: Entry,
    pin: MapPin?,
    mapConfig: MapConfig?,
    onShowOnMap: (entryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = entry.category.toIloveViCategory()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = IloveViSpacing.space5)
            .padding(bottom = IloveViSpacing.space6),
        verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space4),
    ) {
        IloveViCategoryBadge(category = category)

        Text(
            text = entry.name,
            color = IloveViColors.ink,
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = stringResource(entry.labelRes()),
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodyLarge,
        )

        DetailImage(
            imageUrl = entry.imageUrl,
            icon = entry.icon(),
            tint = category.color,
            modifier = Modifier.fillMaxWidth()
        )

        WhereToFindIt(entry = entry, pin = pin, mapConfig = mapConfig, onShowOnMap = onShowOnMap)

        Text(
            text = entry.description,
            color = IloveViColors.ink,
            style = MaterialTheme.typography.bodyLarge
        )

        AttributesTable(entry = entry)
    }
}

/**
 * DET-02/DET-03: the mini-map (only drawable when both [pin] and [mapConfig] exist), the zone
 * name and a short description, plus "Show on map" ([onShowOnMap]) — hidden entirely, with
 * `detail_location_not_available` shown instead, when [pin] is `null` (the entry has no known
 * location at all).
 */
@Composable
private fun WhereToFindIt(
    entry: Entry,
    pin: MapPin?,
    mapConfig: MapConfig?,
    onShowOnMap: (entryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space3)
    ) {
        Text(
            text = stringResource(Res.string.detail_where_to_find_it),
            color = IloveViColors.ink,
            style = MaterialTheme.typography.titleMedium,
        )
        if (pin == null) {
            Text(
                text = stringResource(Res.string.detail_location_not_available),
                color = IloveViColors.inkMuted,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            if (mapConfig != null) {
                EntryMiniMap(mapConfig = mapConfig, pin = pin, modifier = Modifier.fillMaxWidth())
            }
            entry.zone?.let { zone ->
                Text(
                    text = zone,
                    color = IloveViColors.inkMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IloveViButton(
                text = stringResource(Res.string.detail_show_on_map),
                onClick = { onShowOnMap(entry.entryId) },
                modifier = Modifier.testTag("show-on-map-button"),
            )
        }
    }
}

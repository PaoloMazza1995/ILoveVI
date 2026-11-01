package com.pamaz.vi.feature.map.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.ItemType
import com.pamaz.vi.core.model.VehicleType
import com.pamaz.vi.core.model.WeaponType
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.components.IconArrowBack
import com.pamaz.vi.design.components.IloveViButton
import com.pamaz.vi.design.components.IloveViButtonVariant
import com.pamaz.vi.design.components.IloveViChip
import com.pamaz.vi.design.components.IloveViChipGroup
import com.pamaz.vi.design.components.IloveViIcon
import com.pamaz.vi.design.components.IloveViIconButton
import com.pamaz.vi.design.components.icon
import com.pamaz.vi.design.components.labelRes
import com.pamaz.vi.design.components.toIloveViCategory
import ilovevi.feature.map.generated.resources.Res
import ilovevi.feature.map.generated.resources.map_back_content_description
import ilovevi.feature.map.generated.resources.map_category_items
import ilovevi.feature.map.generated.resources.map_category_vehicles
import ilovevi.feature.map.generated.resources.map_category_weapons
import ilovevi.feature.map.generated.resources.map_filters_empty_warning
import ilovevi.feature.map.generated.resources.map_filters_reset
import ilovevi.feature.map.generated.resources.map_filters_show_on_map
import ilovevi.feature.map.generated.resources.map_filters_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Filters secondary screen (MAP-05/MAP-09). [onBack] matches step 8's navigation contract
 * (`() -> Unit`, popping this screen back to the Map tab) — "Show on map" is just this same
 * callback, since every chip tap already applies live via the shared [MapFilterState].
 */
@Composable
fun MapFiltersScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapFiltersViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MapFiltersScreenContent(
        state = state,
        onSubTypeToggled = viewModel::onSubTypeToggled,
        onReset = viewModel::onReset,
        onBack = onBack,
        modifier = modifier,
    )
}

/** The stateless half of [MapFiltersScreen] — see `CatalogScreenContent`'s KDoc for why this split exists. */
@Composable
internal fun MapFiltersScreenContent(
    state: MapFiltersUiState,
    onSubTypeToggled: (String) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(IloveViSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IloveViIconButton(
                contentDescription = stringResource(Res.string.map_back_content_description),
                onClick = onBack,
            ) { tint -> IconArrowBack(tint = tint) }
            Text(
                text = stringResource(Res.string.map_filters_title),
                color = IloveViColors.ink,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(start = IloveViSpacing.space2),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = IloveViSpacing.space5),
            verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space5),
        ) {
            state.groups.forEach { group -> CategoryFilterSection(group, onSubTypeToggled) }

            if (state.isEverythingOff) {
                Text(
                    text = stringResource(Res.string.map_filters_empty_warning),
                    color = IloveViColors.warningYellow,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(IloveViSpacing.space5),
            verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space3),
        ) {
            IloveViButton(
                text = stringResource(Res.string.map_filters_reset),
                onClick = onReset,
                variant = IloveViButtonVariant.Secondary,
            )
            IloveViButton(
                text = stringResource(Res.string.map_filters_show_on_map),
                onClick = onBack,
            )
        }
    }
}

private fun categoryLabelRes(category: EntryCategory): StringResource = when (category) {
    EntryCategory.WEAPON -> Res.string.map_category_weapons
    EntryCategory.VEHICLE -> Res.string.map_category_vehicles
    EntryCategory.ITEM -> Res.string.map_category_items
}

@Composable
private fun CategoryFilterSection(
    group: CategoryFilterGroup,
    onSubTypeToggled: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = group.category.toIloveViCategory().color
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space3)
    ) {
        Text(
            text = stringResource(categoryLabelRes(group.category)),
            color = color,
            style = MaterialTheme.typography.titleMedium,
        )
        IloveViChipGroup {
            group.subTypes.forEach { chip ->
                val (icon, label) = subTypeIconAndLabelRes(group.category, chip.name)
                IloveViChip(
                    label = stringResource(label),
                    active = chip.active,
                    onClick = { onSubTypeToggled(chip.name) },
                    icon = icon,
                )
            }
        }
    }
}

/** Resolves a raw sub-type name (as stored in [MapFilters.subTypes]) back to its icon/label, by parsing it as whichever enum [category] owns. */
private fun subTypeIconAndLabelRes(
    category: EntryCategory,
    name: String
): Pair<IloveViIcon, StringResource> =
    when (category) {
        EntryCategory.WEAPON -> WeaponType.valueOf(name).let { it.icon() to it.labelRes() }
        EntryCategory.VEHICLE -> VehicleType.valueOf(name).let { it.icon() to it.labelRes() }
        EntryCategory.ITEM -> ItemType.valueOf(name).let { it.icon() to it.labelRes() }
    }

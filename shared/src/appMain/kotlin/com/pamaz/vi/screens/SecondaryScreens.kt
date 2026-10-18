package com.pamaz.vi.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.components.IloveViButton
import ilovevi.shared.generated.resources.Res
import ilovevi.shared.generated.resources.app_back_content_description
import ilovevi.shared.generated.resources.app_compilation_title
import ilovevi.shared.generated.resources.app_detail_entry_label
import ilovevi.shared.generated.resources.app_detail_title
import ilovevi.shared.generated.resources.app_map_filters_title
import ilovevi.shared.generated.resources.app_show_on_map_action
import ilovevi.shared.generated.resources.app_stub_placeholder
import org.jetbrains.compose.resources.stringResource

/** Opened from the Map tab's "Filters" action. */
@Composable
internal fun MapFiltersScreen(onBack: () -> Unit) {
    SecondaryScreenScaffold(
        title = stringResource(Res.string.app_map_filters_title),
        backContentDescription = stringResource(Res.string.app_back_content_description),
        onBack = onBack,
    ) {
        Text(
            text = stringResource(Res.string.app_stub_placeholder),
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/**
 * An entry's detail. "Show on map" ([onShowOnMap]) returns to the Map tab with this entry
 * selected via the shell's shared selection state — see [com.pamaz.vi.navigation.AppNavHost].
 */
@Composable
internal fun DetailScreen(
    category: String,
    entryId: String,
    onBack: () -> Unit,
    onShowOnMap: (String) -> Unit
) {
    SecondaryScreenScaffold(
        title = stringResource(Res.string.app_detail_title),
        backContentDescription = stringResource(Res.string.app_back_content_description),
        onBack = onBack,
    ) {
        Text(
            text = "${stringResource(Res.string.app_detail_entry_label)}: $category / $entryId",
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
        IloveViButton(
            text = stringResource(Res.string.app_show_on_map_action),
            onClick = { onShowOnMap(entryId) },
        )
    }
}

/** Opened only from the Music tab's compilation card — see [MusicStubScreen]. Never in the bottom bar. */
@Composable
internal fun CompilationScreen(onBack: () -> Unit) {
    SecondaryScreenScaffold(
        title = stringResource(Res.string.app_compilation_title),
        backContentDescription = stringResource(Res.string.app_back_content_description),
        onBack = onBack,
    ) {
        Text(
            text = stringResource(Res.string.app_stub_placeholder),
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

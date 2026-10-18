package com.pamaz.vi.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pamaz.vi.core.mapview.MapViewBuildConfig
import com.pamaz.vi.core.mapview.MapViewerSampleScreen
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.components.IloveViButton
import com.pamaz.vi.design.components.IloveViButtonVariant
import com.pamaz.vi.design.components.IloveViFeaturedCard
import ilovevi.shared.generated.resources.Res
import ilovevi.shared.generated.resources.app_map_selected_entry_label
import ilovevi.shared.generated.resources.app_open_compilation_action
import ilovevi.shared.generated.resources.app_open_filters_action
import ilovevi.shared.generated.resources.app_stub_placeholder
import ilovevi.shared.generated.resources.app_tab_catalog_title
import ilovevi.shared.generated.resources.app_tab_map_title
import ilovevi.shared.generated.resources.app_tab_music_title
import ilovevi.shared.generated.resources.app_tab_news_title
import ilovevi.shared.generated.resources.app_tab_settings_title
import org.jetbrains.compose.resources.stringResource

/**
 * The Map tab (step 8, bullet 7): in a build where [MapViewBuildConfig.singleImageDevModeEnabled]
 * is `true` (every build except one invoked with `-Pilovevi.release=true` — see
 * `:core:mapview`'s README), shows the real, interactive [MapViewerSampleScreen] so the map is
 * visible in the app before `:core:data`/`:feature:map` exist; otherwise shows the plain stub
 * every other tab gets. [selectedEntryId] is "Show on map"'s shared-selection-state argument —
 * forwarded into the sample screen so it can highlight it if it matches one of the sample's fake
 * pins (see [MapViewerSampleScreen]'s KDoc), and shown as plain text in the release stub.
 */
@Composable
internal fun MapTabScreen(selectedEntryId: String?, onOpenFilters: () -> Unit) {
    if (MapViewBuildConfig.singleImageDevModeEnabled) {
        MapViewerSampleScreen(selectedPinId = selectedEntryId)
    } else {
        TabStubScreen(
            title = stringResource(Res.string.app_tab_map_title),
            placeholder = stringResource(Res.string.app_stub_placeholder),
        ) {
            selectedEntryId?.let { entryId ->
                Text(
                    text = "${stringResource(Res.string.app_map_selected_entry_label)}: $entryId",
                    color = IloveViColors.inkMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            IloveViButton(
                text = stringResource(Res.string.app_open_filters_action),
                onClick = onOpenFilters,
                variant = IloveViButtonVariant.Secondary,
            )
        }
    }
}

/** The Catalog tab. [onOpenDetail] (category, entryId) opens the Detail secondary destination. */
@Composable
internal fun CatalogStubScreen(onOpenDetail: (category: String, entryId: String) -> Unit) {
    TabStubScreen(
        title = stringResource(Res.string.app_tab_catalog_title),
        placeholder = stringResource(Res.string.app_stub_placeholder),
    ) {
        // No real content exists before the game's release; this demonstrates the Detail
        // navigation contract (category + entryId, matching EntryRef's shape) with placeholder
        // values rather than leaving "Show on map" unreachable from this step's stub screens.
        IloveViButton(
            text = "Open sample entry",
            onClick = { onOpenDetail("WEAPON", "sample-weapon-1") },
            variant = IloveViButtonVariant.Secondary,
        )
    }
}

/** The Music tab. The compilation card is the *only* way to reach Compilation — not in the bottom bar. */
@Composable
internal fun MusicStubScreen(onOpenCompilation: () -> Unit) {
    TabStubScreen(
        title = stringResource(Res.string.app_tab_music_title),
        placeholder = stringResource(Res.string.app_stub_placeholder),
    ) {
        IloveViFeaturedCard(
            label = stringResource(Res.string.app_tab_music_title),
            value = stringResource(Res.string.app_open_compilation_action),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenCompilation),
        )
    }
}

@Composable
internal fun NewsStubScreen() {
    TabStubScreen(
        title = stringResource(Res.string.app_tab_news_title),
        placeholder = stringResource(Res.string.app_stub_placeholder),
    )
}

@Composable
internal fun SettingsStubScreen() {
    TabStubScreen(
        title = stringResource(Res.string.app_tab_settings_title),
        placeholder = stringResource(Res.string.app_stub_placeholder),
    )
}

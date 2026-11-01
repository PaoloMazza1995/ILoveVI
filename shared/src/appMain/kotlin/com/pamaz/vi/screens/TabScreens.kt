package com.pamaz.vi.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pamaz.vi.design.components.IloveViFeaturedCard
import ilovevi.shared.generated.resources.Res
import ilovevi.shared.generated.resources.app_open_compilation_action
import ilovevi.shared.generated.resources.app_stub_placeholder
import ilovevi.shared.generated.resources.app_tab_music_title
import ilovevi.shared.generated.resources.app_tab_news_title
import ilovevi.shared.generated.resources.app_tab_settings_title
import org.jetbrains.compose.resources.stringResource

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

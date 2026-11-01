package com.pamaz.vi.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.pamaz.vi.design.IloveViColors
import ilovevi.shared.generated.resources.Res
import ilovevi.shared.generated.resources.app_back_content_description
import ilovevi.shared.generated.resources.app_compilation_title
import ilovevi.shared.generated.resources.app_stub_placeholder
import org.jetbrains.compose.resources.stringResource

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

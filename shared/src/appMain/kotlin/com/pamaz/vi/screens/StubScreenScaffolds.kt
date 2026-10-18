package com.pamaz.vi.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.components.IconArrowBack
import com.pamaz.vi.design.components.IloveViIconButton

/**
 * One of this step's two screen shapes: a tab root. A title and placeholder text, built only
 * from design-system tokens/components (step 8's stub-screen requirement) — [extraContent] is
 * where a screen adds the one or two affordances it needs beyond that (e.g. Music's compilation
 * card, Map's filters button) without every tab needing its own from-scratch layout.
 */
@Composable
internal fun TabStubScreen(
    title: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(IloveViSpacing.space5),
        verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space3),
    ) {
        Text(text = title, color = IloveViColors.ink, style = MaterialTheme.typography.displaySmall)
        Text(
            text = placeholder,
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodyLarge
        )
        extraContent()
    }
}

/**
 * This step's other screen shape: a secondary destination, opened on top of a tab with a back
 * control (step 8's "secondary destinations... with a back control" requirement) — a 44dp
 * [IloveViIconButton] back arrow plus the title in one row, then [content] below.
 */
@Composable
internal fun SecondaryScreenScaffold(
    title: String,
    backContentDescription: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(IloveViSpacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IloveViIconButton(
                contentDescription = backContentDescription,
                onClick = onBack
            ) { tint ->
                IconArrowBack(tint = tint)
            }
            Text(
                text = title,
                color = IloveViColors.ink,
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(start = IloveViSpacing.space3),
            )
        }
        Column(
            modifier = Modifier.padding(horizontal = IloveViSpacing.space5),
            verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space3),
        ) {
            content()
        }
    }
}

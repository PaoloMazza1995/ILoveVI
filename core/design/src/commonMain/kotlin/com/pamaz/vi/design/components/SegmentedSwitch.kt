package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViTheme

/**
 * Exactly two mutually-exclusive, equally-weighted segments (e.g. "Existing entry | New entry") —
 * distinct from [IloveViChip]/[IloveViChipGroup] (any number of independently-styled filter pills
 * in a scrollable row) and from [IloveViSwitch] (an on/off boolean toggle with its own thumb/track
 * visual, not a labeled two-way choice). Introduced for the Map Editor Functional Analysis's
 * Inspector entry-source switch (EDT-51); intentionally generic (plain string labels, no icons) so
 * it's reusable for any future two-way segmented choice in the app, not map-editor-specific.
 *
 * Fixed 44dp height to match [IloveViChip]'s own tap-target height; the active segment gets a
 * `bg-900` background inset within the `bg-800` track, active text in [IloveViColors.ink],
 * inactive in [IloveViColors.inkMuted] — same "track + inset active segment" shape `IloveViSwitch`
 * already uses for its own thumb, applied here to two equal halves instead of a sliding dot.
 */
@Composable
fun IloveViSegmentedSwitch(
    leftLabel: String,
    rightLabel: String,
    leftSelected: Boolean,
    onSelectLeft: () -> Unit,
    onSelectRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(IloveViShapes.radiusSm)
            .background(IloveViColors.bg800, IloveViShapes.radiusSm)
            .padding(3.dp),
    ) {
        SegmentedSwitchOption(
            label = leftLabel,
            selected = leftSelected,
            onClick = onSelectLeft,
            modifier = Modifier.weight(1f),
        )
        SegmentedSwitchOption(
            label = rightLabel,
            selected = !leftSelected,
            onClick = onSelectRight,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentedSwitchOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(IloveViShapes.radiusXs)
            .let {
                if (selected) it.background(
                    IloveViColors.bg900,
                    IloveViShapes.radiusXs
                ) else it
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) IloveViColors.ink else IloveViColors.inkMuted,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview
@Composable
private fun IloveViSegmentedSwitchLeftPreview() {
    IloveViTheme {
        IloveViSegmentedSwitch(
            leftLabel = "Existing entry",
            rightLabel = "New entry",
            leftSelected = true,
            onSelectLeft = {},
            onSelectRight = {},
        )
    }
}

@Preview
@Composable
private fun IloveViSegmentedSwitchRightPreview() {
    IloveViTheme {
        IloveViSegmentedSwitch(
            leftLabel = "Existing entry",
            rightLabel = "New entry",
            leftSelected = false,
            onSelectLeft = {},
            onSelectRight = {},
        )
    }
}

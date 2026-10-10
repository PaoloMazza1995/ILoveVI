package com.pamaz.vi.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViNewsTopic
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme

private val ThumbnailSize = 72.dp

/**
 * design/components/NewsRow/README.md (v1.1, News destination): thumbnail + topic badge +
 * headline + meta line, always with a bottom divider. The topic Badge is NOT [IloveViStatusBadge]
 * reused: the Badge README reserves the solid-fill/category-color combination for status
 * confirmations, and the Chip README reserves colored fills for the three map/catalog
 * categories ("never for anything else") — [NewsTopicBadge] is a small, scoped, visually
 * identical twin kept local to this file so those two existing rules stay intact.
 *
 * `onClick` is expected to open the article in the system browser: this design-system module
 * owns no platform launch API, so the caller supplies the behavior.
 */
@Composable
fun IloveViNewsRow(
    title: String,
    source: String,
    publishedAtText: String,
    topic: IloveViNewsTopic,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dividerColor = IloveViColors.border
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ThumbnailSize)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "$title, $source"
                role = Role.Button
            }
            .drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(vertical = IloveViSpacing.space3),
        horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space3),
    ) {
        NewsThumbnail(imageUrl = imageUrl)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space1),
        ) {
            NewsTopicBadge(topic = topic)
            Text(
                text = title,
                color = IloveViColors.ink,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$source · $publishedAtText",
                color = IloveViColors.inkMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * 72dp, radius-sm, `border` background. The image-placeholder icon is drawn first and the
 * AsyncImage on top of it: while there is no URL, or the image hasn't resolved yet, the icon
 * reads straight through — no separate loading-state plumbing needed.
 */
@Composable
private fun NewsThumbnail(imageUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(ThumbnailSize)
            .clip(IloveViShapes.radiusSm)
            .background(IloveViColors.border, IloveViShapes.radiusSm),
        contentAlignment = Alignment.Center,
    ) {
        IconImagePlaceholder(tint = IloveViColors.inkFaint)
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** Solid fill in the topic color, uppercase `label` text in `ink-on-brand`: see [IloveViNewsRow] KDoc. */
@Composable
private fun NewsTopicBadge(topic: IloveViNewsTopic, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(IloveViShapes.radiusPill)
            .background(topic.color, IloveViShapes.radiusPill)
            .padding(horizontal = IloveViSpacing.space3, vertical = 6.dp),
    ) {
        Text(
            text = topic.label.uppercase(),
            color = IloveViColors.inkOnBrand,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/**
 * Loading placeholder, same footprint as [IloveViNewsRow] (72dp thumbnail, 12dp vertical
 * padding, bottom divider): `border`-colored bars stand in for the badge/title/meta while a
 * fetch is in flight. Used by NewsScreen's loading state.
 */
@Composable
fun IloveViNewsRowSkeleton(modifier: Modifier = Modifier) {
    val dividerColor = IloveViColors.border
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ThumbnailSize)
            .drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(vertical = IloveViSpacing.space3),
        horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space3),
    ) {
        Box(
            Modifier
                .size(ThumbnailSize)
                .clip(IloveViShapes.radiusSm)
                .background(IloveViColors.border, IloveViShapes.radiusSm),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space2),
        ) {
            Box(
                Modifier
                    .size(width = 56.dp, height = 18.dp)
                    .clip(IloveViShapes.radiusPill)
                    .background(IloveViColors.border, IloveViShapes.radiusPill),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(15.dp)
                    .clip(IloveViShapes.radiusXs)
                    .background(IloveViColors.border, IloveViShapes.radiusXs),
            )
            Box(
                Modifier
                    .size(width = 140.dp, height = 13.dp)
                    .clip(IloveViShapes.radiusXs)
                    .background(IloveViColors.border, IloveViShapes.radiusXs),
            )
        }
    }
}

@Preview
@Composable
private fun IloveViNewsRowPreview() {
    IloveViTheme {
        Column {
            IloveViNewsRow(
                title = "Leonida map leak: new district spotted in the trailer",
                source = "IloveVI Community",
                publishedAtText = "2h ago",
                topic = IloveViNewsTopic.News,
                imageUrl = "https://example.com/leonida.jpg",
                onClick = {},
            )
            IloveViNewsRow(
                title = "Weekly mission rotation: what changed this week",
                source = "IloveVI Community",
                publishedAtText = "1d ago",
                topic = IloveViNewsTopic.Missions,
                imageUrl = null,
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun IloveViNewsRowSkeletonPreview() {
    IloveViTheme {
        Column {
            IloveViNewsRowSkeleton()
            IloveViNewsRowSkeleton()
        }
    }
}

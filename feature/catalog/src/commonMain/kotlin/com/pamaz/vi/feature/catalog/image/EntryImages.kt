package com.pamaz.vi.feature.catalog.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.components.IloveViIcon
import ilovevi.feature.catalog.generated.resources.Res
import ilovevi.feature.catalog.generated.resources.detail_image_not_available
import org.jetbrains.compose.resources.stringResource

/**
 * Catalog row thumbnail: the category-colored sub-type icon drawn first, [coil3.compose.AsyncImage]
 * layered on top when [imageUrl] is non-null — the same "placeholder underneath, image on top, no
 * separate loading-state plumbing" idiom `:core:design`'s own `NewsRow`/`NewsThumbnail` already
 * uses, reused here rather than invented again. Sized to fill whatever box
 * [com.pamaz.vi.design.components.IloveViListRow] already gives its `thumbnail` slot (56dp by
 * default).
 */
@Composable
internal fun EntryThumbnail(
    imageUrl: String?,
    icon: IloveViIcon,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        IloveViIcon(icon = icon, tint = tint, size = 26.dp)
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

private val DetailImageHeight = 220.dp

/**
 * Detail's hero image area (DET-01): same placeholder-then-image idiom as [EntryThumbnail], at a
 * much larger, fixed height — the placeholder shows a label (`detail_image_not_available`), not
 * just a bare icon, since here it's the entire visible content of the area, not a small 56dp
 * accent next to a title that already says the entry's name.
 */
@Composable
internal fun DetailImage(
    imageUrl: String?,
    icon: IloveViIcon,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(DetailImageHeight)
            .clip(IloveViShapes.radiusLg)
            .background(IloveViColors.bg800, IloveViShapes.radiusLg),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl == null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IloveViIcon(icon = icon, tint = tint, size = 48.dp)
                Text(
                    text = stringResource(Res.string.detail_image_not_available),
                    color = IloveViColors.inkMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

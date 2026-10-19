package com.pamaz.vi.core.mapview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.map.MapViewport
import com.pamaz.vi.core.model.map.ScreenPoint
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.components.IloveViClusterBadge
import com.pamaz.vi.design.components.IloveViMapPin
import com.pamaz.vi.design.components.IloveViMapPinCategory

/** [MIN_HIT_TARGET_PX_AT_DENSITY_1X] expressed as the [Dp][androidx.compose.ui.unit.Dp] both the hit target and the visual target size use. */
private val MinHitTargetSize = 44.dp

private fun EntryCategory.toPinCategory(): IloveViMapPinCategory = when (this) {
    EntryCategory.WEAPON -> IloveViMapPinCategory.Weapon
    EntryCategory.VEHICLE -> IloveViMapPinCategory.Vehicle
    EntryCategory.ITEM -> IloveViMapPinCategory.Item
}

internal fun EntryCategory.accessibleName(): String = when (this) {
    EntryCategory.WEAPON -> "Weapon"
    EntryCategory.VEHICLE -> "Vehicle"
    EntryCategory.ITEM -> "Item"
}

/**
 * Every [MapViewerItem] in ONE layer, offset from normalized coordinates via the exact same
 * [MapViewport.toScreen] conversion [MapGeometry]'s hit-testing uses — pins/clusters and tiles
 * ([TileLayer]) are therefore always in agreement and never drift apart at any zoom level, since
 * both derive their screen position from the same [TileScheme]/center/zoom inputs.
 *
 * Items outside the viewport (plus a margin, so one whose 44dp target is only half on-screen is
 * still composed and hit-testable) are skipped entirely — this, plus [Modifier.offset]'s
 * lambda-based placement (no per-frame recomposition, only re-layout), is what keeps pan/zoom
 * smooth with a large item count.
 *
 * Draw order (and therefore stacking order, since a [Box]'s later children paint on top): every
 * cluster first, then every non-selected pin, then the selected pin last — so the selected pin is
 * always on top of any cluster (and of every other pin), regardless of [items]' own order.
 */
@Composable
internal fun PinLayer(
    items: List<MapViewerItem>,
    scheme: TileScheme,
    state: MapViewerState,
    selectedPinId: String?,
    onPinTap: (String) -> Unit,
    onClusterTap: (MapViewerCluster) -> Unit,
    modifier: Modifier = Modifier,
    pinContent: @Composable (pin: MapViewerPin, selected: Boolean) -> Unit = { pin, selected ->
        DefaultMapViewerPinContent(pin, selected)
    },
) {
    val viewportSize = state.viewportSize
    if (viewportSize.width <= 0 || viewportSize.height <= 0) return
    val viewportWidth = viewportSize.width.toFloat()
    val viewportHeight = viewportSize.height.toFloat()

    val density = LocalDensity.current
    val halfTargetPx = with(density) { (MinHitTargetSize / 2).toPx() }
    val cullingMarginPx = with(density) { MinHitTargetSize.toPx() }

    fun isVisible(screen: ScreenPoint): Boolean =
        screen.x >= -cullingMarginPx && screen.x <= viewportWidth + cullingMarginPx &&
                screen.y >= -cullingMarginPx && screen.y <= viewportHeight + cullingMarginPx

    val pins = items.mapNotNull { (it as? MapViewerItem.Pin)?.pin }
    val clusters = items.mapNotNull { (it as? MapViewerItem.Cluster)?.cluster }
    val selectedPin = pins.find { it.id == selectedPinId }

    Box(modifier = modifier) {
        for (cluster in clusters) {
            val screen = MapViewport.toScreen(
                cluster.position, scheme, state.center, state.zoom, viewportWidth, viewportHeight,
            )
            if (!isVisible(screen)) continue

            key(cluster.id) {
                Box(
                    modifier = Modifier.offset {
                        IntOffset(
                            (screen.x - halfTargetPx).toInt(),
                            (screen.y - halfTargetPx).toInt(),
                        )
                    },
                ) {
                    IloveViClusterBadge(
                        count = cluster.count,
                        category = cluster.dominantCategory?.toPinCategory()?.category,
                        onClick = { onClusterTap(cluster) },
                    )
                }
            }
        }

        for (pin in pins) {
            if (pin.id == selectedPinId) continue
            PinMarker(
                pin,
                selected = false,
                scheme,
                state,
                viewportWidth,
                viewportHeight,
                halfTargetPx,
                ::isVisible,
                onPinTap,
                pinContent
            )
        }

        selectedPin?.let { pin ->
            PinMarker(
                pin,
                selected = true,
                scheme,
                state,
                viewportWidth,
                viewportHeight,
                halfTargetPx,
                ::isVisible,
                onPinTap,
                pinContent
            )
        }
    }
}

/** One pin's offset/culling/semantics/content — factored out of [PinLayer] so it's called once for unselected pins and once more for the selected one, without duplicating this block. */
@Composable
private fun PinMarker(
    pin: MapViewerPin,
    selected: Boolean,
    scheme: TileScheme,
    state: MapViewerState,
    viewportWidth: Float,
    viewportHeight: Float,
    halfTargetPx: Float,
    isVisible: (ScreenPoint) -> Boolean,
    onPinTap: (String) -> Unit,
    pinContent: @Composable (pin: MapViewerPin, selected: Boolean) -> Unit,
) {
    val screen = MapViewport.toScreen(
        pin.position, scheme, state.center, state.zoom, viewportWidth, viewportHeight,
    )
    if (!isVisible(screen)) return

    key(pin.id) {
        val accessibleLabel = pin.label?.let { "$it, ${pin.category.accessibleName()}" }
            ?: pin.category.accessibleName()

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (screen.x - halfTargetPx).toInt(),
                        (screen.y - halfTargetPx).toInt()
                    )
                }
                .size(MinHitTargetSize)
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibleLabel
                    role = Role.Button
                    onClick { onPinTap(pin.id); true }
                },
            contentAlignment = Alignment.Center,
        ) {
            pinContent(pin, selected)
        }
    }
}

/**
 * The viewer's default pin look: [IloveViMapPin] (category color/icon, ring when [selected]) with
 * an optional small label pill underneath. A caller may still override this (via [PinLayer]'s
 * `pinContent` / [MapViewer]'s `pinContent`) to customize a single pin's look, but clusters are no
 * longer drawn through this hook — see [MapViewerItem]'s KDoc: a [MapViewerCluster] is its own
 * item kind, always drawn with `:core:designsystem`'s `IloveViClusterBadge`.
 */
@Composable
internal fun DefaultMapViewerPinContent(pin: MapViewerPin, selected: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        IloveViMapPin(category = pin.category.toPinCategory(), selected = selected)
        pin.label?.let { label ->
            Box(
                modifier = Modifier
                    .offset(y = 28.dp)
                    .clip(IloveViShapes.radiusPill)
                    .background(IloveViColors.bg800, IloveViShapes.radiusPill)
                    .padding(horizontal = IloveViSpacing.space2, vertical = 2.dp),
            ) {
                Text(
                    text = label,
                    color = IloveViColors.ink,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

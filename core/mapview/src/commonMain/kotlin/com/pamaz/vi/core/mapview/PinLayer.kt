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
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
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
 * All pins in ONE layer, offset from normalized coordinates via the exact same
 * [MapViewport.toScreen] conversion [MapGeometry]'s hit-testing uses — pins and tiles ([TileLayer])
 * are therefore always in agreement and never drift apart at any zoom level, since both derive
 * their screen position from the same [TileScheme]/center/zoom inputs.
 *
 * Pins outside the viewport (plus a margin, so a pin whose 44dp target is only half on-screen is
 * still composed and hit-testable) are skipped entirely — this, plus [Modifier.offset]'s
 * lambda-based placement (no per-frame recomposition, only re-layout), is what keeps pan/zoom
 * smooth with a large pin count.
 */
@Composable
internal fun PinLayer(
    pins: List<MapViewerPin>,
    scheme: TileScheme,
    state: MapViewerState,
    selectedPinId: String?,
    onPinTap: (String) -> Unit,
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

    Box(modifier = modifier) {
        for (pin in pins) {
            val screen = MapViewport.toScreen(
                pin.position,
                scheme,
                state.center,
                state.zoom,
                viewportWidth,
                viewportHeight
            )
            val visible =
                screen.x >= -cullingMarginPx && screen.x <= viewportWidth + cullingMarginPx &&
                        screen.y >= -cullingMarginPx && screen.y <= viewportHeight + cullingMarginPx
            if (!visible) continue

            key(pin.id) {
                val selected = pin.id == selectedPinId
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
    }
}

/**
 * The viewer's default pin look: [IloveViMapPin] (category color/icon, ring when [selected]) with
 * an optional small label pill underneath. `feature:map` overrides this (via [PinLayer]'s
 * `pinContent` / [MapViewer]'s `pinContent`) to draw cluster bubbles instead — see
 * [MapViewerPin]'s KDoc for why that's the clustering hook rather than a separate marker type.
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

package com.pamaz.vi.design.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.design.IloveViCategory
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViNewsTopic
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.design.components.IconCheck
import com.pamaz.vi.design.components.IconArrowBack
import com.pamaz.vi.design.components.IconMusicNote
import com.pamaz.vi.design.components.IconWeaponCrosshair
import com.pamaz.vi.design.components.IconZoomIn
import com.pamaz.vi.design.components.IloveViButton
import com.pamaz.vi.design.components.IloveViButtonVariant
import com.pamaz.vi.design.components.IloveViCategoryBadge
import com.pamaz.vi.design.components.IloveViChip
import com.pamaz.vi.design.components.IloveViChipGroup
import com.pamaz.vi.design.components.IloveViCompactCard
import com.pamaz.vi.design.components.IloveViFeaturedCard
import com.pamaz.vi.design.components.IloveViIconButton
import com.pamaz.vi.design.components.IloveViIconButtonSurface
import com.pamaz.vi.design.components.IloveViLabeledSwitch
import com.pamaz.vi.design.components.IloveViListRow
import com.pamaz.vi.design.components.IloveViListRowTrailing
import com.pamaz.vi.design.components.IloveViMapControls
import com.pamaz.vi.design.components.IloveViMapPin
import com.pamaz.vi.design.components.IloveViMapPinCategory
import com.pamaz.vi.design.components.IloveViNavBar
import com.pamaz.vi.design.components.IloveViNavDestination
import com.pamaz.vi.design.components.IloveViNewsRow
import com.pamaz.vi.design.components.IloveViSearchField
import com.pamaz.vi.design.components.IloveViStatusBadge

/**
 * Visual catalog of all the design system's components and their variants, grouped by
 * section: used to check the entire system at a glance in a single scrollable view.
 */
@Composable
fun ComponentGalleryScreen() {
    IloveViTheme {
        Box(modifier = Modifier.fillMaxSize().background(IloveViColors.bg900)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(IloveViSpacing.space6),
                verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space7),
            ) {
                GallerySection("Button") {
                    IloveViButton(text = "Show on map", onClick = {}, variant = IloveViButtonVariant.Primary)
                    IloveViButton(text = "Add more tracks", onClick = {}, variant = IloveViButtonVariant.Secondary)
                    IloveViButton(text = "Not available", onClick = {}, enabled = false)
                }

                GallerySection("IconButton") {
                    Row(horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space4)) {
                        IloveViIconButton(contentDescription = "Back", onClick = {}) { tint -> IconArrowBack(tint = tint) }
                        IloveViIconButton(
                            contentDescription = "Zoom in",
                            onClick = {},
                            surface = IloveViIconButtonSurface.OnMap,
                        ) { tint -> IconZoomIn(tint = tint) }
                    }
                }

                GallerySection("MapControls") {
                    IloveViMapControls(onZoomIn = {}, onZoomOut = {})
                }

                GallerySection("Chip") {
                    var allActive by remember { mutableStateOf(true) }
                    var radioActive by remember { mutableStateOf(false) }
                    IloveViChipGroup {
                        IloveViChip(label = "All", active = allActive, onClick = { allActive = true; radioActive = false })
                        IloveViChip(label = "Radio station", active = radioActive, onClick = { radioActive = true; allActive = false })
                    }
                    var weaponsActive by remember { mutableStateOf(true) }
                    var vehiclesActive by remember { mutableStateOf(true) }
                    var itemsActive by remember { mutableStateOf(true) }
                    IloveViChipGroup {
                        IloveViChip(label = "Weapons", active = weaponsActive, onClick = { weaponsActive = !weaponsActive }, category = IloveViCategory.Weapons)
                        IloveViChip(label = "Vehicles", active = vehiclesActive, onClick = { vehiclesActive = !vehiclesActive }, category = IloveViCategory.Vehicles)
                        IloveViChip(label = "Items", active = itemsActive, onClick = { itemsActive = !itemsActive }, category = IloveViCategory.Items)
                    }
                }

                GallerySection("SearchField") {
                    var query by remember { mutableStateOf("") }
                    IloveViSearchField(value = query, onValueChange = { query = it }, placeholder = "Search by name or area")
                }

                GallerySection("ListRow") {
                    Column {
                        IloveViListRow(
                            title = "Equipped pistol",
                            subtitle = "Weapons · Vinewood",
                            onClick = {},
                            thumbnail = { IconWeaponCrosshair(tint = IloveViColors.brandPink, size = 26.dp) },
                        )
                        IloveViListRow(
                            title = "In the Wind",
                            subtitle = "Radio Vice · 3:24",
                            thumbnailSize = 48.dp,
                            thumbnailShape = RoundedCornerShape(10.dp),
                            trailing = IloveViListRowTrailing.Action(
                                contentDescription = "Add to playlist",
                                onClick = {},
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(IloveViColors.brandTeal, CircleShape),
                                        contentAlignment = Alignment.Center,
                                    ) { IconCheck(tint = IloveViColors.inkOnBrand) }
                                },
                            ),
                            thumbnail = { IconMusicNote(tint = IloveViColors.inkFaint, size = 22.dp) },
                        )
                    }
                }

                GallerySection("Card") {
                    IloveViFeaturedCard(label = "Map", value = "7 items nearby", modifier = Modifier.fillMaxWidth())
                    IloveViCompactCard(
                        title = "Equipped pistol",
                        subtitle = "Weapons · Vinewood",
                        category = IloveViCategory.Weapons,
                        modifier = Modifier.fillMaxWidth(),
                        icon = { tint -> IconWeaponCrosshair(tint = tint, size = 24.dp) },
                    )
                }

                GallerySection("Badge") {
                    Row(horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space2)) {
                        IloveViCategoryBadge(category = IloveViCategory.Weapons)
                        IloveViCategoryBadge(category = IloveViCategory.Vehicles)
                        IloveViCategoryBadge(category = IloveViCategory.Items)
                    }
                    IloveViStatusBadge(text = "Added", color = IloveViColors.brandTeal)
                }

                GallerySection("NavBar") {
                    var selected by remember { mutableStateOf(IloveViNavDestination.Map) }
                    IloveViNavBar(selected = selected, onSelect = { selected = it })
                }

                GallerySection("NewsRow") {
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

                GallerySection("Switch") {
                    var syncOn by remember { mutableStateOf(true) }
                    var labelsOn by remember { mutableStateOf(false) }
                    IloveViLabeledSwitch(label = "Auto sync", checked = syncOn, onCheckedChange = { syncOn = it })
                    IloveViLabeledSwitch(label = "Map labels", checked = labelsOn, onCheckedChange = { labelsOn = it })
                }

                GallerySection("MapPin") {
                    Row(horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space6)) {
                        IloveViMapPin(category = IloveViMapPinCategory.Weapon)
                        IloveViMapPin(category = IloveViMapPinCategory.Vehicle)
                        IloveViMapPin(category = IloveViMapPinCategory.Item)
                        IloveViMapPin(category = IloveViMapPinCategory.Weapon, selected = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun GallerySection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space3)) {
        Text(text = title, color = IloveViColors.ink, style = MaterialTheme.typography.displaySmall)
        Column(verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space3), content = content)
    }
}

@Preview
@Composable
private fun ComponentGalleryScreenPreview() {
    ComponentGalleryScreen()
}

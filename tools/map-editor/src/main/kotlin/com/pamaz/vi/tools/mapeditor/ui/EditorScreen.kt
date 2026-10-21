package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.mapview.MapContent
import com.pamaz.vi.core.mapview.MapViewer
import com.pamaz.vi.core.mapview.MapViewerPin
import com.pamaz.vi.core.mapview.rememberMapViewerState
import com.pamaz.vi.core.mapview.singleImageTileScheme
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.design.components.IloveViMapPin
import com.pamaz.vi.design.components.IloveViMapPinCategory
import com.pamaz.vi.tools.mapeditor.geometry.NudgeStepPx
import com.pamaz.vi.tools.mapeditor.geometry.NudgeStepPxLarge
import com.pamaz.vi.tools.mapeditor.geometry.movePinByScreenDelta
import com.pamaz.vi.tools.mapeditor.state.EditorState
import com.pamaz.vi.tools.mapeditor.state.EntryFilter
import kotlinx.coroutines.launch

private fun EntryCategory.toPinCategory(): IloveViMapPinCategory = when (this) {
    EntryCategory.WEAPON -> IloveViMapPinCategory.Weapon
    EntryCategory.VEHICLE -> IloveViMapPinCategory.Vehicle
    EntryCategory.ITEM -> IloveViMapPinCategory.Item
}

/**
 * The tool's main screen (PLT-02..05, 07): map in the middle, searchable/filterable entry list on
 * the left (PLT-03), all-pins list on the right (PLT-05), a toolbar for save/export/new-map-version.
 */
@Composable
fun EditorScreen(
    state: EditorState,
    onOpenImport: () -> Unit,
    onOpenExport: () -> Unit,
    onNewMapVersion: () -> Unit
) {
    val content by state.mapContent

    Column(Modifier.fillMaxSize()) {
        Toolbar(state, onOpenImport, onOpenExport, onNewMapVersion)
        Row(Modifier.fillMaxSize()) {
            EntryListPanel(state, Modifier.width(280.dp).fillMaxHeight())
            Box(Modifier.weight(1f).fillMaxHeight()) {
                val currentContent = content
                if (currentContent != null) {
                    MapPane(state, currentContent)
                } else {
                    Text("No map loaded.", modifier = Modifier.padding(16.dp))
                }
            }
            PinListPanel(state, Modifier.width(300.dp).fillMaxHeight())
        }
    }
}

@Composable
private fun Toolbar(
    state: EditorState,
    onOpenImport: () -> Unit,
    onOpenExport: () -> Unit,
    onNewMapVersion: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val isOffline by state.isOffline
    val session by state.adminSession.collectAsState()
    val status by state.statusMessage.collectAsState()
    val error by state.lastError.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }

    Column {
        if (isOffline) {
            Text(
                "Offline: not connected to the backend",
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(8.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                scope.launch { if (isOffline) state.saveOffline() else state.save() }
            }) { Text(if (state.hasUnsavedChanges) "Save*" else "Save") }
            TextButton(onClick = onOpenImport) { Text("Import") }
            TextButton(onClick = onOpenExport) { Text("Export") }
            Box {
                TextButton(onClick = { menuOpen = true }) { Text("Map") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("New map version...") },
                        onClick = { menuOpen = false; onNewMapVersion() })
                }
            }
            Text(session?.email ?: "Not signed in", modifier = Modifier.padding(start = 8.dp))
            (status ?: error)?.let { Text(it, modifier = Modifier.padding(start = 8.dp)) }
        }
    }
}

@Composable
private fun EntryListPanel(state: EditorState, modifier: Modifier) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(EntryFilter.ALL) }
    val armed by state.armedEntry

    Column(modifier.padding(8.dp)) {
        Text("Entries", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth()) {
            EntryFilter.entries.forEach { f ->
                TextButton(onClick = { filter = f }) {
                    Text(
                        f.name,
                        style = if (filter == f) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        LazyColumn {
            items(state.entryListItems(filter, query)) { item ->
                val isArmed = armed?.entryRef == item.summary.entryRef
                Row(
                    Modifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(if (isArmed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                        .clickable { state.armEntry(item.summary) }
                        .padding(4.dp),
                ) {
                    Text(if (item.hasPin) "● " else "○ ")
                    Text(item.summary.name)
                }
            }
        }
    }
}

@Composable
private fun PinListPanel(state: EditorState, modifier: Modifier) {
    val pins = state.pins
    val repositionIds = state.pinsToReposition.map { it.id }.toSet()

    Column(modifier.padding(8.dp)) {
        Text("Pins (${pins.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f)) {
            items(pins) { pin ->
                val suffix = if (pin.id in repositionIds) " (to reposition)" else ""
                Text(
                    "${pin.entryRef.category.name} ${pin.zone ?: ""}  x=${"%.4f".format(pin.position.x)} y=${
                        "%.4f".format(
                            pin.position.y
                        )
                    }$suffix",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        .clickable { state.selectPinById(pin.id.value) },
                )
            }
        }
        Button(onClick = { state.deleteSelectedPin() }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Delete selected pin")
        }
    }
}

@Composable
private fun MapPane(state: EditorState, content: MapContent) {
    val mapViewerState = rememberMapViewerState()
    val scheme = remember(content) {
        when (content) {
            is MapContent.Tiles -> TileScheme(content.config.widthPx, content.config.heightPx)
            is MapContent.SingleImage -> singleImageTileScheme(
                content.image.width,
                content.image.height
            )
        }
    }
    val pins = state.pins.map {
        MapViewerPin(
            id = it.id.value,
            position = it.position,
            category = it.entryRef.category
        )
    }
    val selectedId = state.selectedPin?.id?.value
    val focusRequester = remember { FocusRequester() }
    val armed by state.armedEntry

    Box(
        Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                val selectedPosition = state.selectedPin?.position ?: return@onKeyEvent false
                val step = if (event.isShiftPressed) NudgeStepPxLarge else NudgeStepPx
                val (dx, dy) = when (event.key) {
                    Key.DirectionLeft -> -step to 0f
                    Key.DirectionRight -> step to 0f
                    Key.DirectionUp -> 0f to -step
                    Key.DirectionDown -> 0f to step
                    else -> return@onKeyEvent false
                }
                state.moveSelectedPin(
                    movePinByScreenDelta(
                        selectedPosition,
                        scheme,
                        mapViewerState.zoom,
                        dx,
                        dy
                    )
                )
                true
            },
    ) {
        MapViewer(
            state = mapViewerState,
            content = content,
            pins = pins,
            selectedPinId = selectedId,
            onPinTap = { id ->
                state.selectPinById(id)
                focusRequester.requestFocus()
            },
            onMapTap = { point ->
                if (armed != null) state.placeOrMoveArmedPinAt(point)
                focusRequester.requestFocus()
            },
            modifier = Modifier.fillMaxSize(),
            pinContent = { pin, selected ->
                Box(
                    modifier = Modifier.pointerInput(pin.id, scheme) {
                        detectDragGestures(onDragStart = { state.selectPinById(pin.id) }) { change, dragAmount ->
                            change.consume()
                            val current = state.positionOfPin(pin.id) ?: return@detectDragGestures
                            state.moveSelectedPin(
                                movePinByScreenDelta(
                                    current,
                                    scheme,
                                    mapViewerState.zoom,
                                    dragAmount.x,
                                    dragAmount.y
                                )
                            )
                        }
                    },
                ) {
                    IloveViMapPin(category = pin.category.toPinCategory(), selected = selected)
                }
            },
        )
    }
}

package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.mapview.MapContent
import com.pamaz.vi.core.mapview.MapViewer
import com.pamaz.vi.core.mapview.MapViewerPin
import com.pamaz.vi.core.mapview.rememberMapViewerState
import com.pamaz.vi.core.mapview.singleImageTileScheme
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.core.model.map.MapViewport
import com.pamaz.vi.core.model.map.ScreenPoint
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViShapes
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.components.IloveViButton
import com.pamaz.vi.design.components.IloveViButtonVariant
import com.pamaz.vi.design.components.IloveViEmptyStateCard
import com.pamaz.vi.design.components.IloveViMapPin
import com.pamaz.vi.tools.mapeditor.geometry.movePinByScreenDelta
import com.pamaz.vi.tools.mapeditor.model.EditMode
import com.pamaz.vi.tools.mapeditor.state.EditorState
import com.pamaz.vi.tools.mapeditor.state.EditorUiState

/** EDT-11: database pins are "dimmed, outline only" relative to a draft pin's full-opacity fill. */
private const val DbPinAlpha = 0.5f

/**
 * §4 region 5: the map on a darker pasteboard. Draft pins (≤500, interactive) go through
 * [MapViewer]'s proven `items`/`pinContent` click/drag/pan-safe path; database pins (≤2000,
 * read-only, EDT-11: "not clickable") draw in a single [Canvas] layer with no `pointerInput` at
 * all — never competing with [MapViewer]'s own gestures, and exactly where a naive one-node-per-
 * pin approach would cost the most. See this module's README for why the two pin sets are handled
 * differently.
 */
@Composable
fun MapArea(
    uiState: EditorUiState,
    editorState: EditorState,
    onPinPlaced: (String) -> Unit,
    onCursorMove: (NormalizedPoint?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val content = uiState.mapContent
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IloveViColors.bg900)
            .padding(24.dp),
    ) {
        if (content == null) {
            val error = uiState.mapLoadError
            IloveViEmptyStateCard(
                title = "No map loaded",
                body = when {
                    uiState.isLoading -> "Loading..."
                    error != null -> buildString {
                        append(error.message)
                        error.requestedUrl?.let { append("\n$it") }
                        uiState.invalidMapConfigRow?.let { row ->
                            append("\ntile_url_template: ${row.tileUrlTemplate ?: "(unknown)"}")
                            append("\nsource_image_url: ${row.sourceImageUrl ?: "(unknown)"}")
                        }
                    }

                    else -> "No map is configured."
                },
            ) {
                if (error != null) {
                    IloveViButton(
                        text = "Retry",
                        onClick = editorState::load,
                        variant = IloveViButtonVariant.Secondary,
                        modifier = Modifier.padding(top = IloveViSpacing.space3),
                    )
                }
                IloveViButton(
                    text = "Load map image...",
                    onClick = {
                        FilePickers.pickOpenFile("Image", "png", "jpg", "jpeg")
                            ?.let(editorState::startMapPublish)
                    },
                    modifier = Modifier.padding(top = IloveViSpacing.space3),
                )
                IloveViButton(
                    text = "Open local image (offline, no upload)",
                    onClick = {
                        FilePickers.pickOpenFile("Image", "png", "jpg", "jpeg")
                            ?.let(editorState::openLocalImage)
                    },
                    variant = IloveViButtonVariant.Secondary,
                    modifier = Modifier.padding(top = IloveViSpacing.space2),
                )
            }
            return@Box
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(IloveViShapes.radiusLg)
                .background(IloveViColors.bg800),
        ) {
            MapCanvas(uiState, editorState, content, onPinPlaced, onCursorMove)
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun MapCanvas(
    uiState: EditorUiState,
    editorState: EditorState,
    content: MapContent,
    onPinPlaced: (String) -> Unit,
    onCursorMove: (NormalizedPoint?) -> Unit,
) {
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

    var shiftHeld by remember { mutableStateOf(false) }
    var marqueeStart by remember { mutableStateOf<NormalizedPoint?>(null) }
    var marqueeCurrent by remember { mutableStateOf<NormalizedPoint?>(null) }
    val focusRequester = remember { FocusRequester() }

    val draftItems = uiState.draftPins.map {
        MapViewerPin(
            id = it.id,
            position = it.position,
            category = it.category
        )
    }
    val draftPinsById = uiState.draftPins.associateBy { it.id }

    Box(
        Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPointerEvent(PointerEventType.Move) { event ->
                val position = event.changes.first().position
                val viewportSize = mapViewerState.viewportSize
                if (viewportSize.width > 0 && viewportSize.height > 0) {
                    // The cursor can legitimately sit outside the map's 0..1 bounds (hovering the
                    // pasteboard around it); NormalizedPoint validates its range, so an out-of-bounds
                    // hover is simply "no coordinates to show," not an error.
                    val point = try {
                        MapViewport.toNormalized(
                            ScreenPoint(position.x, position.y),
                            scheme,
                            mapViewerState.center,
                            mapViewerState.zoom,
                            viewportSize.width.toFloat(),
                            viewportSize.height.toFloat(),
                        )
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                    onCursorMove(point)
                }
            }
            .onPointerEvent(PointerEventType.Exit) { onCursorMove(null) }
            .onKeyEvent { event ->
                when (event.key) {
                    Key.ShiftLeft, Key.ShiftRight -> {
                        shiftHeld = event.type == KeyEventType.KeyDown
                        false
                    }

                    Key.Delete, Key.Backspace -> {
                        if (event.type == KeyEventType.KeyDown && uiState.mode == EditMode.SELECT) {
                            editorState.deleteSelected(); true
                        } else {
                            false
                        }
                    }

                    Key.Escape -> {
                        if (event.type == KeyEventType.KeyDown) {
                            editorState.clearSelection(); true
                        } else {
                            false
                        }
                    }

                    Key.Z -> {
                        val isUndoRedoChord = event.isCtrlPressed || event.isMetaPressed
                        if (event.type == KeyEventType.KeyDown && isUndoRedoChord) {
                            if (event.isShiftPressed) editorState.redo() else editorState.undo()
                            true
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            },
    ) {
        MapViewer(
            state = mapViewerState,
            content = content,
            pins = draftItems,
            selectedPinId = uiState.selectedPinIds.singleOrNull(),
            onPinTap = { id ->
                focusRequester.requestFocus()
                if (uiState.mode == EditMode.SELECT) editorState.select(id, additive = shiftHeld)
            },
            onMapTap = { point ->
                focusRequester.requestFocus()
                when (uiState.mode) {
                    EditMode.PLACE -> onPinPlaced(editorState.placeAt(point).id)
                    EditMode.SELECT -> if (!shiftHeld) editorState.clearSelection()
                }
            },
            modifier = Modifier.fillMaxSize(),
            backgroundDragSelectEnabled = uiState.mode == EditMode.SELECT,
            onBackgroundDragStart = { point -> marqueeStart = point; marqueeCurrent = point },
            onBackgroundDrag = { point -> marqueeCurrent = point },
            onBackgroundDragEnd = {
                val start = marqueeStart
                val end = marqueeCurrent
                if (start != null && end != null) editorState.selectWithin(start, end, shiftHeld)
                marqueeStart = null
                marqueeCurrent = null
            },
            pinContent = { pin, _ ->
                val draftPin = draftPinsById[pin.id]
                val incomplete = draftPin?.isComplete(uiState.draftEntriesById) == false
                val icon = iconFor(pin.category, draftPin?.subType)
                val isSelected = pin.id in uiState.selectedPinIds
                Box(
                    modifier = Modifier.pointerInput(pin.id, scheme, uiState.mode) {
                        if (uiState.mode != EditMode.SELECT) return@pointerInput
                        detectDragGestures(
                            onDragStart = { focusRequester.requestFocus(); editorState.select(pin.id) },
                        ) { change, dragAmount ->
                            change.consume()
                            val current =
                                draftPinsById[pin.id]?.position ?: return@detectDragGestures
                            val delta = movePinByScreenDelta(
                                current, scheme, mapViewerState.zoom, dragAmount.x, dragAmount.y,
                            )
                            editorState.moveSelectedTo(pin.id, delta)
                        }
                    },
                ) {
                    IloveViMapPin(
                        category = pin.category.toPinCategory(),
                        icon = icon,
                        diameter = 34.dp,
                        selected = isSelected,
                        incomplete = incomplete,
                    )
                }
            },
        )

        if (uiState.showDbPins) {
            Canvas(Modifier.fillMaxSize()) {
                val viewportWidth = size.width
                val viewportHeight = size.height
                uiState.dbPins.forEach { dbPin ->
                    val screen = MapViewport.toScreen(
                        dbPin.position, scheme, mapViewerState.center, mapViewerState.zoom,
                        viewportWidth, viewportHeight,
                    )
                    drawCircle(
                        color = dbPin.category.toPinCategory().category.color.copy(alpha = DbPinAlpha),
                        radius = 11.dp.toPx(),
                        center = Offset(screen.x, screen.y),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
    }
}

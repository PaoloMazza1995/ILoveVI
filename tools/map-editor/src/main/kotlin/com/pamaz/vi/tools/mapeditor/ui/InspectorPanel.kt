package com.pamaz.vi.tools.mapeditor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.NormalizedPoint
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.IloveViTheme
import com.pamaz.vi.design.components.IloveViButton
import com.pamaz.vi.design.components.IloveViButtonVariant
import com.pamaz.vi.design.components.IloveViChip
import com.pamaz.vi.design.components.IloveViChipGroup
import com.pamaz.vi.design.components.IloveViCompactCard
import com.pamaz.vi.design.components.IloveViEmptyStateCard
import com.pamaz.vi.design.components.IloveViIcon
import com.pamaz.vi.design.components.IloveViListRow
import com.pamaz.vi.design.components.IloveViMapPin
import com.pamaz.vi.design.components.IloveViSearchField
import com.pamaz.vi.design.components.IloveViSegmentedSwitch
import com.pamaz.vi.design.components.IloveViStatusBadge
import com.pamaz.vi.design.components.IloveViTextField
import com.pamaz.vi.tools.mapeditor.data.fake.FakeEntryRepository
import com.pamaz.vi.tools.mapeditor.model.DraftEntry
import com.pamaz.vi.tools.mapeditor.model.DraftPin
import com.pamaz.vi.tools.mapeditor.model.EntryFields
import com.pamaz.vi.tools.mapeditor.model.EntrySource
import com.pamaz.vi.tools.mapeditor.model.rawEntryId
import com.pamaz.vi.tools.mapeditor.model.subTypes
import com.pamaz.vi.tools.mapeditor.state.EditorState
import com.pamaz.vi.tools.mapeditor.state.EditorUiState
import java.io.File

/** §4 region 7 / §5.6: the selected pin's editor — link an existing entry, or create one inline. */
@Composable
fun InspectorPanel(
    uiState: EditorUiState,
    editorState: EditorState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(IloveViSpacing.space4),
    ) {
        when {
            uiState.selectedPinIds.isEmpty() -> IloveViEmptyStateCard(
                title = "No pin selected",
                body = "Place a new pin or select an existing one on the map to edit it here.",
            )

            uiState.selectedPinIds.size > 1 -> MultiSelectActions(uiState, editorState)
            else -> SinglePinInspector(uiState, editorState, uiState.selectedPinIds.first())
        }
    }
}

@Composable
private fun MultiSelectActions(uiState: EditorUiState, editorState: EditorState) {
    Text(
        "${uiState.selectedPinIds.size} pins selected",
        color = IloveViColors.ink,
        style = MaterialTheme.typography.titleMedium,
    )
    Row(
        Modifier.padding(top = IloveViSpacing.space3),
        horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space2)
    ) {
        EntryCategory.entries.forEach { category ->
            IloveViChip(
                label = category.name,
                active = false,
                onClick = {
                    uiState.selectedPinIds.forEach { pinId ->
                        editorState.setCategoryForPin(
                            pinId,
                            category
                        )
                    }
                },
            )
        }
    }
    IloveViButton(
        text = "Delete all",
        onClick = editorState::deleteSelected,
        variant = IloveViButtonVariant.Secondary,
        modifier = Modifier.padding(top = IloveViSpacing.space3),
    )
}

@Composable
private fun SinglePinInspector(uiState: EditorUiState, editorState: EditorState, pinId: String) {
    val pin = uiState.draftPins.find { it.id == pinId } ?: return
    val isComplete = pin.isComplete(uiState.draftEntriesById)

    InspectorHeader(pin, uiState, isComplete)

    if (pin.entry == null) {
        Text(
            "Category",
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(
                top = IloveViSpacing.space3,
                bottom = IloveViSpacing.space1
            ),
        )
        IloveViChipGroup {
            EntryCategory.entries.forEach { category ->
                IloveViChip(
                    label = category.name,
                    active = category == pin.category,
                    onClick = { editorState.setCategoryForPin(pinId, category) },
                )
            }
        }
    }

    var showExisting by remember(pinId) { mutableStateOf(pin.entry !is EntrySource.Draft) }

    IloveViSegmentedSwitch(
        leftLabel = "Existing entry",
        rightLabel = "New entry",
        leftSelected = showExisting,
        onSelectLeft = { showExisting = true },
        onSelectRight = { showExisting = false },
        modifier = Modifier.padding(top = IloveViSpacing.space4, bottom = IloveViSpacing.space3),
    )

    if (showExisting) {
        ExistingEntrySide(pin, uiState, editorState)
    } else {
        NewEntrySide(pin, uiState, editorState)
    }

    val sharedCount = when (val source = pin.entry) {
        is EntrySource.Db -> uiState.draftPins.count { it.id != pin.id && it.entry == source }
        is EntrySource.Draft -> uiState.draftPins.count { it.id != pin.id && it.entry == source }
        null -> 0
    }
    if (sharedCount > 0) {
        Text(
            "Used by ${sharedCount + 1} pins",
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = IloveViSpacing.space2),
        )
    }

    if (pin.entry != null) {
        IloveViButton(
            text = "Place another pin of this entry",
            onClick = { editorState.setActiveEntry(pin.entry) },
            modifier = Modifier.padding(top = IloveViSpacing.space4),
        )
    }

    IloveViButton(
        text = "Delete pin",
        onClick = { editorState.select(pinId); editorState.deleteSelected() },
        variant = IloveViButtonVariant.Secondary,
        modifier = Modifier.padding(top = IloveViSpacing.space2),
    )
}

@Composable
private fun InspectorHeader(pin: DraftPin, uiState: EditorUiState, isComplete: Boolean) {
    val entryName = when (val source = pin.entry) {
        null -> "New ${pin.category.name.lowercase()} pin"
        is EntrySource.Db -> uiState.dbEntries.find { it.entryRef.rawEntryId() == source.id }?.name
            ?: source.id

        is EntrySource.Draft -> uiState.draftEntriesById[source.id]?.name?.takeIf { it.isNotBlank() }
            ?: "New ${pin.category.name.lowercase()} pin"
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space3)
    ) {
        IloveViMapPin(
            category = pin.category.toPinCategory(),
            icon = iconFor(pin.category, pin.subType),
            diameter = 32.dp
        )
        Column(Modifier.weight(1f)) {
            Text(
                entryName,
                color = IloveViColors.ink,
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                "x ${"%.3f".format(pin.position.x)} y ${"%.3f".format(pin.position.y)} · ${pin.subType.lowercase()}",
                color = IloveViColors.inkMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        IloveViStatusBadge(
            text = if (isComplete) "Ready" else "Needs an entry",
            color = if (isComplete) IloveViColors.brandTeal else IloveViColors.warningYellow,
        )
    }
}

@Composable
private fun ExistingEntrySide(pin: DraftPin, uiState: EditorUiState, editorState: EditorState) {
    val linkedDbEntry = (pin.entry as? EntrySource.Db)?.let { source ->
        uiState.dbEntries.find { it.entryRef.rawEntryId() == source.id }
    }
    if (linkedDbEntry != null) {
        IloveViCompactCard(
            title = linkedDbEntry.name,
            subtitle = "${linkedDbEntry.subType.lowercase()}${
                linkedDbEntry.zone?.let { " · $it" }.orEmpty()
            }",
            category = linkedDbEntry.entryRef.category.toDesignCategory(),
            icon = { tint ->
                IloveViIcon(
                    icon = iconFor(
                        linkedDbEntry.entryRef.category,
                        linkedDbEntry.subType
                    ), tint = tint
                )
            },
        )
        return
    }

    var query by remember(pin.id) { mutableStateOf("") }
    IloveViSearchField(
        value = query,
        onValueChange = { query = it },
        placeholder = "Search by name or zone"
    )

    val matches = uiState.dbEntries.filter {
        it.entryRef.category == pin.category &&
                (query.isBlank() || it.name.contains(query, ignoreCase = true) || it.zone?.contains(
                    query,
                    ignoreCase = true
                ) == true)
    }
    if (matches.isEmpty()) {
        Text(
            "No match. Try a different search, or switch to New entry.",
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = IloveViSpacing.space2),
        )
    } else {
        LazyColumn(Modifier.height(240.dp)) {
            items(matches) { entry ->
                IloveViListRow(
                    title = entry.name,
                    subtitle = "${entry.subType.lowercase()}${
                        entry.zone?.let { " · $it" }.orEmpty()
                    }",
                    onClick = { editorState.linkExisting(pin.id, entry) },
                    thumbnail = {
                        IloveViIcon(
                            icon = iconFor(entry.entryRef.category, entry.subType),
                            tint = IloveViColors.inkMuted
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun NewEntrySide(pin: DraftPin, uiState: EditorUiState, editorState: EditorState) {
    val existingDraft = (pin.entry as? EntrySource.Draft)?.let { uiState.draftEntriesById[it.id] }

    var name by remember(pin.id) { mutableStateOf(existingDraft?.name ?: "") }
    var zone by remember(pin.id) { mutableStateOf(existingDraft?.zone ?: "") }
    var description by remember(pin.id) { mutableStateOf(existingDraft?.description ?: "") }
    var subType by remember(pin.id) { mutableStateOf(existingDraft?.subType ?: pin.subType) }
    var extra by remember(pin.id) { mutableStateOf(existingDraft?.extra ?: emptyMap()) }
    var imageFile by remember(pin.id) { mutableStateOf(existingDraft?.imageFile) }
    var imageError by remember(pin.id) { mutableStateOf<String?>(null) }

    IloveViTextField(
        value = name,
        onValueChange = { name = it },
        label = "Name",
        placeholder = "e.g. Falcon .45",
        errorText = "Name is required".takeIf { name.isBlank() },
        modifier = Modifier.testTag("new-entry-name"),
    )

    Text(
        "Sub-type",
        color = IloveViColors.inkMuted,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.padding(top = IloveViSpacing.space3, bottom = IloveViSpacing.space1),
    )
    IloveViChipGroup {
        pin.category.subTypes().forEach { option ->
            IloveViChip(
                label = option.lowercase(),
                active = option == subType,
                onClick = { subType = option })
        }
    }

    Box(Modifier.padding(top = IloveViSpacing.space3)) {
        IloveViTextField(
            value = zone,
            onValueChange = { zone = it },
            label = "Zone",
            placeholder = "e.g. Vice Beach"
        )
    }
    Box(Modifier.padding(top = IloveViSpacing.space3)) {
        IloveViTextField(
            value = description,
            onValueChange = { description = it },
            label = "Description",
            singleLine = false
        )
    }

    EntryFields.extraFieldsFor(pin.category).forEach { spec ->
        Box(Modifier.padding(top = IloveViSpacing.space3)) {
            IloveViTextField(
                value = extra[spec.key].orEmpty(),
                onValueChange = { extra = extra + (spec.key to it) },
                label = spec.label,
            )
        }
    }

    Box(Modifier.padding(top = IloveViSpacing.space3)) {
        ImagePicker(imageFile, onPick = {
            val file = FilePickers.pickOpenFile("Image", "png", "jpg", "jpeg", "webp")
            if (file != null) {
                imageError = validateImage(file)
                if (imageError == null) imageFile = file
            }
        }, onRemove = { imageFile = null; imageError = null }, errorText = imageError)
    }

    val duplicate = editorState.duplicateMatch(pin.category, name)
    if (duplicate != null) {
        Column(Modifier.padding(top = IloveViSpacing.space2)) {
            Text(
                "An entry named \"${duplicate.name}\" already exists in this category.",
                color = IloveViColors.warningYellow,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Use the existing entry",
                color = IloveViColors.brandPink,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = IloveViSpacing.space1)
                    .clickable { editorState.useExistingInstead(pin.id, duplicate) },
            )
        }
    }

    val extraErrors = EntryFields.validateExtra(pin.category, extra)
    IloveViButton(
        text = if (existingDraft != null) "Save entry" else "Create entry",
        enabled = name.isNotBlank() && extraErrors.isEmpty() && imageError == null,
        onClick = {
            editorState.stageEntry(
                pin.id,
                pin.category,
                subType,
                name,
                zone.ifBlank { null },
                description,
                extra,
                imageFile
            )
        },
        modifier = Modifier.padding(top = IloveViSpacing.space3),
    )
}

@Composable
private fun ImagePicker(file: File?, onPick: () -> Unit, onRemove: () -> Unit, errorText: String?) {
    Column {
        Text(
            "Image (optional)",
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            Modifier.padding(top = IloveViSpacing.space1).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space2),
        ) {
            Text(
                file?.name ?: "No image chosen",
                color = if (file != null) IloveViColors.ink else IloveViColors.inkMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Choose...",
                color = IloveViColors.brandPink,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable(onClick = onPick),
            )
            if (file != null) {
                Text(
                    "Remove",
                    color = IloveViColors.inkFaint,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable(onClick = onRemove),
                )
            }
        }
        errorText?.let {
            Text(
                it,
                color = IloveViColors.warningYellow,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview
@Composable
private fun InspectorPanelEmptyPreview() {
    IloveViTheme {
        InspectorPanel(uiState = EditorUiState(), editorState = previewEditorState())
    }
}

@Preview
@Composable
private fun InspectorPanelNewEntryFormPreview() {
    val pin = DraftPin(
        id = "pin-1",
        position = NormalizedPoint(0.42f, 0.58f),
        category = EntryCategory.WEAPON,
        subType = "PISTOL",
        entry = EntrySource.Draft("draft-1"),
    )
    val uiState = EditorUiState(
        draftPins = listOf(pin),
        draftEntries = listOf(
            DraftEntry(
                id = "draft-1",
                category = EntryCategory.WEAPON,
                subType = "PISTOL",
                name = ""
            ),
        ),
        selectedPinIds = setOf(pin.id),
    )
    IloveViTheme {
        InspectorPanel(uiState = uiState, editorState = previewEditorState())
    }
}

@Preview
@Composable
private fun InspectorPanelExistingEntryPickerPreview() {
    val pin = DraftPin(
        id = "pin-1",
        position = NormalizedPoint(0.3f, 0.3f),
        category = EntryCategory.WEAPON,
        subType = "PISTOL",
        entry = null,
    )
    val uiState = EditorUiState(
        draftPins = listOf(pin),
        selectedPinIds = setOf(pin.id),
        dbEntries = FakeEntryRepository.ALL,
    )
    IloveViTheme {
        InspectorPanel(uiState = uiState, editorState = previewEditorState())
    }
}

private const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
private val ALLOWED_IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp")

/** EDT-80's edge case: "Image too large or wrong type — inline error under the image field; nothing is attached." */
private fun validateImage(file: File): String? {
    if (file.extension.lowercase() !in ALLOWED_IMAGE_EXTENSIONS) return "Choose a PNG, JPEG or WebP image."
    if (file.length() > MAX_IMAGE_BYTES) return "Image is larger than 5 MB."
    return null
}

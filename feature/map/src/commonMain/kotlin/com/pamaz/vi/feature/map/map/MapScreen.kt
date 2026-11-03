package com.pamaz.vi.feature.map.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pamaz.vi.core.data.repository.CatalogRepository
import com.pamaz.vi.core.data.repository.MapPinWithEntryName
import com.pamaz.vi.core.mapview.MapContent
import com.pamaz.vi.core.mapview.MapViewer
import com.pamaz.vi.core.mapview.MapViewerItem
import com.pamaz.vi.core.mapview.MapViewerPin
import com.pamaz.vi.core.mapview.rememberMapViewerState
import com.pamaz.vi.core.model.Entry
import com.pamaz.vi.core.model.EntryCategory
import com.pamaz.vi.core.model.EntryRef
import com.pamaz.vi.core.model.map.TileScheme
import com.pamaz.vi.design.IloveViColors
import com.pamaz.vi.design.IloveViSpacing
import com.pamaz.vi.design.components.IconFilter
import com.pamaz.vi.design.components.IconSearch
import com.pamaz.vi.design.components.IloveViButton
import com.pamaz.vi.design.components.IloveViChip
import com.pamaz.vi.design.components.IloveViChipGroup
import com.pamaz.vi.design.components.IloveViCompactCard
import com.pamaz.vi.design.components.IloveViIcon
import com.pamaz.vi.design.components.IloveViIconButton
import com.pamaz.vi.design.components.IloveViMapControls
import com.pamaz.vi.design.components.IloveViSearchField
import com.pamaz.vi.design.components.icon
import com.pamaz.vi.design.components.toIloveViCategory
import com.pamaz.vi.feature.map.cluster.CLUSTER_DISTANCE_PX
import com.pamaz.vi.feature.map.cluster.ClusterablePin
import com.pamaz.vi.feature.map.cluster.PinClusterer
import ilovevi.feature.map.generated.resources.Res
import ilovevi.feature.map.generated.resources.map_category_items
import ilovevi.feature.map.generated.resources.map_category_vehicles
import ilovevi.feature.map.generated.resources.map_category_weapons
import ilovevi.feature.map.generated.resources.map_config_missing_hint
import ilovevi.feature.map.generated.resources.map_config_missing_title
import ilovevi.feature.map.generated.resources.map_empty_hint
import ilovevi.feature.map.generated.resources.map_empty_title
import ilovevi.feature.map.generated.resources.map_error_title
import ilovevi.feature.map.generated.resources.map_filters_content_description
import ilovevi.feature.map.generated.resources.map_pin_category_zone
import ilovevi.feature.map.generated.resources.map_search_content_description
import ilovevi.feature.map.generated.resources.map_search_placeholder
import ilovevi.feature.map.generated.resources.map_view_details
import ilovevi.feature.map.generated.resources.map_wordmark
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

private val ClusterZoomPaddingDp = 32.dp

/** The selected-pin bottom sheet's collapsed ("peek") height: just enough for the compact card + "View details" — dragging it up past this reveals the entry's description underneath. */
private val SheetPeekHeight = 220.dp

/** How far the camera has to pan, in screen pixels, before re-clustering is worth doing again — see the `remember` key comment in [MapLoadedContent] for why. Half [CLUSTER_DISTANCE_PX]: finer movement than that can't change which pins [PinClusterer] groups together. */
private const val ClusterRecomputeThresholdPx = CLUSTER_DISTANCE_PX / 2f

/**
 * The Map tab (MAP-01..11, MST-04..07). [selectedEntryId] matches step 8's navigation contract:
 * when it changes to a non-null, currently visible entry, the camera animates there and selects
 * its pin — "Show on map" from Detail. [onOpenDetail] matches
 * [com.pamaz.vi.core.model.EntryRef]'s shape exactly, the same contract `:feature:catalog`'s
 * `CatalogScreen.onOpenDetail` already uses.
 *
 * **No user position anywhere in this screen**: the camera only ever moves because of an explicit
 * tap, zoom-button press, "Show on map" selection or cluster-tap zoom — never because of a device
 * location. There is no location marker, no "center on player" control, and nothing here reads
 * any location API.
 */
@Composable
fun MapScreen(
    selectedEntryId: String?,
    onOpenFilters: () -> Unit,
    onOpenDetail: (category: String, entryId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    MapScreenContent(
        state = state,
        selectedEntryId = selectedEntryId,
        searchQuery = searchQuery,
        searchResults = searchResults,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCategoryToggled = viewModel::onCategoryToggled,
        onPinSelected = viewModel::onPinSelected,
        onBackgroundTapped = viewModel::onBackgroundTapped,
        onSelectByEntryId = viewModel::selectByEntryId,
        onOpenFilters = onOpenFilters,
        onOpenDetail = onOpenDetail,
        modifier = modifier,
    )
}

/** The stateless half of [MapScreen] — see `CatalogScreenContent`'s KDoc for why this split exists. */
@Composable
internal fun MapScreenContent(
    state: MapUiState,
    selectedEntryId: String?,
    searchQuery: String,
    searchResults: List<MapPinWithEntryName>,
    onSearchQueryChanged: (String) -> Unit,
    onCategoryToggled: (EntryCategory) -> Unit,
    onPinSelected: (String) -> Unit,
    onBackgroundTapped: () -> Unit,
    onSelectByEntryId: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onOpenDetail: (category: String, entryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val loaded = state as? MapUiState.Loaded

    Column(modifier = modifier.fillMaxSize()) {
        MapHeader(
            onSearchClick = { searchExpanded = !searchExpanded },
            onFiltersClick = onOpenFilters,
        )

        if (searchExpanded) {
            IloveViSearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = stringResource(Res.string.map_search_placeholder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = IloveViSpacing.space5, vertical = IloveViSpacing.space2),
            )
        }

        if (searchExpanded && searchQuery.isNotBlank()) {
            // Modifier.weight(1f), not fillMaxSize() — see the other branch's Box below for why a
            // non-weighted Column child can't resolve "fill the rest of the screen" on its own.
            MapSearchResults(
                results = searchResults,
                onResultSelected = { entryId ->
                    onSelectByEntryId(entryId)
                    searchExpanded = false
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        } else {
            if (loaded != null) {
                IloveViChipGroup(
                    modifier = Modifier.padding(
                        horizontal = IloveViSpacing.space5,
                        vertical = IloveViSpacing.space2,
                    ),
                ) {
                    CategoryChip(EntryCategory.WEAPON, loaded.activeCategories, onCategoryToggled)
                    CategoryChip(EntryCategory.VEHICLE, loaded.activeCategories, onCategoryToggled)
                    CategoryChip(EntryCategory.ITEM, loaded.activeCategories, onCategoryToggled)
                }
            }

            // `Modifier.weight(1f)`, not `fillMaxSize()`: this Box is a non-weighted `Column`
            // child would be measured with an *unbounded* max height (Compose's `Column` always
            // gives non-weighted children `maxHeight = Infinity` so their intrinsic content size
            // isn't artificially capped) — `fillMaxSize()` can't "fill infinity", so without
            // `weight()` this collapsed down to whatever tiny size its own content happened to
            // report, which is exactly the "map renders as a small floating square instead of
            // filling the screen" bug: `MapViewer`'s `viewportSize` (read from this Box's own
            // measured size via `onSizeChanged`) was never actually the real screen size to begin
            // with, so every zoom/fit computation downstream was already working off a tiny
            // viewport no matter what zoom value it produced.
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (state) {
                    is MapUiState.Loading -> MapLoading()
                    is MapUiState.MapConfigMissing -> MapMessage(
                        title = stringResource(Res.string.map_config_missing_title),
                        hint = stringResource(Res.string.map_config_missing_hint),
                    )

                    is MapUiState.Error -> MapMessage(
                        title = stringResource(Res.string.map_error_title),
                        hint = state.message,
                    )

                    is MapUiState.Loaded -> MapLoadedContent(
                        state = state,
                        selectedEntryId = selectedEntryId,
                        onPinSelected = onPinSelected,
                        onBackgroundTapped = onBackgroundTapped,
                        onSelectByEntryId = onSelectByEntryId,
                        onOpenDetail = onOpenDetail,
                    )
                }
            }
        }
    }
}

private fun categoryLabelRes(category: EntryCategory) = when (category) {
    EntryCategory.WEAPON -> Res.string.map_category_weapons
    EntryCategory.VEHICLE -> Res.string.map_category_vehicles
    EntryCategory.ITEM -> Res.string.map_category_items
}

@Composable
private fun CategoryChip(
    category: EntryCategory,
    activeCategories: Set<EntryCategory>,
    onCategoryToggled: (EntryCategory) -> Unit,
) {
    IloveViChip(
        label = stringResource(categoryLabelRes(category)),
        active = category in activeCategories,
        onClick = { onCategoryToggled(category) },
        category = category.toIloveViCategory(),
        icon = category.icon(),
    )
}

@Composable
private fun MapHeader(
    onSearchClick: () -> Unit,
    onFiltersClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = IloveViSpacing.space5, vertical = IloveViSpacing.space3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(Res.string.map_wordmark),
            color = IloveViColors.brandPink,
            style = MaterialTheme.typography.displaySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(IloveViSpacing.space2)) {
            IloveViIconButton(
                contentDescription = stringResource(Res.string.map_search_content_description),
                onClick = onSearchClick,
            ) { tint -> IconSearch(tint = tint) }
            IloveViIconButton(
                contentDescription = stringResource(Res.string.map_filters_content_description),
                onClick = onFiltersClick,
            ) { tint -> IconFilter(tint = tint) }
        }
    }
}

@Composable
private fun MapSearchResults(
    results: List<MapPinWithEntryName>,
    onResultSelected: (entryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(results, key = { it.pin.id.value }) { result ->
            val category = result.pin.category.toIloveViCategory()
            val entryId = result.pin.entryRef.rawEntryId()
            IloveViCompactCard(
                title = result.entryName,
                subtitle = result.pin.zone ?: stringResource(categoryLabelRes(result.pin.category)),
                category = category,
                icon = { tint ->
                    IloveViIcon(
                        icon = result.pin.category.icon(),
                        tint = tint,
                        size = 24.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = IloveViSpacing.space5, vertical = IloveViSpacing.space1)
                    .clickable { onResultSelected(entryId) },
            )
        }
    }
}

@Composable
private fun MapLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = IloveViColors.brandPink)
    }
}

@Composable
private fun MapMessage(title: String, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(IloveViSpacing.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title, color = IloveViColors.ink, style = MaterialTheme.typography.titleMedium)
        Text(
            text = hint,
            color = IloveViColors.inkMuted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = IloveViSpacing.space2),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapLoadedContent(
    state: MapUiState.Loaded,
    selectedEntryId: String?,
    onPinSelected: (String) -> Unit,
    onBackgroundTapped: () -> Unit,
    onSelectByEntryId: (String) -> Unit,
    onOpenDetail: (category: String, entryId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // minZoom/maxZoom passed directly, not left at rememberMapViewerState()'s 0f/0f default: this
    // only matters for the very first frame or two, before MapViewer's own internal effect (see
    // its KDoc) widens them to the real, viewport-aware range — but a 0f/0f window, how ever brief,
    // is exactly the state `animateTo`/`setZoomRange` calls made during it would clamp into.
    //
    // Wrapped in `key(...)`, but on only widthPx/heightPx/minZoom/maxZoom — *not* the whole
    // `state.mapConfig` object (an earlier version of this fix used `key(state.mapConfig)`
    // directly, which turned out to cause a real, reported bug: "dragging the map resets the
    // zoom". `MapConfig` carries fields that can legitimately change on an ordinary resync without
    // the map itself changing at all (`updatedAt`, `tileUrlTemplate`, `mapVersion`, ...); keying on
    // the whole object meant any one of those discarded this *entire* `MapViewerState` — center,
    // zoom, everything the user had just panned/zoomed to — and rebuilt a fresh one from scratch.
    // The narrower key here only reconstructs the camera when the geometry it was actually built
    // for has changed) — on a cold app start, the locally-bundled seed map config (a small
    // 1024x1024 placeholder) is available immediately, before `ContentSyncer` has had a chance to
    // replace it with the real synced one, so `MapUiState.Loaded` (and this composable) can
    // genuinely run *twice* with two different, differently-*sized* `MapConfig`s in quick
    // succession — that specific transition still needs a fresh `MapViewerState` (its old
    // minZoom/maxZoom bounds, sized for the seed's small placeholder image, are wrong for the real
    // one), which this key still catches.
    //
    // The initial "fit the viewport" zoom and the "can't zoom out past the map's own edges" floor
    // both live in `:core:mapview`'s `MapViewer` itself now (see its KDoc on exactly why, and the
    // `coverFitZoom`/`setZoomRange` it uses) — the same treatment `MapContent.SingleImage` mode
    // already had; this screen doesn't need its own copy of that logic for `MapContent.Tiles` mode
    // anymore, and an earlier, now-removed duplicate here raced against `MapViewer`'s own version
    // of the same correction.
    val cameraState = key(
        state.mapConfig.widthPx,
        state.mapConfig.heightPx,
        state.mapConfig.minZoom,
        state.mapConfig.maxZoom,
    ) {
        rememberMapViewerState(
            minZoom = state.mapConfig.minZoom.toFloat(),
            maxZoom = state.mapConfig.maxZoom.toFloat(),
        )
    }
    val scheme = remember(state.mapConfig.widthPx, state.mapConfig.heightPx) {
        TileScheme(state.mapConfig.widthPx, state.mapConfig.heightPx)
    }
    val clusterer = remember { PinClusterer() }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Lowest-zoom-levels prefetch ("never blank, even offline from first launch") now lives
    // inside `:core:mapview`'s own `MapViewer`/`TileEngine` (pinned cache levels, loaded once per
    // map version) — this screen no longer needs its own copy; see that module's README "Tile
    // pipeline" section. `TilePrefetch.kt`'s old Coil3-`SingletonImageLoader`-based version is
    // removed entirely, per this step's "no local tile loading/caching code in feature:map" rule.

    // Re-clustering is real work (a full grid-bucket + union-find pass — see `PinClusterer`'s own
    // KDoc); keying `remember` on the camera's *exact* zoom/center re-triggers it on literally
    // every frame of an active pinch/pan gesture (both change continuously, many times a second),
    // which is a real, measured source of the lag reported against this screen. Rounding to a
    // coarse grid for the *key* only (the block below still clusters against the camera's precise,
    // un-rounded `cameraState.zoom`/`cameraState.center`) cuts that down to only re-clustering when
    // the camera has moved far enough to plausibly change any grouping — pin *positions* never lag
    // behind the real camera either way, since `MapViewer`'s own `PinLayer` always places them from
    // the live camera state directly, independent of when this list was last recomputed.
    //
    // The rounding grid is expressed in *screen pixels* (via the current zoom's scale), not raw
    // normalized-coordinate units: a fixed normalized threshold means a fixed fraction of the
    // *whole map*, which is a wildly different number of screen pixels depending on zoom — at low
    // zoom (zoomed out, usually when the *most* pins and the *most* clustering work are in play)
    // a fixed-normalized threshold is tiny in screen terms and barely throttles anything; at high
    // zoom it's needlessly huge. Tying it to actual on-screen movement keeps the throttle doing
    // the same job at every zoom level. `CLUSTER_DISTANCE_PX / 2` is the threshold: finer than
    // that, a pan genuinely can't have changed any grouping decision `PinClusterer` would make.
    val currentScale = scheme.scaleAt(cameraState.zoom)
    val panThresholdX = (ClusterRecomputeThresholdPx / (scheme.widthPx * currentScale)).toFloat()
    val panThresholdY = (ClusterRecomputeThresholdPx / (scheme.heightPx * currentScale)).toFloat()
    val clusterZoomKey = (cameraState.zoom * 5f).roundToInt()
    val clusterCenterXKey = (cameraState.center.x / panThresholdX).roundToInt()
    val clusterCenterYKey = (cameraState.center.y / panThresholdY).roundToInt()
    val items = remember(
        state.pins,
        state.selectedPinId,
        state.showLabels,
        clusterZoomKey,
        clusterCenterXKey,
        clusterCenterYKey,
        cameraState.maxZoom,
        cameraState.viewportSize,
        scheme,
    ) {
        val clusterablePins = state.pins.map { entry ->
            ClusterablePin(
                id = entry.pin.id.value,
                position = entry.pin.position,
                category = entry.pin.category,
                label = if (state.showLabels) entry.entryName else null,
            )
        }
        val grouped = clusterer.cluster(
            pins = clusterablePins,
            scheme = scheme,
            center = cameraState.center,
            zoom = cameraState.zoom,
            maxZoom = cameraState.maxZoom,
            viewportWidthPx = cameraState.viewportSize.width.toFloat(),
            viewportHeightPx = cameraState.viewportSize.height.toFloat(),
            excludedPinId = state.selectedPinId,
        )
        // PinClusterer excludes the selected pin from every group (MAP-11) but never draws it
        // itself (see its KDoc) — this is the caller adding it back as its own, always-on-top
        // single pin; MapViewer's own PinLayer separately guarantees the selected pin paints over
        // every cluster/pin regardless of list order, so appending it last here is just for
        // clarity, not something the draw order actually depends on.
        val selected = clusterablePins.find { it.id == state.selectedPinId }
        if (selected != null) {
            grouped + MapViewerItem.Pin(
                MapViewerPin(
                    id = selected.id,
                    position = selected.position,
                    category = selected.category,
                    label = selected.label
                )
            )
        } else {
            grouped
        }
    }

    // "Show on map" (MAP-06): re-runs only when selectedEntryId or the pin list actually changes,
    // so re-entering this tab with the same already-focused entry never re-animates the camera.
    LaunchedEffect(selectedEntryId, state.pins) {
        if (selectedEntryId == null) return@LaunchedEffect
        val match =
            state.pins.firstOrNull { entry -> entry.pin.entryRef.rawEntryId() == selectedEntryId }
        if (match != null) {
            onSelectByEntryId(selectedEntryId)
            val targetZoom = (state.mapConfig.minZoom + 2)
                .coerceAtMost(state.mapConfig.maxZoom)
                .toFloat()
            cameraState.animateTo(match.pin.position, targetZoom)
        }
    }

    // Selecting a pin opens this sheet peeked to the compact summary (MAP-04); dragging it up
    // reveals the entry's description underneath ("all the infos", without leaving the map).
    // Dragging it back down past the peek (SheetValue.Hidden) clears the selection exactly the
    // same way tapping the map's background already does, via the second effect below — the sheet
    // and [state.selectedPinId] can never disagree about whether something is selected.
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    LaunchedEffect(state.selectedSummary) {
        if (state.selectedSummary != null) sheetState.partialExpand() else sheetState.hide()
    }
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Hidden) onBackgroundTapped()
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = if (state.selectedSummary != null) SheetPeekHeight else 0.dp,
        sheetContainerColor = IloveViColors.bg900,
        sheetContentColor = IloveViColors.ink,
        containerColor = Color.Transparent,
        sheetContent = {
            state.selectedSummary?.let { summary ->
                MapPinBottomSheetContent(
                    summary = summary,
                    onViewDetails = { onOpenDetail(summary.category.name, summary.entryId) },
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MapViewer(
                state = cameraState,
                content = MapContent.Tiles(state.mapConfig),
                items = items,
                selectedPinId = state.selectedPinId,
                onPinTap = onPinSelected,
                onClusterTap = { cluster ->
                    coroutineScope.launch {
                        val paddingPx = with(density) { ClusterZoomPaddingDp.toPx() }
                        cameraState.animateToBounds(scheme, cluster.bounds, paddingPx)
                    }
                },
                onMapTap = { onBackgroundTapped() },
                modifier = Modifier.fillMaxSize(),
            )

            IloveViMapControls(
                onZoomIn = {
                    coroutineScope.launch {
                        cameraState.animateTo(
                            cameraState.center,
                            cameraState.zoom + 1f
                        )
                    }
                },
                onZoomOut = {
                    coroutineScope.launch {
                        cameraState.animateTo(
                            cameraState.center,
                            cameraState.zoom - 1f
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(IloveViSpacing.space3),
            )

            if (state.isEmpty) {
                MapMessage(
                    title = stringResource(Res.string.map_empty_title),
                    hint = stringResource(Res.string.map_empty_hint),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * The selected pin's bottom sheet (MAP-04): the compact summary + "View details" sits within
 * [SheetPeekHeight], always visible; the entry's description is appended below that, revealed
 * only once the user drags the sheet open past its peek. [Entry] isn't part of [SelectedPinSummary]
 * (that type is built from [com.pamaz.vi.core.data.repository.MapRepository]'s own pin+name join,
 * which never needed the full catalog row before) — fetched here instead, directly from
 * [CatalogRepository] via Koin, the same offline-first repository `:feature:catalog`'s own Detail
 * screen reads from. `null` while it's still loading (or if the entry genuinely doesn't exist)
 * simply means the description section doesn't render yet, no separate loading state needed since
 * the peek content above is already complete and usable without it.
 */
@Composable
private fun MapPinBottomSheetContent(
    summary: SelectedPinSummary,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val category = summary.category.toIloveViCategory()
    val subtitle = summary.zone?.let { zone ->
        stringResource(
            Res.string.map_pin_category_zone,
            stringResource(categoryLabelRes(summary.category)),
            zone
        )
    } ?: stringResource(categoryLabelRes(summary.category))

    val catalogRepository = koinInject<CatalogRepository>()
    val entry by produceState<Entry?>(initialValue = null, summary.category, summary.entryId) {
        catalogRepository.entry(summary.category, summary.entryId).collect { value = it }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = IloveViSpacing.space5, vertical = IloveViSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(IloveViSpacing.space4),
    ) {
        IloveViCompactCard(
            title = summary.name,
            subtitle = subtitle,
            category = category,
            icon = { tint ->
                IloveViIcon(
                    icon = summary.category.icon(),
                    tint = tint,
                    size = 24.dp
                )
            },
        )
        IloveViButton(
            text = stringResource(Res.string.map_view_details),
            onClick = onViewDetails,
            modifier = Modifier.testTag("map-view-details-button"),
        )
        entry?.let { loadedEntry ->
            Text(
                text = loadedEntry.description,
                color = IloveViColors.inkMuted,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun EntryRef.rawEntryId(): String = when (this) {
    is EntryRef.ToWeapon -> id.value
    is EntryRef.ToVehicle -> id.value
    is EntryRef.ToItem -> id.value
}

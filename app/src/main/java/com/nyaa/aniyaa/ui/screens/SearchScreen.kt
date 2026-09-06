package com.nyaa.aniyaa.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nyaa.aniyaa.data.api.resolvedMagnet
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.Category
import com.nyaa.aniyaa.data.model.FilterOption
import com.nyaa.aniyaa.data.model.SearchHistoryEntry
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.prefs.AppPreferences
import com.nyaa.aniyaa.ui.theme.NyaaLeecher
import com.nyaa.aniyaa.ui.theme.NyaaRemake
import com.nyaa.aniyaa.ui.theme.NyaaSeeder
import com.nyaa.aniyaa.ui.theme.NyaaTrusted
import com.nyaa.aniyaa.ui.viewmodel.BookmarkViewModel
import com.nyaa.aniyaa.ui.viewmodel.SearchHistoryViewModel
import com.nyaa.aniyaa.ui.viewmodel.SearchUiState
import com.nyaa.aniyaa.ui.viewmodel.SearchViewModel
import com.nyaa.aniyaa.util.PubDateFormatter
import com.nyaa.aniyaa.util.formatCount
import com.nyaa.aniyaa.util.hasNotificationPermission
import com.nyaa.aniyaa.util.openMagnet
import com.nyaa.aniyaa.util.parseReleaseTitle
import com.nyaa.aniyaa.util.prepareSavedSearchAlerts
import kotlinx.coroutines.launch

private const val LOAD_MORE_BUFFER = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onTorrentClick: (Torrent) -> Unit,
    searchViewModel: SearchViewModel = viewModel(),
    searchHistoryViewModel: SearchHistoryViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    bottomPadding: Dp = 0.dp
) {
    val viewModel = searchViewModel
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by searchHistoryViewModel.history.collectAsStateWithLifecycle()
    val bookmarks by bookmarkViewModel.allBookmarks.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingSave
        pendingSave = null
        if (pending != null) {
            if (granted) prepareSavedSearchAlerts(context)
            scope.launch {
                viewModel.saveCurrentSearch(pending.first, granted && pending.second)
                snackbarHostState.showSnackbar("Search saved")
            }
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val interactionSource = remember { MutableInteractionSource() }
    val searchFocused by interactionSource.collectIsFocusedAsState()
    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.bookmarkKey() }.toSet() }
    var showSukebeiWarning by remember { mutableStateOf(false) }
    var chromeVisible by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableIntStateOf(0) }
    var previousOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index == 0 && offset < 12) {
                    chromeVisible = true
                } else if (index > previousIndex || (index == previousIndex && offset > previousOffset + 12)) {
                    chromeVisible = false
                } else if (index < previousIndex || (index == previousIndex && offset < previousOffset - 12)) {
                    chromeVisible = true
                }
                previousIndex = index
                previousOffset = offset
            }
    }

    LaunchedEffect(uiState.error, uiState.torrents.isNotEmpty()) {
        val error = uiState.error
        if (error != null && uiState.torrents.isNotEmpty()) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.searchParams.page == 1 && uiState.torrents.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    val suggestions = remember(query, history, searchFocused) {
        if (!searchFocused) emptyList()
        else history.filter {
            it.site == uiState.searchParams.site && it.query.contains(query.trim(), ignoreCase = true)
        }.take(6)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::updateQuery,
                        placeholder = {
                            Text(
                                "Search…",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        interactionSource = interactionSource,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                            viewModel.search()
                        }),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateQuery("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (uiState.searchParams.hasActiveFilters()) {
                                        Badge()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                if (prefs.sukebeiEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CatalogSite.entries.forEach { site ->
                            FilterChip(
                                selected = uiState.searchParams.site == site,
                                onClick = {
                                    if (site.nsfw && !prefs.sukebeiAcknowledged) {
                                        showSukebeiWarning = true
                                    } else {
                                        viewModel.switchSite(site)
                                    }
                                },
                                label = { Text(if (site.nsfw) "${site.displayName} 18+" else site.displayName) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (suggestions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    suggestions.forEach { entry ->
                        Text(
                            text = entry.query,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    keyboardController?.hide()
                                    viewModel.applyHistory(entry)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            val siteHistory = history.filter { it.site == uiState.searchParams.site && it.query.isNotBlank() }
            val filterCaption = uiState.searchParams.activeFilterCaption()
            AnimatedVisibility(
                visible = chromeVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    val primaryCategories = uiState.searchParams.site.primaryCategories
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(primaryCategories, key = { it.value }) { category ->
                            FilterChip(
                                selected = category.groups(uiState.searchParams.category),
                                onClick = {
                                    viewModel.updateCategory(category)
                                    viewModel.search()
                                },
                                label = { Text(category.shortLabel) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = uiState.searchParams.filter == FilterOption.TRUSTED,
                                onClick = {
                                    viewModel.updateFilter(
                                        if (uiState.searchParams.filter == FilterOption.TRUSTED) {
                                            FilterOption.ALL
                                        } else {
                                            FilterOption.TRUSTED
                                        }
                                    )
                                    viewModel.search()
                                },
                                label = { Text("Trusted") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        item {
                            FilterChip(
                                selected = uiState.searchParams.filter == FilterOption.NO_REMAKES,
                                onClick = {
                                    viewModel.updateFilter(
                                        if (uiState.searchParams.filter == FilterOption.NO_REMAKES) {
                                            FilterOption.ALL
                                        } else {
                                            FilterOption.NO_REMAKES
                                        }
                                    )
                                    viewModel.search()
                                },
                                label = { Text("No remakes") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                    if (filterCaption.isNotBlank()) {
                        Text(
                            text = filterCaption,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                        )
                    }
                    if (siteHistory.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(siteHistory.take(12), key = { "${it.site.id}|${it.query}|${it.timestamp}" }) { entry ->
                                FilterChip(
                                    selected = false,
                                    onClick = { viewModel.applyHistory(entry) },
                                    label = {
                                        Text(
                                            entry.query,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 180.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            SearchResultsBody(
                uiState = uiState,
                listState = listState,
                bottomPadding = bottomPadding,
                bookmarkedIds = bookmarkedIds,
                onTorrentClick = onTorrentClick,
                onRetry = { viewModel.search(forceNetwork = true) },
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadNextPage,
                onMagnet = { torrent ->
                    val error = openMagnet(context, torrent.resolvedMagnet(), prefs.preferredTorrentPackage)
                    if (error != null) scope.launch { snackbarHostState.showSnackbar(error) }
                },
                onToggleBookmark = { torrent -> bookmarkViewModel.toggleBookmark(torrent) },
                onSearchQuery = { text ->
                    viewModel.applyParams(SearchParams(query = text, site = uiState.searchParams.site))
                },
                onOpenUser = { username ->
                    viewModel.applyParams(SearchParams(query = "user:$username", site = uiState.searchParams.site))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            FilterBottomSheetContent(
                searchParams = uiState.searchParams,
                categories = uiState.searchParams.site.categories,
                defaultCategory = prefs.defaultCategory(uiState.searchParams.site),
                defaultSortField = com.nyaa.aniyaa.data.model.sortFieldByValue(prefs.defaultSortFieldValue(uiState.searchParams.site)),
                defaultSortOrder = com.nyaa.aniyaa.data.model.sortOrderByValue(prefs.defaultSortOrderValue(uiState.searchParams.site)),
                onCategoryChange = viewModel::updateCategory,
                onFilterChange = viewModel::updateFilter,
                onSortFieldChange = viewModel::updateSortField,
                onSortOrderChange = viewModel::updateSortOrder,
                onReset = viewModel::resetFilters,
                onSaveSearch = {
                    showFilterSheet = false
                    showSaveDialog = true
                },
                onApply = {
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion { showFilterSheet = false }
                    viewModel.search()
                }
            )
        }
    }

    if (showSukebeiWarning) {
        AlertDialog(
            onDismissRequest = { showSukebeiWarning = false },
            title = { Text("Sukebei is 18+") },
            text = {
                Text(
                    "Sukebei lists adult content. You must be 18 or older to continue. " +
                        "You can switch back to Nyaa at any time."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefs.sukebeiEnabled = true
                        showSukebeiWarning = false
                        viewModel.switchSite(CatalogSite.SUKEBEI)
                    }
                ) { Text("I am 18+") }
            },
            dismissButton = {
                TextButton(onClick = { showSukebeiWarning = false }) { Text("Cancel") }
            }
        )
    }

    if (showSaveDialog) {
        SaveSearchDialog(
            defaultName = query.ifBlank { "Latest listings" },
            onDismiss = { showSaveDialog = false },
            onSave = { name, notify ->
                showSaveDialog = false
                if (notify && !hasNotificationPermission(context) && Build.VERSION.SDK_INT >= 33) {
                    pendingSave = name to true
                    notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    if (notify) prepareSavedSearchAlerts(context)
                    scope.launch {
                        viewModel.saveCurrentSearch(name, notify)
                        snackbarHostState.showSnackbar("Search saved")
                    }
                }
            }
        )
    }
}

@Composable
private fun SaveSearchDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    var notify by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save this search") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                FilterChip(
                    selected = notify,
                    onClick = { notify = !notify },
                    label = { Text("Notify me of new results") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, notify) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultsBody(
    uiState: SearchUiState,
    listState: LazyListState,
    bottomPadding: Dp,
    bookmarkedIds: Set<String>,
    onTorrentClick: (Torrent) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onMagnet: (Torrent) -> Unit,
    onToggleBookmark: (Torrent) -> Unit,
    onSearchQuery: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading && uiState.torrents.isEmpty() -> {
                SearchLoadingPlaceholder(bottomPadding = bottomPadding)
            }
            !uiState.hasSearched && uiState.torrents.isEmpty() -> {
                SearchHomeEmpty(
                    siteName = uiState.searchParams.site.displayName,
                    bottomPadding = bottomPadding,
                    onShowLatest = onRetry
                )
            }
            uiState.error != null && uiState.torrents.isEmpty() && uiState.hasSearched -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding)
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Search failed",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Retry") }
                }
            }
            uiState.torrents.isEmpty() && uiState.hasSearched -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding)
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No results found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Try different search terms or filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            else -> {
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val totalItems = listState.layoutInfo.totalItemsCount
                        totalItems > 0 && lastVisibleItem >= totalItems - LOAD_MORE_BUFFER
                    }
                }
                LaunchedEffect(shouldLoadMore, uiState.isLoadingMore, uiState.canLoadMore, uiState.isLoading) {
                    if (shouldLoadMore && !uiState.isLoading && !uiState.isLoadingMore && uiState.canLoadMore) {
                        onLoadMore()
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp + bottomPadding
                    )
                ) {
                    itemsIndexed(
                        items = uiState.torrents,
                        key = { index, torrent -> torrent.listKey(index) },
                        contentType = { _, _ -> "torrent" }
                    ) { _, torrent ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                when (value) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        onMagnet(torrent)
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        onToggleBookmark(torrent)
                                        false
                                    }
                                    else -> false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                val alignment = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    else -> Alignment.CenterEnd
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = alignment
                                ) {
                                    Surface(shape = CircleShape, color = color) {
                                        Icon(
                                            imageVector = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                                Icons.Default.Link
                                            } else {
                                                Icons.Default.Bookmark
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.padding(12.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        ) {
                            TorrentCard(
                                torrent = torrent,
                                onClick = onTorrentClick,
                                isBookmarked = torrent.bookmarkKey() in bookmarkedIds,
                                showSiteBadge = false,
                                onToggleBookmark = { onToggleBookmark(torrent) },
                                onSearchQuery = onSearchQuery,
                                onOpenUser = onOpenUser
                            )
                        }
                    }
                    if (uiState.isLoadingMore) {
                        item(key = "loading-more", contentType = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheetContent(
    searchParams: SearchParams,
    categories: List<Category> = searchParams.site.categories,
    defaultCategory: Category = categories.first(),
    defaultSortField: SortField = SortField.DATE,
    defaultSortOrder: SortOrder = SortOrder.DESC,
    onCategoryChange: (Category) -> Unit,
    onFilterChange: (FilterOption) -> Unit,
    onSortFieldChange: (SortField) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onReset: () -> Unit,
    onSaveSearch: () -> Unit = {},
    onApply: () -> Unit
) {
    var tempCategory by remember(key1 = searchParams) { mutableStateOf(searchParams.category) }
    var tempFilter by remember(key1 = searchParams) { mutableStateOf(searchParams.filter) }
    var tempSortField by remember(key1 = searchParams) { mutableStateOf(searchParams.sortField) }
    var tempSortOrder by remember(key1 = searchParams) { mutableStateOf(searchParams.sortOrder) }
    var categoryExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Search Filters",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
            text = "CATEGORY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.filter { it.isPrimary }.forEach { category ->
                FilterChip(
                    selected = category.groups(tempCategory),
                    onClick = { tempCategory = category },
                    label = { Text(category.shortLabel) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            val moreCategories = categories.filter { !it.isPrimary }
            if (moreCategories.isNotEmpty()) {
                Box {
                    FilterChip(
                        selected = moreCategories.any { it.value == tempCategory.value },
                        onClick = { categoryExpanded = true },
                        label = { Text("More") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        moreCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.displayName) },
                                onClick = {
                                    tempCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "FILTER",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterOption.entries.forEach { option ->
                FilterChip(
                    selected = tempFilter == option,
                    onClick = { tempFilter = option },
                    label = { Text(option.displayName) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "SORT BY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SortField.entries.forEach { field ->
                FilterChip(
                    selected = tempSortField == field,
                    onClick = { tempSortField = field },
                    label = { Text(field.displayName) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "ORDER",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortOrder.entries.forEach { order ->
                FilterChip(
                    selected = tempSortOrder == order,
                    onClick = { tempSortOrder = order },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (order == SortOrder.DESC) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(order.displayName)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onSaveSearch) { Text("Save this search") }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(
                onClick = {
                    tempCategory = defaultCategory
                    tempFilter = FilterOption.ALL
                    tempSortField = defaultSortField
                    tempSortOrder = defaultSortOrder
                    onReset()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Reset", fontWeight = FontWeight.SemiBold) }
            Button(
                onClick = {
                    onCategoryChange(tempCategory)
                    onFilterChange(tempFilter)
                    onSortFieldChange(tempSortField)
                    onSortOrderChange(tempSortOrder)
                    onApply()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Apply & Search", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun SearchHomeEmpty(
    siteName: String,
    bottomPadding: Dp,
    onShowLatest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Search $siteName",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Type a name, or show the latest listings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onShowLatest, shape = RoundedCornerShape(12.dp)) {
            Text("Show latest")
        }
    }
}

@Composable
private fun SearchLoadingPlaceholder(bottomPadding: Dp) {
    val pulse = rememberInfiniteTransition(label = "search-skeleton")
    val alpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "search-skeleton-alpha"
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 8.dp + bottomPadding
        ),
        userScrollEnabled = false
    ) {
        items(8) {
            SearchSkeletonCard(alpha = alpha)
        }
    }
}

@Composable
private fun SearchSkeletonCard(alpha: Float) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f + 0.06f * alpha)
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.64f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                )
                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TorrentCard(
    torrent: Torrent,
    onClick: (Torrent) -> Unit,
    isBookmarked: Boolean = false,
    showSiteBadge: Boolean = false,
    onMagnet: (() -> Unit)? = null,
    onCopyMagnet: (() -> Unit)? = null,
    onToggleBookmark: (() -> Unit)? = null,
    onSearchQuery: ((String) -> Unit)? = null,
    onOpenUser: ((String) -> Unit)? = null
) {
    Card(
        onClick = { onClick(torrent) },
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = torrent.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )
            val parsed = remember(torrent.title) { parseReleaseTitle(torrent.title) }
            val group = parsed.group
            val show = parsed.show
            if (onSearchQuery != null && (group != null || show != null) ||
                (onOpenUser != null && torrent.submitter.isNotBlank())
            ) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onSearchQuery != null && group != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { onSearchQuery(group) }
                        ) {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (onSearchQuery != null && show != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { onSearchQuery(show) }
                        ) {
                            Text(
                                text = show,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (onOpenUser != null && torrent.submitter.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.clickable { onOpenUser(torrent.submitter) }
                        ) {
                            Text(
                                text = torrent.submitter,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (showSiteBadge) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(
                            text = torrent.site.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (torrent.category.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            text = torrent.category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (torrent.trusted) {
                    Surface(shape = RoundedCornerShape(8.dp), color = NyaaTrusted.copy(alpha = 0.12f)) {
                        Text(
                            text = "✓ Trusted",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = NyaaTrusted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (torrent.remake) {
                    Surface(shape = RoundedCornerShape(8.dp), color = NyaaRemake.copy(alpha = 0.12f)) {
                        Text(
                            text = "⚠ Remake",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = NyaaRemake,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(
                        text = torrent.size,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Seeders", modifier = Modifier.size(13.dp), tint = NyaaSeeder)
                    Text(text = formatCount(torrent.seeders), style = MaterialTheme.typography.labelMedium, color = NyaaSeeder, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Leechers", modifier = Modifier.size(13.dp), tint = NyaaLeecher)
                    Text(text = formatCount(torrent.leechers), style = MaterialTheme.typography.labelMedium, color = NyaaLeecher, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.Download, contentDescription = "Downloads", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = formatCount(torrent.downloads), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = remember(torrent.pubDate) { PubDateFormatter.formatRelative(torrent.pubDate) },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (onMagnet != null || onCopyMagnet != null || onToggleBookmark != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMagnet != null) {
                        IconButton(onClick = onMagnet, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = "Open magnet",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (onCopyMagnet != null) {
                        IconButton(onClick = onCopyMagnet, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy magnet",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (onToggleBookmark != null) {
                        IconButton(onClick = onToggleBookmark, modifier = Modifier.size(40.dp)) {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
                                modifier = Modifier.size(20.dp),
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

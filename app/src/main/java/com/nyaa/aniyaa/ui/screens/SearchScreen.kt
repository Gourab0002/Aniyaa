package com.nyaa.aniyaa.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nyaa.aniyaa.data.model.CATEGORIES
import com.nyaa.aniyaa.data.model.FilterOption
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.ui.theme.NyaaLeecher
import com.nyaa.aniyaa.ui.theme.NyaaRemake
import com.nyaa.aniyaa.ui.theme.NyaaSeeder
import com.nyaa.aniyaa.ui.theme.NyaaTrusted
import com.nyaa.aniyaa.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onTorrentClick: (Torrent) -> Unit,
    viewModel: SearchViewModel = viewModel(),
    bottomPadding: Dp = 0.dp
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val keyboardController = LocalSoftwareKeyboardController.current
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = viewModel::updateQuery,
                        placeholder = { Text("Search torrents...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                            viewModel.search()
                        }),
                        trailingIcon = {
                            if (uiState.query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.torrents.isEmpty() && uiState.hasSearched -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                !uiState.hasSearched -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Search nyaa.si",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Find anime, manga, music and more",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            top = 8.dp,
                            bottom = 8.dp + bottomPadding
                        )
                    ) {
                        items(uiState.torrents, key = { "${it.id}|${it.infoHash}" }) { torrent ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 }
                            ) {
                                TorrentCard(torrent = torrent, onClick = { onTorrentClick(torrent) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            FilterBottomSheetContent(
                searchParams = uiState.searchParams,
                onCategoryChange = viewModel::updateCategory,
                onFilterChange = viewModel::updateFilter,
                onSortFieldChange = viewModel::updateSortField,
                onSortOrderChange = viewModel::updateSortOrder,
                onReset = viewModel::resetFilters,
                onApply = {
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion { showFilterSheet = false }
                    viewModel.search()
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheetContent(
    searchParams: com.nyaa.aniyaa.data.model.SearchParams,
    onCategoryChange: (com.nyaa.aniyaa.data.model.Category) -> Unit,
    onFilterChange: (FilterOption) -> Unit,
    onSortFieldChange: (SortField) -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onReset: () -> Unit,
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
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Search Filters",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Category
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box {
            OutlinedTextField(
                value = tempCategory.displayName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { categoryExpanded = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { categoryExpanded = true }
            )
            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                CATEGORIES.forEach { category ->
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

        Spacer(Modifier.height(16.dp))

        // Filter
        Text(
            text = "Filter",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterOption.entries.forEach { option ->
                FilterChip(
                    selected = tempFilter == option,
                    onClick = { tempFilter = option },
                    label = { Text(option.displayName) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Sort by
        Text(
            text = "Sort By",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortField.entries.forEach { field ->
                FilterChip(
                    selected = tempSortField == field,
                    onClick = { tempSortField = field },
                    label = { Text(field.displayName) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Sort order
        Text(
            text = "Order",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
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
                    }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = {
                    tempCategory = CATEGORIES[0]
                    tempFilter = FilterOption.ALL
                    tempSortField = SortField.DATE
                    tempSortOrder = SortOrder.DESC
                    onReset()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset")
            }
            Button(
                onClick = {
                    onCategoryChange(tempCategory)
                    onFilterChange(tempFilter)
                    onSortFieldChange(tempSortField)
                    onSortOrderChange(tempSortOrder)
                    onApply()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply & Search")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TorrentCard(torrent: Torrent, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Title
            Text(
                text = torrent.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            // Badges row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Category chip
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                ) {
                    Text(
                        text = torrent.category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (torrent.trusted) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NyaaTrusted.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "✓ Trusted",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = NyaaTrusted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (torrent.remake) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NyaaRemake.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "⚠ Remake",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = NyaaRemake,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Size
                Text(
                    text = torrent.size,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Seeders
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Seeders",
                        modifier = Modifier.size(12.dp),
                        tint = NyaaSeeder
                    )
                    Text(
                        text = torrent.seeders.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = NyaaSeeder,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Leechers
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = "Leechers",
                        modifier = Modifier.size(12.dp),
                        tint = NyaaLeecher
                    )
                    Text(
                        text = torrent.leechers.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = NyaaLeecher,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Downloads
                Text(
                    text = "↓ ${torrent.downloads}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Date (shortened)
                Text(
                    text = torrent.pubDate.take(11),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

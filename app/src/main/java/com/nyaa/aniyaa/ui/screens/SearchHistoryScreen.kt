package com.nyaa.aniyaa.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nyaa.aniyaa.data.model.SavedSearch
import com.nyaa.aniyaa.data.model.SearchHistoryEntry
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.ui.viewmodel.SearchHistoryViewModel
import com.nyaa.aniyaa.util.hasNotificationPermission
import com.nyaa.aniyaa.util.prepareSavedSearchAlerts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHistoryScreen(
    onHistoryItemClick: (SearchHistoryEntry) -> Unit,
    onSavedSearchClick: (SavedSearch) -> Unit,
    onTorrentClick: (Torrent) -> Unit = {},
    searchHistoryViewModel: SearchHistoryViewModel = viewModel(),
    bottomPadding: Dp = 0.dp
) {
    val history by searchHistoryViewModel.history.collectAsStateWithLifecycle()
    val saved by searchHistoryViewModel.savedSearches.collectAsStateWithLifecycle()
    val viewed by searchHistoryViewModel.viewed.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingNotifyId by remember { mutableStateOf<Long?>(null) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var pendingDeleteSaved by remember { mutableStateOf<SavedSearch?>(null) }
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val id = pendingNotifyId
        pendingNotifyId = null
        if (granted && id != null) {
            prepareSavedSearchAlerts(context)
            saved.find { it.id == id }?.let { searchHistoryViewModel.toggleNotify(it) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text("Search History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { confirmClearHistory = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear all history",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (history.isEmpty() && saved.isEmpty() && viewed.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "No search history",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Your recent searches will appear here",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp + bottomPadding
                    )
                ) {
                    if (viewed.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recently viewed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(viewed.take(12), key = { "viewed-${it.bookmarkKey()}" }) { torrent ->
                            TorrentCard(
                                torrent = torrent,
                                onClick = onTorrentClick,
                                showSiteBadge = true
                            )
                        }
                    }
                    if (saved.isNotEmpty()) {
                        item {
                            Text(
                                text = "Saved searches",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(saved, key = { "saved-${it.id}" }) { search ->
                            SavedSearchCard(
                                search = search,
                                onClick = { onSavedSearchClick(search) },
                                onToggleNotify = {
                                    if (search.notify) {
                                        searchHistoryViewModel.toggleNotify(search)
                                    } else if (hasNotificationPermission(context)) {
                                        prepareSavedSearchAlerts(context)
                                        searchHistoryViewModel.toggleNotify(search)
                                    } else if (Build.VERSION.SDK_INT >= 33) {
                                        pendingNotifyId = search.id
                                        notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        prepareSavedSearchAlerts(context)
                                        searchHistoryViewModel.toggleNotify(search)
                                    }
                                },
                                onDelete = { pendingDeleteSaved = search }
                            )
                        }
                    }
                    if (history.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    items(history, key = { "${it.site.id}|${it.query}|${it.timestamp}" }) { entry ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    searchHistoryViewModel.removeEntry(entry)
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        modifier = Modifier.padding(end = 16.dp)
                                    ) {
                                        IconButton(onClick = { searchHistoryViewModel.removeEntry(entry) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove from history",
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            SearchHistoryCard(
                                entry = entry,
                                onClick = { onHistoryItemClick(entry) }
                            )
                        }
                    }
                }
            }
        }
    }
    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Clear search history?") },
            text = { Text("This removes recent searches. Saved searches are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    searchHistoryViewModel.clearHistory()
                    confirmClearHistory = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClearHistory = false }) { Text("Cancel") } }
        )
    }
    val deleteSaved = pendingDeleteSaved
    if (deleteSaved != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteSaved = null },
            title = { Text("Delete saved search?") },
            text = { Text("“${deleteSaved.displayName()}” will be removed, including alerts.") },
            confirmButton = {
                TextButton(onClick = {
                    searchHistoryViewModel.deleteSavedSearch(deleteSaved.id)
                    pendingDeleteSaved = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteSaved = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SavedSearchCard(
    search: SavedSearch,
    onClick: () -> Unit,
    onToggleNotify: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = search.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val summary = search.toSearchParams().let {
                    buildList {
                        add(search.site.displayName)
                        if (it.query.isNotBlank()) add(it.query)
                        if (it.category.value != "0_0") add(it.category.displayName)
                        if (it.filter.displayName != "No Filter") add(it.filter.displayName)
                    }.joinToString(" · ")
                }
                if (summary.isNotBlank()) {
                    Text(text = summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = onToggleNotify) {
                Icon(
                    if (search.notify) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = if (search.notify) "Disable alerts" else "Enable alerts",
                    tint = if (search.notify) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete saved search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SearchHistoryCard(
    entry: SearchHistoryEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.query,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val extra = listOfNotNull(formatTimestamp(entry.timestamp), entry.filterSummary().ifBlank { null })
                    .joinToString(" · ")
                Text(text = extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Search, contentDescription = "Search again", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val historyDateFormatter: SimpleDateFormat by lazy {
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 0 -> historyDateFormatter.format(Date(timestamp))
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> historyDateFormatter.format(Date(timestamp))
    }
}

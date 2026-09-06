package com.nyaa.aniyaa.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nyaa.aniyaa.BuildConfig
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.DarkMode
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.ui.theme.APP_THEMES
import com.nyaa.aniyaa.ui.viewmodel.SettingsViewModel
import com.nyaa.aniyaa.util.listTorrentApps
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentThemeIndex: Int,
    darkMode: DarkMode,
    onThemeSelected: (Int) -> Unit,
    onDarkModeSelected: (DarkMode) -> Unit,
    onPrivacyFlagsChanged: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
    bottomPadding: Dp = 0.dp
) {
    val context = LocalContext.current
    val prefs = settingsViewModel.prefs
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val message by settingsViewModel.message.collectAsStateWithLifecycle()
    val update by settingsViewModel.update.collectAsStateWithLifecycle()
    val checkingUpdate by settingsViewModel.checkingUpdate.collectAsStateWithLifecycle()

    var currentSite by remember { mutableStateOf(prefs.currentSite) }
    var nyaaBaseUrl by remember { mutableStateOf(prefs.baseUrl(CatalogSite.NYAA)) }
    var sukebeiBaseUrl by remember { mutableStateOf(prefs.baseUrl(CatalogSite.SUKEBEI)) }
    var showSukebeiWarning by remember { mutableStateOf(false) }
    var sukebeiEnabled by remember { mutableStateOf(prefs.sukebeiEnabled) }
    var lockEnabled by remember { mutableStateOf(prefs.lockEnabled) }
    var hideScreenshots by remember { mutableStateOf(prefs.hideScreenshots) }
    var hideFromRecents by remember { mutableStateOf(prefs.hideFromRecents) }
    var defaultCategory by remember { mutableStateOf(prefs.defaultCategoryValue(currentSite)) }
    var defaultSort by remember { mutableStateOf(prefs.defaultSortFieldValue(currentSite)) }
    var defaultOrder by remember { mutableStateOf(prefs.defaultSortOrderValue(currentSite)) }
    var torrentPackage by remember { mutableStateOf(prefs.preferredTorrentPackage) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = settingsViewModel.exportBackup()
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    snackbarHostState.showSnackbar("Backup exported")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(e.message ?: "Export failed")
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
                settingsViewModel.importBackup(json)
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar(e.message ?: "Import failed") }
            }
        }
    }

    LaunchedEffect(message) {
        val text = message
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            settingsViewModel.consumeMessage()
        }
    }

    LaunchedEffect(Unit) {
        val last = prefs.lastUpdateCheckAt
        if (System.currentTimeMillis() - last > 86_400_000L) {
            settingsViewModel.checkForUpdates(manual = false)
        }
    }

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = bottomPadding)
        ) {
            SectionLabel("Appearance")
            Text("Color theme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(12.dp))
            APP_THEMES.chunked(2).forEachIndexed { rowIndex, rowThemes ->
                if (rowIndex > 0) Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowThemes.forEachIndexed { columnIndex, theme ->
                        val index = rowIndex * 2 + columnIndex
                        ThemeCard(
                            themeName = theme.name,
                            primaryColor = theme.primary,
                            secondaryColor = theme.secondary,
                            tertiaryColor = theme.tertiary,
                            isSelected = currentThemeIndex == index,
                            onClick = {
                                onThemeSelected(index)
                                settingsViewModel.setThemeIndex(index)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowThemes.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Dark mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkMode.entries.forEach { mode ->
                    FilterChip(
                        selected = darkMode == mode,
                        onClick = {
                            onDarkModeSelected(mode)
                            settingsViewModel.setDarkMode(mode)
                        },
                        label = { Text(mode.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Catalog")
            Text("Nyaa is the default. Sukebei is optional adult content.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            SettingsSwitchRow("Enable Sukebei (18+)", "Show the Sukebei catalog on Search", sukebeiEnabled) { enabled ->
                if (enabled && !prefs.sukebeiAcknowledged) {
                    showSukebeiWarning = true
                } else {
                    sukebeiEnabled = enabled
                    settingsViewModel.setSukebeiEnabled(enabled)
                    if (!enabled) {
                        currentSite = CatalogSite.NYAA
                        defaultCategory = prefs.defaultCategoryValue(CatalogSite.NYAA)
                        defaultSort = prefs.defaultSortFieldValue(CatalogSite.NYAA)
                        defaultOrder = prefs.defaultSortOrderValue(CatalogSite.NYAA)
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Search defaults")
            Text("These defaults apply when you reset filters for ${currentSite.displayName}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            if (sukebeiEnabled) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CatalogSite.entries.forEach { site ->
                        FilterChip(
                            selected = currentSite == site,
                            onClick = {
                                currentSite = site
                                defaultCategory = prefs.defaultCategoryValue(site)
                                defaultSort = prefs.defaultSortFieldValue(site)
                                defaultOrder = prefs.defaultSortOrderValue(site)
                            },
                            label = { Text(if (site.nsfw) "${site.displayName} 18+" else site.displayName) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currentSite.categories.forEach { category ->
                    FilterChip(
                        selected = defaultCategory == category.value,
                        onClick = {
                            defaultCategory = category.value
                            settingsViewModel.setDefaultCategory(category.value, currentSite)
                        },
                        label = { Text(category.displayName) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SortField.entries.forEach { field ->
                    FilterChip(
                        selected = defaultSort == field.value,
                        onClick = {
                            defaultSort = field.value
                            settingsViewModel.setDefaultSortField(field.value, currentSite)
                        },
                        label = { Text(field.displayName) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = defaultOrder == order.value,
                        onClick = {
                            defaultOrder = order.value
                            settingsViewModel.setDefaultSortOrder(order.value, currentSite)
                        },
                        label = { Text(order.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Mirrors")
            Text("If a site is blocked, paste a working mirror for that catalog.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nyaaBaseUrl,
                onValueChange = { nyaaBaseUrl = it },
                label = { Text("Nyaa URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { settingsViewModel.setBaseUrl(CatalogSite.NYAA, nyaaBaseUrl) }) { Text("Save") }
                OutlinedButton(onClick = {
                    nyaaBaseUrl = CatalogSite.NYAA.defaultBase
                    settingsViewModel.setBaseUrl(CatalogSite.NYAA, CatalogSite.NYAA.defaultBase)
                }) { Text("Official") }
            }
            if (sukebeiEnabled) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = sukebeiBaseUrl,
                    onValueChange = { sukebeiBaseUrl = it },
                    label = { Text("Sukebei URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { settingsViewModel.setBaseUrl(CatalogSite.SUKEBEI, sukebeiBaseUrl) }) { Text("Save") }
                    OutlinedButton(onClick = {
                        sukebeiBaseUrl = CatalogSite.SUKEBEI.defaultBase
                        settingsViewModel.setBaseUrl(CatalogSite.SUKEBEI, CatalogSite.SUKEBEI.defaultBase)
                    }) { Text("Official") }
                }
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Privacy")
            SettingsSwitchRow("App lock", "Require a PIN when you return to the app", lockEnabled) { enabled ->
                if (enabled && !prefs.hasPin) {
                    showPinDialog = true
                } else {
                    lockEnabled = enabled
                    settingsViewModel.setLockEnabled(enabled)
                    onPrivacyFlagsChanged()
                }
            }
            TextButton(onClick = { showPinDialog = true }) { Text(if (prefs.hasPin) "Change PIN" else "Set PIN") }
            SettingsSwitchRow("Hide screenshots", "Block screenshots and recents previews", hideScreenshots) { enabled ->
                hideScreenshots = enabled
                settingsViewModel.setHideScreenshots(enabled)
                onPrivacyFlagsChanged()
            }
            SettingsSwitchRow("Hide from recents", "Remove Aniyaa from the recents list", hideFromRecents) { enabled ->
                hideFromRecents = enabled
                settingsViewModel.setHideFromRecents(enabled)
                onPrivacyFlagsChanged()
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Torrent app")
            val apps = remember { listTorrentApps(context) }
            FilterChip(
                selected = torrentPackage.isBlank(),
                onClick = {
                    torrentPackage = ""
                    settingsViewModel.setPreferredTorrentPackage("")
                },
                label = { Text("Always ask") }
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                apps.forEach { app ->
                    FilterChip(
                        selected = torrentPackage == app.packageName,
                        onClick = {
                            torrentPackage = app.packageName
                            settingsViewModel.setPreferredTorrentPackage(app.packageName)
                        },
                        label = { Text(app.label) }
                    )
                }
            }
            if (apps.isEmpty()) {
                Text("No magnet-handling apps found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Backup")
            Text("Export bookmarks, history, saved searches, and settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { exportLauncher.launch("aniyaa-backup.json") }) { Text("Export") }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) { Text("Import") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { settingsViewModel.clearNetworkCache() }) { Text("Clear cache") }
                OutlinedButton(onClick = { showResetDialog = true }) { Text("Reset local data") }
            }
            if (showResetDialog) {
                AlertDialog(
                    onDismissRequest = { showResetDialog = false },
                    title = { Text("Reset this device?") },
                    text = { Text("This removes bookmarks, search history, and saved searches on this phone. A backup is not created.") },
                    confirmButton = {
                        TextButton(onClick = {
                            settingsViewModel.resetLocalData()
                            showResetDialog = false
                        }) { Text("Reset") }
                    },
                    dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
                )
            }

            Spacer(Modifier.height(28.dp))
            SectionLabel("Updates")
            if (update != null) {
                Text("Version ${update?.versionName} is available.", color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { openUrl(update?.htmlUrl.orEmpty()) }) { Text("Download") }
                    TextButton(onClick = { settingsViewModel.dismissUpdate() }) { Text("Later") }
                }
            } else {
                Button(onClick = { settingsViewModel.checkForUpdates(true) }, enabled = !checkingUpdate) {
                    Text(if (checkingUpdate) "Checking…" else "Check for updates")
                }
            }

            Spacer(Modifier.height(32.dp))
            SectionLabel("About")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Aniyaa searches nyaa.si and sukebei.nyaa.si from your phone. It finds listings — a torrent app you already have does the downloading.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "This is not an official nyaa.si or sukebei.nyaa.si app. Bookmarks and search history stay on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))
                    InfoSettingsRow(label = "Version", value = BuildConfig.VERSION_NAME)
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(10.dp))
                    InfoSettingsRow(
                        label = "Catalog",
                        value = currentSite.displayName,
                        onClick = { openUrl(prefs.baseUrl(currentSite)) }
                    )
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(10.dp))
                    InfoSettingsRow(
                        label = "Updates",
                        value = "GitHub Releases",
                        onClick = { openUrl("https://github.com/Gourab0002/Aniyaa/releases/latest") }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSukebeiWarning) {
        AlertDialog(
            onDismissRequest = { showSukebeiWarning = false },
            title = { Text("Sukebei is 18+") },
            text = { Text("Sukebei lists adult content. You must be 18 or older to continue.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.acknowledgeSukebei()
                        sukebeiEnabled = true
                        currentSite = CatalogSite.SUKEBEI
                        defaultCategory = prefs.defaultCategoryValue(CatalogSite.SUKEBEI)
                        defaultSort = prefs.defaultSortFieldValue(CatalogSite.SUKEBEI)
                        defaultOrder = prefs.defaultSortOrderValue(CatalogSite.SUKEBEI)
                        showSukebeiWarning = false
                    }
                ) { Text("I am 18+") }
            },
            dismissButton = { TextButton(onClick = { showSukebeiWarning = false }) { Text("Cancel") } }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set PIN") },
            text = {
                OutlinedTextField(
                    value = pinValue,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pinValue = it },
                    label = { Text("4–8 digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = pinValue.length in 4..8,
                    onClick = {
                        settingsViewModel.setPin(pinValue)
                        lockEnabled = true
                        settingsViewModel.setLockEnabled(true)
                        onPrivacyFlagsChanged()
                        pinValue = ""
                        showPinDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Icon(
            if (text == "About") Icons.Default.Info else Icons.Default.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeCard(
    themeName: String,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "themeCardColor"
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(primaryColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(secondaryColor))
                Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(tertiaryColor))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = themeName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InfoSettingsRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

package com.nyaa.aniyaa

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.DarkMode
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.network.AppHttpClient
import com.nyaa.aniyaa.ui.lock.LockScreen
import com.nyaa.aniyaa.ui.screens.BookmarksScreen
import com.nyaa.aniyaa.ui.screens.SearchHistoryScreen
import com.nyaa.aniyaa.ui.screens.SearchScreen
import com.nyaa.aniyaa.ui.screens.SettingsScreen
import com.nyaa.aniyaa.ui.screens.TorrentDetailScreen
import com.nyaa.aniyaa.ui.theme.AniyaaTheme
import com.nyaa.aniyaa.ui.viewmodel.BookmarkViewModel
import com.nyaa.aniyaa.ui.viewmodel.SearchHistoryViewModel
import com.nyaa.aniyaa.ui.viewmodel.SearchViewModel
import com.nyaa.aniyaa.util.HighRefreshRate

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    private lateinit var app: AniyaaApplication
    private val incomingLink = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = application as AniyaaApplication
        AppHttpClient.configure(cacheDir)
        enableEdgeToEdge()
        HighRefreshRate.apply(this)
        applyPrivacyFlags()
        incomingLink.value = intent?.data
        app.lockController.prepare(app.prefs.lockEnabled, app.prefs.hasPin)
        setContent {
            val prefs = app.prefs
            var themeIndex by remember { mutableIntStateOf(prefs.themeIndex) }
            var darkMode by remember { mutableStateOf(prefs.darkMode) }
            val locked by app.lockController.locked.collectAsStateWithLifecycle()
            var pinError by remember { mutableStateOf<String?>(null) }
            val biometricAvailable = remember {
                BiometricManager.from(this).canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                ) == BiometricManager.BIOMETRIC_SUCCESS
            }

            AniyaaTheme(darkMode = darkMode, themeIndex = themeIndex) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AniyaaApp(
                        currentThemeIndex = themeIndex,
                        darkMode = darkMode,
                        pendingDeepLink = incomingLink.value,
                        onDeepLinkConsumed = { incomingLink.value = null },
                        onThemeSelected = { index ->
                            themeIndex = index
                            prefs.themeIndex = index
                        },
                        onDarkModeSelected = { mode ->
                            darkMode = mode
                            prefs.darkMode = mode
                        },
                        onPrivacyFlagsChanged = { applyPrivacyFlags() }
                    )
                    if (locked && prefs.lockEnabled && prefs.hasPin) {
                        LockScreen(
                            error = pinError,
                            biometricAvailable = biometricAvailable,
                            onUnlockWithPin = { pin ->
                                if (prefs.verifyPin(pin)) {
                                    pinError = null
                                    app.lockController.unlock()
                                } else {
                                    pinError = "Wrong PIN"
                                }
                            },
                            onUnlockWithBiometric = { promptBiometric { pinError = null } }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingLink.value = intent.data
    }

    override fun onResume() {
        super.onResume()
        HighRefreshRate.apply(this)
        applyPrivacyFlags()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) HighRefreshRate.apply(this)
    }

    private fun applyPrivacyFlags() {
        if (app.prefs.hideScreenshots) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        val am = getSystemService(ActivityManager::class.java)
        am?.appTasks?.forEach { it.setExcludeFromRecents(app.prefs.hideFromRecents) }
    }

    private fun promptBiometric(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    app.lockController.unlock()
                    onSuccess()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Aniyaa")
            .setNegativeButtonText("Use PIN")
            .build()
        prompt.authenticate(info)
    }
}

private val navFadeSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

private val navSlideSpring = spring<IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem("history", "History", Icons.Filled.History, Icons.Outlined.History),
    BottomNavItem("bookmarks", "Bookmarks", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    BottomNavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

private fun encodeNavId(id: String): String =
    URLEncoder.encode(id, StandardCharsets.UTF_8.toString())

private fun decodeNavId(id: String): String =
    URLDecoder.decode(id, StandardCharsets.UTF_8.toString())

@Composable
fun AniyaaApp(
    currentThemeIndex: Int,
    darkMode: DarkMode,
    pendingDeepLink: Uri?,
    onDeepLinkConsumed: () -> Unit,
    onThemeSelected: (Int) -> Unit,
    onDarkModeSelected: (DarkMode) -> Unit,
    onPrivacyFlagsChanged: () -> Unit
) {
    val navController = rememberNavController()
    var selectedTorrent by rememberSaveable { mutableStateOf<Torrent?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val searchHistoryViewModel: SearchHistoryViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val bookmarkViewModel: BookmarkViewModel = viewModel()
    val expanded = LocalConfiguration.current.screenWidthDp >= 700
    val showBottomBar = currentRoute?.startsWith("detail") != true

    fun openUser(username: String) {
        searchViewModel.applyParams(SearchParams(query = "user:$username"))
        selectedTorrent = null
        navController.navigate("search") {
            popUpTo("search") { inclusive = true }
            launchSingleTop = true
        }
    }

    val openTorrent = remember(navController, expanded) {
        { torrent: Torrent ->
            selectedTorrent = torrent
            if (!expanded) {
                navController.navigate("detail/${torrent.site.id}/${encodeNavId(torrent.navId())}")
            }
        }
    }

    LaunchedEffect(pendingDeepLink) {
        val data = pendingDeepLink ?: return@LaunchedEffect
        onDeepLinkConsumed()
        val host = data.host.orEmpty()
        val segments = data.pathSegments
        val linkedSite = CatalogSite.fromHost(host) ?: CatalogSite.fromUrl(data.toString())
        val viewId = when {
            host == "view" -> data.lastPathSegment
            segments.firstOrNull() == "view" -> segments.getOrNull(1)
            else -> null
        }?.substringBefore("#")
        val savedId = if (host == "saved") data.lastPathSegment?.toLongOrNull() else null
        if (viewId != null) {
            val site = linkedSite ?: CatalogSite.NYAA
            if (site.nsfw) {
                AniyaaApplication.instance.prefs.sukebeiAcknowledged = true
            }
            searchViewModel.switchSite(site)
            selectedTorrent = searchViewModel.torrentByNavId(viewId, site)
                ?: bookmarkViewModel.torrentByNavId(viewId, site)
            if (!expanded) {
                navController.navigate("detail/${site.id}/${encodeNavId(viewId)}")
            }
        } else if (savedId != null) {
            val saved = searchHistoryViewModel.savedSearchById(savedId)
            if (saved != null) {
                searchViewModel.applySavedSearch(saved)
                navController.navigate("search") {
                    popUpTo("search") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) { it },
                exit = slideOutVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) { it }
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = selected,
                            onClick = {
                                selectedTorrent = null
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo("search") { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "search",
                modifier = Modifier.weight(1f),
                enterTransition = {
                    fadeIn(navFadeSpring) + slideInHorizontally(navSlideSpring) { it / 8 }
                },
                exitTransition = { fadeOut(navFadeSpring) },
                popEnterTransition = {
                    fadeIn(navFadeSpring) + slideInHorizontally(navSlideSpring) { -it / 8 }
                },
                popExitTransition = {
                    fadeOut(navFadeSpring) + slideOutHorizontally(navSlideSpring) { it / 8 }
                }
            ) {
                composable("search") {
                    SearchScreen(
                        onTorrentClick = openTorrent,
                        bottomPadding = innerPadding.calculateBottomPadding(),
                        searchViewModel = searchViewModel,
                        searchHistoryViewModel = searchHistoryViewModel,
                        bookmarkViewModel = bookmarkViewModel
                    )
                }
                composable("history") {
                    SearchHistoryScreen(
                        onHistoryItemClick = { entry ->
                            searchViewModel.applyHistory(entry)
                            navController.navigate("search") {
                                popUpTo("search") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onSavedSearchClick = { saved ->
                            searchViewModel.applySavedSearch(saved)
                            navController.navigate("search") {
                                popUpTo("search") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        searchHistoryViewModel = searchHistoryViewModel,
                        bottomPadding = innerPadding.calculateBottomPadding()
                    )
                }
                composable("bookmarks") {
                    BookmarksScreen(
                        onTorrentClick = openTorrent,
                        bookmarkViewModel = bookmarkViewModel,
                        bottomPadding = innerPadding.calculateBottomPadding()
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        currentThemeIndex = currentThemeIndex,
                        darkMode = darkMode,
                        onThemeSelected = onThemeSelected,
                        onDarkModeSelected = onDarkModeSelected,
                        onPrivacyFlagsChanged = onPrivacyFlagsChanged,
                        bottomPadding = innerPadding.calculateBottomPadding()
                    )
                }
                composable(
                    route = "detail/{site}/{torrentId}",
                    arguments = listOf(
                        navArgument("site") { type = NavType.StringType },
                        navArgument("torrentId") { type = NavType.StringType }
                    )
                ) { entry ->
                    val navId = decodeNavId(entry.arguments?.getString("torrentId").orEmpty())
                    val site = CatalogSite.fromId(entry.arguments?.getString("site").orEmpty())
                    val cached = selectedTorrent?.takeIf { it.matchesNavId(navId) && it.site == site }
                        ?: searchViewModel.torrentByNavId(navId, site)
                        ?: bookmarkViewModel.torrentByNavId(navId, site)
                    TorrentDetailGate(
                        navId = navId,
                        site = site,
                        cached = cached,
                        onNavigateBack = { navController.navigateUp() },
                        onOpenUser = ::openUser,
                        bookmarkViewModel = bookmarkViewModel
                    )
                }
            }
            if (expanded && selectedTorrent != null && showBottomBar) {
                Box(modifier = Modifier.weight(1.15f).fillMaxWidth()) {
                    TorrentDetailScreen(
                        torrent = selectedTorrent!!,
                        onNavigateBack = { selectedTorrent = null },
                        onOpenUser = ::openUser,
                        bookmarkViewModel = bookmarkViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun TorrentDetailGate(
    navId: String,
    site: CatalogSite,
    cached: Torrent?,
    onNavigateBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    bookmarkViewModel: BookmarkViewModel
) {
    var torrent by remember(navId, site) { mutableStateOf(cached) }
    var loading by remember(navId, site) { mutableStateOf(cached == null && navId.isNotBlank() && navId != "unknown") }
    var failed by remember(navId, site) { mutableStateOf(false) }

    LaunchedEffect(navId, site, cached) {
        if (cached != null) {
            torrent = cached
            loading = false
            return@LaunchedEffect
        }
        if (navId.isBlank() || navId == "unknown") {
            failed = true
            loading = false
            return@LaunchedEffect
        }
        loading = true
        AniyaaApplication.instance.nyaaRepository.fetchTorrent(navId, site = site)
            .onSuccess {
                torrent = it
                loading = false
            }
            .onFailure {
                failed = true
                loading = false
            }
    }

    when {
        torrent != null -> TorrentDetailScreen(
            torrent = torrent!!,
            onNavigateBack = onNavigateBack,
            onOpenUser = onOpenUser,
            bookmarkViewModel = bookmarkViewModel
        )
        loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> MissingTorrentScreen(onNavigateBack = onNavigateBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissingTorrentScreen(onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "This torrent is no longer available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

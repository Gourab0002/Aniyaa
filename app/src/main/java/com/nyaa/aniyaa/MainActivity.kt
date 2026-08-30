package com.nyaa.aniyaa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.network.AppHttpClient
import com.nyaa.aniyaa.ui.screens.BookmarksScreen
import com.nyaa.aniyaa.ui.screens.SearchHistoryScreen
import com.nyaa.aniyaa.ui.screens.SearchScreen
import com.nyaa.aniyaa.ui.screens.SettingsScreen
import com.nyaa.aniyaa.ui.screens.TorrentDetailScreen
import com.nyaa.aniyaa.ui.theme.AniyaaTheme
import com.nyaa.aniyaa.ui.theme.ThemePreferences
import com.nyaa.aniyaa.ui.viewmodel.BookmarkViewModel
import com.nyaa.aniyaa.ui.viewmodel.SearchHistoryViewModel
import com.nyaa.aniyaa.ui.viewmodel.SearchViewModel
import com.nyaa.aniyaa.util.HighRefreshRate
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppHttpClient.configure(cacheDir)
        enableEdgeToEdge()
        HighRefreshRate.apply(this)
        setContent {
            val themePrefs = remember { ThemePreferences(this) }
            var themeIndex by remember { mutableIntStateOf(themePrefs.themeIndex) }

            AniyaaTheme(themeIndex = themeIndex) {
                AniyaaApp(
                    currentThemeIndex = themeIndex,
                    onThemeSelected = { index ->
                        themeIndex = index
                        themePrefs.themeIndex = index
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        HighRefreshRate.apply(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            HighRefreshRate.apply(this)
        }
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
    onThemeSelected: (Int) -> Unit
) {
    val navController = rememberNavController()
    var selectedTorrent by rememberSaveable { mutableStateOf<Torrent?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val searchHistoryViewModel: SearchHistoryViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val bookmarkViewModel: BookmarkViewModel = viewModel()

    val showBottomBar = currentRoute?.startsWith("detail") != true
    val openTorrent = remember(navController) {
        { torrent: Torrent ->
            selectedTorrent = torrent
            navController.navigate("detail/${encodeNavId(torrent.navId())}")
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
        NavHost(
            navController = navController,
            startDestination = "search",
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
                    searchHistoryViewModel = searchHistoryViewModel
                )
            }
            composable("history") {
                SearchHistoryScreen(
                    onHistoryItemClick = { query ->
                        searchViewModel.updateQuery(query)
                        searchViewModel.search()
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
                    onThemeSelected = onThemeSelected,
                    bottomPadding = innerPadding.calculateBottomPadding()
                )
            }
            composable(
                route = "detail/{torrentId}",
                arguments = listOf(navArgument("torrentId") { type = NavType.StringType })
            ) { entry ->
                val navId = decodeNavId(entry.arguments?.getString("torrentId").orEmpty())
                val torrent = selectedTorrent?.takeIf { it.matchesNavId(navId) }
                    ?: searchViewModel.torrentByNavId(navId)
                    ?: bookmarkViewModel.torrentByNavId(navId)
                if (torrent != null) {
                    TorrentDetailScreen(
                        torrent = torrent,
                        onNavigateBack = { navController.navigateUp() },
                        bookmarkViewModel = bookmarkViewModel
                    )
                } else {
                    MissingTorrentScreen(onNavigateBack = { navController.navigateUp() })
                }
            }
        }
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

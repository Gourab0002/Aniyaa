package com.nyaa.aniyaa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.ui.screens.BookmarksScreen
import com.nyaa.aniyaa.ui.screens.SearchScreen
import com.nyaa.aniyaa.ui.screens.SettingsScreen
import com.nyaa.aniyaa.ui.screens.TorrentDetailScreen
import com.nyaa.aniyaa.ui.theme.AniyaaTheme
import com.nyaa.aniyaa.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Request high refresh rate for 120Hz displays
        window.attributes = window.attributes.apply {
            preferredRefreshRate = 120f
        }
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
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("search", "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem("bookmarks", "Bookmarks", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    BottomNavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun AniyaaApp(
    currentThemeIndex: Int,
    onThemeSelected: (Int) -> Unit
) {
    val navController = rememberNavController()
    var selectedTorrent by rememberSaveable { mutableStateOf<Torrent?>(null) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute != "detail"

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
            enterTransition = { fadeIn(tween(200)) + slideInHorizontally(tween(250)) { it / 6 } },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) + slideInHorizontally(tween(250)) { -it / 6 } },
            popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(250)) { it / 6 } }
        ) {
            composable("search") {
                SearchScreen(
                    onTorrentClick = { torrent ->
                        selectedTorrent = torrent
                        navController.navigate("detail")
                    },
                    bottomPadding = innerPadding.calculateBottomPadding()
                )
            }
            composable("bookmarks") {
                BookmarksScreen(
                    onTorrentClick = { torrent ->
                        selectedTorrent = torrent
                        navController.navigate("detail")
                    },
                    bottomPadding = innerPadding.calculateBottomPadding()
                )
            }
            composable("settings") {
                SettingsScreen(
                    currentThemeIndex = currentThemeIndex,
                    onThemeSelected = onThemeSelected
                )
            }
            composable("detail") {
                selectedTorrent?.let { torrent ->
                    TorrentDetailScreen(
                        torrent = torrent,
                        onNavigateBack = { navController.navigateUp() }
                    )
                }
            }
        }
    }
}

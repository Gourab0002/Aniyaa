package com.nyaa.aniyaa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.ui.screens.SearchScreen
import com.nyaa.aniyaa.ui.screens.TorrentDetailScreen
import com.nyaa.aniyaa.ui.theme.AniyaaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AniyaaTheme {
                AniyaaApp()
            }
        }
    }
}

@Composable
fun AniyaaApp() {
    val navController = rememberNavController()
    var selectedTorrent by remember { mutableStateOf<Torrent?>(null) }

    NavHost(navController = navController, startDestination = "search") {
        composable("search") {
            SearchScreen(
                onTorrentClick = { torrent ->
                    selectedTorrent = torrent
                    navController.navigate("detail")
                }
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

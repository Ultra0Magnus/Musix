package com.louis.musix.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.louis.musix.data.SelectedSongHolder
import com.louis.musix.ui.screens.home.HomeScreen
import com.louis.musix.ui.screens.library.LibraryScreen
import com.louis.musix.ui.screens.player.PlayerScreen
import com.louis.musix.ui.screens.search.SearchScreen
import org.koin.compose.koinInject

@Composable
fun MusixNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    // Holder injecté par Koin — permet de passer une Song entre écrans
    val songHolder: SelectedSongHolder = koinInject()

    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
        modifier = modifier,
    ) {
        composable(Routes.Home.route) { HomeScreen() }

        composable(Routes.Search.route) {
            SearchScreen(
                onSongClick = { song ->
                    // 1. Stocker la chanson sélectionnée
                    songHolder.current = song
                    // 2. Naviguer vers le player
                    navController.navigate(Routes.Player.route)
                }
            )
        }

        composable(Routes.Library.route) { LibraryScreen() }

        composable(Routes.Player.route) {
            PlayerScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

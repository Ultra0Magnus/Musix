package com.louis.musix.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.louis.musix.data.SelectedSongHolder
import com.louis.musix.domain.model.Song
import com.louis.musix.ui.screens.home.HomeScreen
import com.louis.musix.ui.screens.library.LibraryScreen
import com.louis.musix.ui.screens.player.PlayerScreen
import com.louis.musix.ui.screens.playlist.PlaylistDetailScreen
import com.louis.musix.ui.screens.search.SearchScreen
import com.louis.musix.ui.screens.spotify.SpotifyImportScreen
import org.koin.compose.koinInject

@Composable
fun MusixNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val songHolder: SelectedSongHolder = koinInject()

    /** Navigue vers le Player avec la chanson selectionnee. */
    fun playSong(song: Song) {
        songHolder.current = song
        navController.navigate(Routes.Player.route)
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
        modifier = modifier,
    ) {
        composable(Routes.Home.route) {
            HomeScreen(onSongClick = { song -> playSong(song) })
        }

        composable(Routes.Search.route) {
            SearchScreen(
                onSongClick = { song -> playSong(song) }
            )
        }

        composable(Routes.Library.route) {
            LibraryScreen(
                onPlaylistClick = { playlistId ->
                    navController.navigate(Routes.PlaylistDetail.createRoute(playlistId))
                },
                onSongClick = { song -> playSong(song) },
                onSpotifyImportClick = {
                    navController.navigate(Routes.SpotifyImport.route)
                },
            )
        }

        composable(Routes.Player.route) {
            PlayerScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SpotifyImport.route) {
            SpotifyImportScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Routes.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: return@composable
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBack = { navController.popBackStack() },
                onSongClick = { song -> playSong(song) },
            )
        }
    }
}

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
import com.louis.musix.ui.screens.artist.AlbumDetailScreen
import com.louis.musix.ui.screens.artist.ArtistScreen
import com.louis.musix.ui.screens.home.HomeScreen
import com.louis.musix.ui.screens.library.LibraryScreen
import com.louis.musix.ui.screens.player.PlayerScreen
import com.louis.musix.ui.screens.playlist.PlaylistDetailScreen
import com.louis.musix.ui.screens.search.SearchScreen
import com.louis.musix.ui.screens.settings.SettingsScreen
import com.louis.musix.ui.screens.spotify.SpotifyImportScreen
import org.koin.compose.koinInject

@Composable
fun MusixNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val songHolder: SelectedSongHolder = koinInject()

    /** Navigates to the Player with a standalone song (artist auto-queue). */
    fun playSong(song: Song) {
        songHolder.current = song
        navController.navigate(Routes.Player.route)
    }

    /**
     * Navigates to the Player loading an entire list as the playback queue.
     * Used from playlists and albums — does NOT trigger artist auto-queue.
     */
    fun playFromList(songs: List<Song>, startIndex: Int) {
        val song = songs.getOrNull(startIndex) ?: return
        songHolder.current           = song
        songHolder.pendingQueue      = songs
        songHolder.pendingQueueIndex = startIndex
        navController.navigate(Routes.Player.route)
    }

    /** Navigates to the artist page. */
    fun openArtist(name: String) {
        navController.navigate(Routes.Artist.createRoute(name))
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Home.route,
        modifier = modifier,
    ) {
        composable(Routes.Home.route) {
            HomeScreen(
                onSongClick = { song -> playSong(song) },
                onSettingsClick = { navController.navigate(Routes.Settings.route) }
            )
        }

        composable(Routes.Search.route) {
            SearchScreen(
                onSongClick   = { song -> playSong(song) },
                onArtistClick = { name -> openArtist(name) },
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
            PlayerScreen(
                onBack        = { navController.popBackStack() },
                onArtistClick = { name -> openArtist(name) },
            )
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
                playlistId  = playlistId,
                onBack      = { navController.popBackStack() },
                onSongClick = { songs, index -> playFromList(songs, index) },
            )
        }

        composable(
            route = Routes.Artist.route,
            arguments = listOf(navArgument("name") {
                type = NavType.StringType
                defaultValue = ""
            }),
        ) { backStackEntry ->
            val artistName = backStackEntry.arguments?.getString("name") ?: ""
            ArtistScreen(
                artistName   = artistName,
                onSongClick  = { songs, index -> playFromList(songs, index) },
                onAlbumClick = { album ->
                    navController.navigate(
                        Routes.AlbumDetail.createRoute(album.name, album.playlistUrl)
                    )
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.AlbumDetail.route,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType; defaultValue = "" },
                navArgument("url")  { type = NavType.StringType; defaultValue = "" },
            ),
        ) { backStackEntry ->
            val albumName   = backStackEntry.arguments?.getString("name") ?: ""
            val playlistUrl = backStackEntry.arguments?.getString("url")  ?: ""
            AlbumDetailScreen(
                albumName   = albumName,
                playlistUrl = playlistUrl,
                onSongClick = { songs, index -> playFromList(songs, index) },
                onBack      = { navController.popBackStack() },
            )
        }
        composable(Routes.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

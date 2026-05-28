package com.louis.musix.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Routes(val route: String) {
    data object Home    : Routes("home")
    data object Search  : Routes("search")
    data object Library : Routes("library")
    data object Player  : Routes("player")

    // Phase 5 — detail d'une playlist
    data object PlaylistDetail : Routes("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }

    // Phase 7 — import Spotify
    data object SpotifyImport : Routes("spotify_import")

    // Phase 8 — page artiste
    // On passe le nom en query param (plus sûr que le path param pour les noms avec "/" ou espaces)
    data object Artist : Routes("artist?name={name}") {
        fun createRoute(name: String) = "artist?name=${Uri.encode(name)}"
    }

    // Phase 8 — détail d'un album (playlist YouTube Music)
    data object AlbumDetail : Routes("album?name={name}&url={url}") {
        fun createRoute(name: String, playlistUrl: String) =
            "album?name=${Uri.encode(name)}&url=${Uri.encode(playlistUrl)}"
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.Home.route,    "Accueil",      Icons.Outlined.Home),
    BottomNavItem(Routes.Search.route,  "Recherche",    Icons.Outlined.Search),
    BottomNavItem(Routes.Library.route, "Bibliotheque", Icons.Outlined.LibraryMusic),
)

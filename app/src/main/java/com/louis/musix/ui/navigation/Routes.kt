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

    // Phase 5 — playlist detail
    data object PlaylistDetail : Routes("playlist/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }

    // Phase 7 — Spotify import
    data object SpotifyImport : Routes("spotify_import")

    // Phase 8 — artist page
    // Name passed as query param (safer than path param for names with "/" or spaces)
    data object Artist : Routes("artist?name={name}") {
        fun createRoute(name: String) = "artist?name=${Uri.encode(name)}"
    }

    // Phase 8 — album detail (YouTube Music playlist)
    data object AlbumDetail : Routes("album?name={name}&url={url}") {
        fun createRoute(name: String, playlistUrl: String) =
            "album?name=${Uri.encode(name)}&url=${Uri.encode(playlistUrl)}"
    }

    data object Settings : Routes("settings")

    // Phase 1.0 — open-source licenses
    data object Licenses : Routes("licenses")
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.Home.route,    "Home",    Icons.Outlined.Home),
    BottomNavItem(Routes.Search.route,  "Search",  Icons.Outlined.Search),
    BottomNavItem(Routes.Library.route, "Library", Icons.Outlined.LibraryMusic),
)

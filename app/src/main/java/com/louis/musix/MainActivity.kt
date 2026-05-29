package com.louis.musix

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.louis.musix.data.spotify.SpotifyAuthManager
import com.louis.musix.player.PlayerController
import com.louis.musix.ui.components.MiniPlayer
import com.louis.musix.ui.navigation.MusixBottomBar
import com.louis.musix.ui.navigation.MusixNavHost
import com.louis.musix.ui.navigation.Routes
import com.louis.musix.ui.theme.MusixTheme
import org.koin.android.ext.android.inject
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    // Koin injection at the Activity level (not in the Compose scope)
    private val spotifyAuthManager: SpotifyAuthManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Check whether the Activity was started by the Spotify deep link (cold start)
        handleSpotifyCallback(intent)
        setContent {
            KoinContext {
                MusixTheme {
                    MusixContent()
                }
            }
        }
    }

    /** Called when the app is already in the foreground and receives the deep link (singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSpotifyCallback(intent)
    }

    /**
     * Extracts the auth code or error from the musix://callback deep link
     * and forwards it to [SpotifyAuthManager].
     */
    private fun handleSpotifyCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "musix" || uri.host != "callback") return

        val error = uri.getQueryParameter("error")
        if (error != null) {
            spotifyAuthManager.handleCallbackError(error)
            return
        }
        val code = uri.getQueryParameter("code") ?: return
        spotifyAuthManager.handleCallback(code)
    }
}

@Composable
private fun MusixContent() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val playerController: PlayerController = koinInject()
    val playerState by playerController.state.collectAsStateWithLifecycle()

    // ── Request POST_NOTIFICATIONS permission on Android 13+ ──────────────────
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* granted or denied — the notification will work or not, without crashing */ }
        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // The BottomBar (and MiniPlayer) are hidden on full-screen screens
    val showBottomBar = currentRoute != Routes.Player.route &&
                        currentRoute != Routes.SpotifyImport.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                Column {
                    // ── MiniPlayer — animated vertical slide ──────────────────
                    AnimatedVisibility(
                        visible = playerState.hasActiveMedia,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    ) {
                        MiniPlayer(
                            onTap = {
                                navController.navigate(Routes.Player.route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    // ── Main navigation bar ────────────────────────────────────
                    MusixBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        MusixNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

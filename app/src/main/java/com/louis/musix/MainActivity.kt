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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.louis.musix.data.spotify.SpotifyAuthManager
import com.louis.musix.domain.util.NetworkMonitor
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

    val networkMonitor: NetworkMonitor = koinInject()
    val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle()

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
                    // ── Offline Banner ──────────────────────────────────────────
                    AnimatedVisibility(
                        visible = !isOnline,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.error)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No internet connection. Showing downloaded music only.",
                                color = MaterialTheme.colorScheme.onError,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

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

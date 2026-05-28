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

    // Injection Koin au niveau de l'Activity (pas dans le scope Compose)
    private val spotifyAuthManager: SpotifyAuthManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Vérifier si l'Activity a été démarrée par le deep link Spotify (démarrage à froid)
        handleSpotifyCallback(intent)
        setContent {
            KoinContext {
                MusixTheme {
                    MusixContent()
                }
            }
        }
    }

    /** Appelé quand l'app est déjà en foreground et reçoit le deep link (singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSpotifyCallback(intent)
    }

    /**
     * Extrait le code ou l'erreur du deep link musix://callback
     * et le pousse dans [SpotifyAuthManager].
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

    // ── Demande la permission POST_NOTIFICATIONS sur Android 13+ ──────────────
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* granted ou refusé — la notification fonctionnera ou non, sans crash */ }
        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // La BottomBar (et le MiniPlayer) sont cachés sur les écrans plein-écran
    val showBottomBar = currentRoute != Routes.Player.route &&
                        currentRoute != Routes.SpotifyImport.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                Column {
                    // ── MiniPlayer — slide vertical anime ─────────────────────
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
                    // ── Barre de navigation principale ─────────────────────────
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

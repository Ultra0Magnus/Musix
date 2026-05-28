package com.louis.musix

import android.Manifest
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
import com.louis.musix.player.PlayerController
import com.louis.musix.ui.components.MiniPlayer
import com.louis.musix.ui.navigation.MusixBottomBar
import com.louis.musix.ui.navigation.MusixNavHost
import com.louis.musix.ui.navigation.Routes
import com.louis.musix.ui.theme.MusixTheme
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KoinContext {
                MusixTheme {
                    MusixContent()
                }
            }
        }
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

    // La BottomBar (et le MiniPlayer) sont cachés sur l'écran player (plein écran)
    val showBottomBar = currentRoute != Routes.Player.route

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

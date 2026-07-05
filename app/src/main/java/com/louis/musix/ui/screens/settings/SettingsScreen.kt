package com.louis.musix.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.BuildConfig
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSpotifyClick: () -> Unit = {},
    onLicensesClick: () -> Unit = {},
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val cacheSizeMb by viewModel.cacheSizeMb.collectAsStateWithLifecycle()
    val spotifyConnected by viewModel.spotifyConnected.collectAsStateWithLifecycle()

    // Refresh cache size + Spotify status when the screen becomes visible
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.updateCacheSize()
        viewModel.refreshSpotifyStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { SettingsHeader("Cache & Storage") }
            item {
                SettingsClickItem(
                    title = "Clear Stream Cache",
                    subtitle = String.format(Locale.US, "Current size: %.1f MB", cacheSizeMb),
                    icon = Icons.Default.DeleteSweep,
                    onClick = { viewModel.clearCache() }
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsHeader("Integrations") }
            item {
                SettingsClickItem(
                    title = "Spotify Connection",
                    subtitle = if (spotifyConnected) "Connected — tap to manage import"
                               else "Not connected — tap to import",
                    icon = if (spotifyConnected) Icons.Default.Link else Icons.Default.LinkOff,
                    onClick = onSpotifyClick
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsHeader("About") }
            item {
                SettingsClickItem(
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME,
                    icon = Icons.Default.Info,
                    onClick = {}
                )
            }
            item {
                SettingsClickItem(
                    title = "Open Source Licenses",
                    subtitle = "Libraries used in Musix",
                    icon = Icons.Default.HistoryEdu,
                    onClick = onLicensesClick
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsClickItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

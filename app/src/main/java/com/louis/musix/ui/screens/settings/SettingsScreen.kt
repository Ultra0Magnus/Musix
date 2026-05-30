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
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val cacheSizeMb by viewModel.cacheSizeMb.collectAsStateWithLifecycle()
    
    // Refresh cache size when screen becomes visible
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.updateCacheSize()
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
            item { SettingsHeader("Playback") }
            item {
                SettingsToggleItem(
                    title = "External Audio Focus",
                    subtitle = "Pause playback when another app starts playing audio",
                    icon = Icons.Default.Audiotrack,
                    checked = true,
                    onCheckedChange = {}
                )
            }
            item {
                SettingsClickItem(
                    title = "Audio Quality",
                    subtitle = "High (YouTube 128kbps AAC)",
                    icon = Icons.Default.HighQuality,
                    onClick = {}
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
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
                    subtitle = "Connected as Louis",
                    icon = Icons.Default.Link,
                    onClick = {}
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item { SettingsHeader("About") }
            item {
                SettingsClickItem(
                    title = "Version",
                    subtitle = "0.6.2 (Stable)",
                    icon = Icons.Default.Info,
                    onClick = {}
                )
            }
            item {
                SettingsClickItem(
                    title = "Open Source Licenses",
                    subtitle = "Libraries used in Musix",
                    icon = Icons.Default.HistoryEdu,
                    onClick = {}
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
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) }
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

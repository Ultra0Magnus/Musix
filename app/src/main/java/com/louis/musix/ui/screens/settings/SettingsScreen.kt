package com.louis.musix.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.BuildConfig
import com.louis.musix.ui.components.BackButton
import com.louis.musix.ui.components.PillButton
import com.louis.musix.ui.components.PillProgressBar
import com.louis.musix.ui.components.ScreenTitle
import com.louis.musix.ui.theme.CardShape
import com.louis.musix.ui.theme.PillShape
import com.louis.musix.ui.theme.SillonLabels
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

/** Repère visuel de la barre de cache (le cache de flux n'a pas de plafond strict). */
private const val CACHE_GAUGE_MB = 300f

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
    LaunchedEffect(Unit) {
        viewModel.updateCacheSize()
        viewModel.refreshSpotifyStatus()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = onBack)
                Spacer(Modifier.width(14.dp))
                ScreenTitle("Settings")
            }
        }

        // ── Spotify ───────────────────────────────────────────────────────────
        item {
            SettingsCard(title = "Spotify") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (spotifyConnected) SillonLabels[2]
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (spotifyConnected) "Connected" else "Not connected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Import your playlists, liked tracks and listening history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        text = "Import",
                        icon = Icons.Outlined.CloudSync,
                        onClick = onSpotifyClick,
                    )
                    if (spotifyConnected) {
                        PillButton(
                            text = "Disconnect",
                            outlined = true,
                            onClick = viewModel::disconnectSpotify,
                        )
                    }
                }
            }
        }

        // ── Stockage ──────────────────────────────────────────────────────────
        item {
            SettingsCard(title = "Storage") {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.US, "%.1f MB", cacheSizeMb),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Stream cache",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                PillProgressBar(fraction = cacheSizeMb / CACHE_GAUGE_MB)
                PillButton(
                    text = "Clear cache",
                    icon = Icons.Outlined.DeleteSweep,
                    outlined = true,
                    onClick = { viewModel.clearCache() },
                )
            }
        }

        // ── À propos ──────────────────────────────────────────────────────────
        item {
            SettingsCard(title = "About") {
                InfoRow(label = "Version", value = BuildConfig.VERSION_NAME)
                InfoRow(label = "Open source licenses", value = null, onClick = onLicensesClick)
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String?,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

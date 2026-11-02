package com.louis.musix.ui.screens.spotify

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.ui.components.PageHeader
import com.louis.musix.ui.components.PillButton
import com.louis.musix.ui.components.ProgressRing
import com.louis.musix.ui.components.ScreenTitle
import com.louis.musix.ui.components.VinylDisc
import com.louis.musix.ui.theme.CardShape
import com.louis.musix.ui.theme.PillShape
import org.koin.androidx.compose.koinViewModel

@Composable
fun SpotifyImportScreen(onBack: () -> Unit) {
    val viewModel: SpotifyImportViewModel = koinViewModel()
    val state by viewModel.importState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // JSON file picker (Spotify GDPR export)
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val files = mutableMapOf<String, String>()
        uris.forEach { uri ->
            try {
                val name = context.contentResolver
                    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> cursor.moveToFirst(); cursor.getString(0) }
                    ?: uri.lastPathSegment ?: "file.json"
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText() ?: ""
                if (content.isNotEmpty()) files[name] = content
            } catch (_: Exception) {}
        }
        if (files.isNotEmpty()) viewModel.importFromJsonFiles(files)
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(onBack = onBack, label = "Spotify")
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (val s = state) {
                is ImportState.Idle        -> IdleContent(
                    viewModel   = viewModel,
                    onPickFiles = { filePicker.launch(arrayOf("application/json", "*/*")) },
                )
                is ImportState.WaitingAuth -> WaitingAuthContent(viewModel)
                is ImportState.Exchanging  -> ExchangingContent()
                is ImportState.Importing   -> ImportingContent(s, viewModel)
                is ImportState.Done        -> DoneContent(s, onBack)
                is ImportState.Error       -> ErrorContent(s, viewModel)
            }
        }
    }
}

// ── Idle — two options ────────────────────────────────────────────────────────

@Composable
private fun IdleContent(
    viewModel: SpotifyImportViewModel,
    onPickFiles: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Import from Spotify")
        Text(
            text = "Bring your playlists, liked tracks and listening history into Musix.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ── Option A: JSON files (recommended) ───────────────────────────────
        OptionCard(
            title = "Data export",
            badge = "Recommended",
        ) {
            Text(
                text = "Request your export at spotify.com/account/privacy " +
                       "(email within 5 days). Extract the ZIP, then select:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FileHint("Streaming_History_Audio_*.json", "Top 50 all time + Top 20 by year")
            FileHint("YourLibrary.json", "your liked tracks")
            FileHint("Playlist*.json", "your playlists")
            PillButton(
                text = "Select JSON files",
                icon = Icons.Outlined.FolderOpen,
                onClick = onPickFiles,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── Option B: OAuth API (Premium required) ────────────────────────────
        OptionCard(title = "Account connection") {
            Text(
                text = "Uses the Spotify API. Requires the app's developer account to have Spotify Premium.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (viewModel.isConnected) {
                PillButton(
                    text = "Start import (API)",
                    icon = Icons.Outlined.Link,
                    outlined = true,
                    onClick = viewModel::connectAndImport,
                    modifier = Modifier.fillMaxWidth(),
                )
                PillButton(
                    text = "Disconnect",
                    outlined = true,
                    onClick = viewModel::disconnect,
                    modifier = Modifier.fillMaxWidth(),
                    contentColor = MaterialTheme.colorScheme.error,
                )
            } else {
                PillButton(
                    text = "Connect Spotify (Premium)",
                    icon = Icons.Outlined.Link,
                    outlined = true,
                    onClick = viewModel::connectAndImport,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun OptionCard(
    title: String,
    badge: String? = null,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        content()
    }
}

@Composable
private fun FileHint(file: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(file, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Disque qui tourne, entouré d'un anneau de progression optionnel. */
@Composable
private fun SpinningRecord(progress: Float? = null) {
    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
        if (progress != null) {
            ProgressRing(progress = progress, modifier = Modifier.size(150.dp), strokeWidth = 5.dp)
        }
        VinylDisc(
            artworkUrl = null,
            labelKey = "spotify",
            spinning = true,
            modifier = Modifier.size(134.dp),
        )
    }
}

// ── Waiting for browser redirect ──────────────────────────────────────────────

@Composable
private fun WaitingAuthContent(viewModel: SpotifyImportViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SpinningRecord()
        Text("Waiting for Spotify…", style = MaterialTheme.typography.headlineSmall)
        Text(
            text  = "Authorize access in your browser\nthen come back to the app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PillButton(text = "Cancel", outlined = true, onClick = viewModel::retry)
    }
}

// ── Token exchange ────────────────────────────────────────────────────────────

@Composable
private fun ExchangingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SpinningRecord()
        Text("Connecting…", style = MaterialTheme.typography.headlineSmall)
    }
}

// ── Import in progress ────────────────────────────────────────────────────────

@Composable
private fun ImportingContent(state: ImportState.Importing, viewModel: SpotifyImportViewModel) {
    val progress = if (state.total > 0) state.current.toFloat() / state.total.toFloat() else 0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SpinningRecord(progress = if (state.total > 0) progress else null)
        Text("Importing…", style = MaterialTheme.typography.headlineSmall)
        Text(
            text  = state.phase,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (state.total > 0) {
            Text(
                text  = "${state.current} / ${state.total}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text  = "Import may take a few minutes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        PillButton(text = "Cancel", outlined = true, onClick = viewModel::cancelImport)
    }
}

// ── Done ──────────────────────────────────────────────────────────────────────

@Composable
private fun DoneContent(state: ImportState.Done, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp),
            )
        }
        Text("Import complete!", style = MaterialTheme.typography.displaySmall)

        val t = state.tracksImported
        val p = state.playlistsCreated
        Text(
            text = "${t} song${if (t > 1) "s" else ""} imported\n" +
                   "${p} playlist${if (p > 1) "s" else ""} created",
            style     = MaterialTheme.typography.bodyLarge,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        PillButton(text = "Go to library", onClick = onBack, modifier = Modifier.fillMaxWidth())
    }
}

// ── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(state: ImportState.Error, viewModel: SpotifyImportViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier           = Modifier.size(64.dp),
            tint               = MaterialTheme.colorScheme.error,
        )
        Text(
            text      = "Something went wrong",
            style     = MaterialTheme.typography.headlineSmall,
            color     = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text      = state.message,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        PillButton(text = "Retry", onClick = viewModel::retry, modifier = Modifier.fillMaxWidth())
    }
}

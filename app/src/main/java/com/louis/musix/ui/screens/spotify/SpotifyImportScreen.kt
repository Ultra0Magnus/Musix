package com.louis.musix.ui.screens.spotify

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.ui.components.inkButtonColors
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from Spotify") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
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
        modifier = Modifier.verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.CloudSync,
            contentDescription = null,
            modifier           = Modifier.size(64.dp),
            tint               = MaterialTheme.colorScheme.primary,
        )

        Text(
            text      = "Import from Spotify",
            style     = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        // ── Option A: JSON files (recommended) ───────────────────────────────
        Text(
            text       = "Recommended option — GDPR Export",
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.primary,
            textAlign  = TextAlign.Center,
        )

        Text(
            text = "Request your export at spotify.com/account/privacy " +
                   "(email within 5 days). Extract the ZIP, then select:\n\n" +
                   "• Streaming_History_Audio_*.json → creates a Top 50 all time + Top 20 by year\n" +
                   "• YourLibrary.json → your liked tracks\n" +
                   "• Playlist*.json → your playlists",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Button(
            onClick  = onPickFiles,
            modifier = Modifier.fillMaxWidth(),
            colors = inkButtonColors(),
        ) {
            Icon(
                Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text("Select JSON files")
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))

        // ── Option B: OAuth API (Premium required) ────────────────────────────
        Text(
            text       = "Alternative option — Spotify API (Premium required)",
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign  = TextAlign.Center,
        )

        Text(
            text = "Requires the app's developer account to have Spotify Premium.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (viewModel.isConnected) {
            OutlinedButton(
                onClick  = viewModel::connectAndImport,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Start import (API)") }

            TextButton(
                onClick  = viewModel::disconnect,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Disconnect") }
        } else {
            OutlinedButton(
                onClick  = viewModel::connectAndImport,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Connect Spotify (Premium)") }
        }
    }
}

// ── Waiting for browser redirect ──────────────────────────────────────────────

@Composable
private fun WaitingAuthContent(viewModel: SpotifyImportViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(56.dp))
        Text("Waiting for Spotify...", style = MaterialTheme.typography.titleMedium)
        Text(
            text  = "Authorize access in your browser\nthen come back to the app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = viewModel::retry) { Text("Cancel") }
    }
}

// ── Token exchange ────────────────────────────────────────────────────────────

@Composable
private fun ExchangingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(56.dp))
        Text("Connecting...", style = MaterialTheme.typography.titleMedium)
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
        CircularProgressIndicator(modifier = Modifier.size(56.dp))
        Text("Importing...", style = MaterialTheme.typography.titleMedium)
        Text(
            text  = state.phase,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (state.total > 0) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                text  = "${state.current} / ${state.total}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text  = "Import may take a few minutes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = viewModel::cancelImport) { Text("Cancel") }
    }
}

// ── Done ──────────────────────────────────────────────────────────────────────

@Composable
private fun DoneContent(state: ImportState.Done, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector        = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier           = Modifier.size(72.dp),
            tint               = MaterialTheme.colorScheme.primary,
        )
        Text("Import complete!", style = MaterialTheme.typography.headlineSmall)

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
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = inkButtonColors()) {
            Text("Go to library")
        }
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
            imageVector        = Icons.Outlined.Error,
            contentDescription = null,
            modifier           = Modifier.size(64.dp),
            tint               = MaterialTheme.colorScheme.error,
        )
        Text(
            text      = "Something went wrong",
            style     = MaterialTheme.typography.titleLarge,
            color     = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Text(
            text      = state.message,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = viewModel::retry, modifier = Modifier.fillMaxWidth(), colors = inkButtonColors()) {
            Text("Retry")
        }
    }
}

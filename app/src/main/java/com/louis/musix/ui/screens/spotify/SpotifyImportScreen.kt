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
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyImportScreen(onBack: () -> Unit) {
    val viewModel: SpotifyImportViewModel = koinViewModel()
    val state by viewModel.importState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Sélecteur de fichiers JSON (export RGPD Spotify)
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
                title = { Text("Importer depuis Spotify") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Retour")
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
                    viewModel  = viewModel,
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

// ── Idle — les deux options côte à côte ───────────────────────────────────────

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
            text      = "Importer depuis Spotify",
            style     = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        // ── Option A : fichiers JSON (recommandée) ────────────────────────────
        Text(
            text      = "Option recommandée — Export RGPD",
            style     = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color     = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Demande ton export sur spotify.com/account/privacy " +
                   "(mail reçu sous 5 jours). Extrais le ZIP, puis sélectionne :\n\n" +
                   "• Streaming_History_Audio_*.json → crée un Top 50 all time + Top 20 par année\n" +
                   "• YourLibrary.json → tes titres aimés\n" +
                   "• Playlist*.json → tes playlists",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Button(
            onClick  = onPickFiles,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Outlined.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text("Sélectionner les fichiers JSON")
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))

        // ── Option B : API OAuth (Premium requis) ─────────────────────────────
        Text(
            text      = "Option alternative — API Spotify (Premium requis)",
            style     = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Nécessite que le compte développeur de l'app ait Spotify Premium.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (viewModel.isConnected) {
            OutlinedButton(
                onClick  = viewModel::connectAndImport,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Lancer l'import (API)") }

            TextButton(
                onClick  = viewModel::disconnect,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Déconnecter") }
        } else {
            OutlinedButton(
                onClick  = viewModel::connectAndImport,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Connecter Spotify (Premium)") }
        }
    }
}

// ── En attente de la redirection navigateur ───────────────────────────────────

@Composable
private fun WaitingAuthContent(viewModel: SpotifyImportViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(56.dp))
        Text("En attente de Spotify...", style = MaterialTheme.typography.titleMedium)
        Text(
            text  = "Autorise l'accès dans le navigateur\npuis reviens sur l'app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = viewModel::retry) { Text("Annuler") }
    }
}

// ── Échange du code ───────────────────────────────────────────────────────────

@Composable
private fun ExchangingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(56.dp))
        Text("Connexion en cours...", style = MaterialTheme.typography.titleMedium)
    }
}

// ── Import en cours ───────────────────────────────────────────────────────────

@Composable
private fun ImportingContent(state: ImportState.Importing, viewModel: SpotifyImportViewModel) {
    val progress = if (state.total > 0) state.current.toFloat() / state.total.toFloat() else 0f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(56.dp))
        Text("Importation en cours...", style = MaterialTheme.typography.titleMedium)
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
            text  = "L'import peut prendre quelques minutes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = viewModel::cancelImport) { Text("Annuler") }
    }
}

// ── Terminé ───────────────────────────────────────────────────────────────────

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
        Text("Import terminé !", style = MaterialTheme.typography.headlineSmall)

        val t = state.tracksImported
        val p = state.playlistsCreated
        Text(
            text = "${t} morceau${if (t > 1) "x" else ""} importé${if (t > 1) "s" else ""}\n" +
                   "${p} playlist${if (p > 1) "s" else ""} créée${if (p > 1) "s" else ""}",
            style     = MaterialTheme.typography.bodyLarge,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Voir ma bibliotheque")
        }
    }
}

// ── Erreur ────────────────────────────────────────────────────────────────────

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
            text      = "Quelque chose s'est mal passé",
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
        Button(onClick = viewModel::retry, modifier = Modifier.fillMaxWidth()) {
            Text("Réessayer")
        }
    }
}

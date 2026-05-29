package com.louis.musix.ui.screens.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.domain.model.Song
import com.louis.musix.ui.components.SongRow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    /** [songs] = toute la playlist, [startIndex] = rang du morceau cliqué. */
    onSongClick: (songs: List<Song>, startIndex: Int) -> Unit,
) {
    val viewModel: PlaylistDetailViewModel = koinViewModel(parameters = { parametersOf(playlistId) })
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.playlistName.ifEmpty { "Playlist" },
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.songs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Playlist vide\nAjoute des morceaux depuis la recherche",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // Bouton "Tout lire"
                Button(
                    onClick = { if (state.songs.isNotEmpty()) onSongClick(state.songs, 0) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Tout lire (${state.songs.size} morceaux)")
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(
                        items = state.songs,
                        key   = { index, song -> "${index}_${song.id}" },
                    ) { index, song ->
                        // ── Swipe-to-dismiss sécurisé ─────────────────────────
                        // On n'utilise PAS confirmValueChange avec side-effect
                        // (risque de suppression lors d'un recompose).
                        // L'action est confirmée via LaunchedEffect sur currentValue.
                        val dismissState = rememberSwipeToDismissBoxState(
                            positionalThreshold = { totalDistance -> totalDistance * 0.5f },
                        )
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.removeSong(song.id)
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            },
                            enableDismissFromStartToEnd = false,
                        ) {
                            SongRow(
                                song    = song,
                                onClick = { onSongClick(state.songs, index) },
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}

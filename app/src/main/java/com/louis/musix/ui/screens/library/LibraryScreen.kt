package com.louis.musix.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.domain.model.Playlist
import com.louis.musix.domain.model.Song
import com.louis.musix.ui.components.SongRow
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onPlaylistClick: (Long) -> Unit = {},
    onSongClick: (Song) -> Unit = {},
    onSpotifyImportClick: () -> Unit = {},
) {
    val viewModel: LibraryViewModel = koinViewModel()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Playlists", "Favorites", "History", "Downloads")

    // Create playlist dialog
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false; newPlaylistName = "" },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createPlaylist(newPlaylistName)
                    showCreateDialog = false
                    newPlaylistName = ""
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newPlaylistName = "" }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "New playlist")
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            when (selectedTab) {
                0 -> PlaylistsTab(playlists, onPlaylistClick, viewModel::deletePlaylist, onSpotifyImportClick)
                1 -> FavoritesTab(favorites, onSongClick, viewModel::removeFavorite)
                2 -> HistoryTab(history, onSongClick, viewModel::clearHistory)
                3 -> DownloadsTab(viewModel.downloaded.collectAsStateWithLifecycle().value, onSongClick)
            }
        }
    }
}

// ─── Playlists Tab ────────────────────────────────────────────────────────────

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onSpotifyImport: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Spotify import button at the top of the tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Outlined.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            TextButton(onClick = onSpotifyImport) {
                Text("Import from Spotify")
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        )

        if (playlists.isEmpty()) {
            EmptyState("No playlists\nTap + to create one")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp),
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.id) },
                        onDelete = { onDelete(playlist.id) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.PlaylistPlay,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${playlist.songCount} song${if (playlist.songCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClick) { Text("Open") }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error)
        }
    }
}

// ─── Favorites Tab ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoritesTab(
    favorites: List<Song>,
    onSongClick: (Song) -> Unit,
    onRemove: (Song) -> Unit,
) {
    if (favorites.isEmpty()) {
        EmptyState("No favorites yet\nTap the heart in the player")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(favorites, key = { it.id }) { song ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onRemove(song)
                            true
                        } else false
                    }
                )
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
                    SongRow(song = song, onClick = onSongClick)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                )
            }
        }
    }
}

// ─── History Tab ──────────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(
    history: List<Song>,
    onSongClick: (Song) -> Unit,
    onClearAll: () -> Unit,
) {
    if (history.isEmpty()) {
        EmptyState("No history yet\nPlay a song to get started")
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            // "Clear all" button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onClearAll) {
                    Icon(Icons.Outlined.Delete, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Clear all")
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(history, key = { it.id }) { song ->
                    SongRow(song = song, onClick = onSongClick)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

// ─── Downloads Tab ────────────────────────────────────────────────────────────

@Composable
private fun DownloadsTab(
    downloaded: List<Song>,
    onSongClick: (Song) -> Unit,
) {
    if (downloaded.isEmpty()) {
        EmptyState("No downloads yet\nOffline music will appear here")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(downloaded, key = { it.id }) { song ->
                SongRow(song = song, onClick = onSongClick)
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                )
            }
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

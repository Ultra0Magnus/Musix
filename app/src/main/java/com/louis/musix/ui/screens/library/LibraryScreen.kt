package com.louis.musix.ui.screens.library

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.domain.model.Playlist
import com.louis.musix.domain.model.Song
import com.louis.musix.ui.components.ScreenTitle
import com.louis.musix.ui.components.SongRow
import com.louis.musix.ui.components.inkButtonColors
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
    val favorites by viewModel.filteredFavorites.collectAsStateWithLifecycle()
    val history by viewModel.filteredHistory.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Playlists", "Favorites", "History", "Downloads")

    // Create playlist dialog
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // Rename playlist dialog — non-null target means the dialog is open
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var renameText by remember { mutableStateOf("") }

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
                Button(
                    onClick = {
                        viewModel.createPlaylist(newPlaylistName)
                        showCreateDialog = false
                        newPlaylistName = ""
                    },
                    colors = inkButtonColors(),
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false; newPlaylistName = "" }) {
                    Text("Cancel")
                }
            },
        )
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename playlist") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renamePlaylist(target.id, renameText)
                        renameTarget = null
                    },
                    colors = inkButtonColors(),
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RectangleShape,
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
            ScreenTitle(
                text = "Library",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
            )

            LibTabs(
                tabs = tabs,
                selected = selectedTab,
                onSelect = { selectedTab = it },
            )

            when (selectedTab) {
                0 -> PlaylistsTab(
                    playlists       = playlists,
                    onPlaylistClick = onPlaylistClick,
                    onDelete        = viewModel::deletePlaylist,
                    onRenameRequest = { playlist -> renameTarget = playlist; renameText = playlist.name },
                    onSpotifyImport = onSpotifyImportClick,
                )
                1 -> FavoritesTab(favorites, query, viewModel::onQueryChange, onSongClick, viewModel::removeFavorite)
                2 -> HistoryTab(history, query, viewModel::onQueryChange, onSongClick, viewModel::clearHistory)
                3 -> DownloadsTab(viewModel.downloaded.collectAsStateWithLifecycle().value, onSongClick, viewModel::removeDownload)
            }
        }
    }
}

// ─── Library tabs (soulignement vert, angles droits) ──────────────────────────

@Composable
private fun LibTabs(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        tabs.forEachIndexed { index, title ->
            val isSel = index == selected
            val accent = MaterialTheme.colorScheme.primary
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSel) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onSelect(index) }
                    .padding(vertical = 12.dp)
                    .then(
                        if (isSel) Modifier.drawBehind {
                            drawRect(
                                color = accent,
                                topLeft = Offset(0f, size.height + 2.dp.toPx()),
                                size = Size(size.width, 2.dp.toPx()),
                            )
                        } else Modifier
                    ),
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

// ─── Playlists Tab ────────────────────────────────────────────────────────────

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRenameRequest: (Playlist) -> Unit,
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
                        onRename = { onRenameRequest(playlist) },
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
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${playlist.songCount} TRACK${if (playlist.songCount > 1) "S" else ""}".uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRename) {
            Icon(Icons.Outlined.Edit, contentDescription = "Rename",
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
    query: String,
    onQueryChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onRemove: (Song) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LibrarySearchField(query, onQueryChange)
        if (favorites.isEmpty()) {
            EmptyState(
                if (query.isBlank()) "No favorites yet\nTap the heart in the player"
                else "No favorites match \"$query\""
            )
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
}

// ─── History Tab ──────────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(
    history: List<Song>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibrarySearchField(query, onQueryChange, modifier = Modifier.weight(1f))
            TextButton(onClick = onClearAll) {
                Icon(Icons.Outlined.Delete, contentDescription = null,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.size(4.dp))
                Text("Clear all")
            }
        }
        if (history.isEmpty()) {
            EmptyState(
                if (query.isBlank()) "No history yet\nPlay a song to get started"
                else "No history matches \"$query\""
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // v0.9.1 fix: removed key={it.id} to allow duplicates in full history without crash
                items(history) { song ->
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

// ─── Library search field ─────────────────────────────────────────────────────

@Composable
private fun LibrarySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search title or artist") },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

// ─── Downloads Tab ────────────────────────────────────────────────────────────

@Composable
private fun DownloadsTab(
    downloaded: List<Song>,
    onSongClick: (Song) -> Unit,
    onRemove: (Song) -> Unit,
) {
    if (downloaded.isEmpty()) {
        EmptyState("No downloads yet\nOffline music will appear here")
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(downloaded, key = { it.id }) { song ->
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

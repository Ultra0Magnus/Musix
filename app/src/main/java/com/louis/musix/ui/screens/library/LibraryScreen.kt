package com.louis.musix.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.domain.model.Playlist
import com.louis.musix.domain.model.Song
import com.louis.musix.ui.components.PillButton
import com.louis.musix.ui.components.PillTabs
import com.louis.musix.ui.components.PillTextField
import com.louis.musix.ui.components.RoundIconButton
import com.louis.musix.ui.components.ScreenTitle
import com.louis.musix.ui.components.SheetRow
import com.louis.musix.ui.components.SillonBottomSheet
import com.louis.musix.ui.components.SongRow
import com.louis.musix.ui.components.VinylDisc
import com.louis.musix.ui.components.accentButtonColors
import com.louis.musix.ui.theme.PillShape
import com.louis.musix.ui.theme.SillonOnAccent
import org.koin.androidx.compose.koinViewModel

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
    val downloaded by viewModel.downloaded.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Playlists", "Favorites", "History", "Downloads")

    // Create playlist dialog
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // Rename playlist dialog — non-null target means the dialog is open
    var renameTarget by remember { mutableStateOf<Playlist?>(null) }
    var renameText by remember { mutableStateOf("") }

    // Playlist options sheet
    var optionsTarget by remember { mutableStateOf<Playlist?>(null) }

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            value = newPlaylistName,
            onValueChange = { newPlaylistName = it },
            onConfirm = {
                viewModel.createPlaylist(newPlaylistName)
                showCreateDialog = false
                newPlaylistName = ""
            },
            onDismiss = { showCreateDialog = false; newPlaylistName = "" },
        )
    }

    renameTarget?.let { target ->
        PlaylistNameDialog(
            title = "Rename playlist",
            confirmLabel = "Rename",
            value = renameText,
            onValueChange = { renameText = it },
            onConfirm = {
                viewModel.renamePlaylist(target.id, renameText)
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    optionsTarget?.let { target ->
        SillonBottomSheet(
            onDismiss = { optionsTarget = null },
            title = target.name,
            subtitle = trackCount(target.songCount),
            skipPartiallyExpanded = true,
        ) {
            SheetRow(label = "Rename", icon = Icons.Outlined.Edit, onClick = {
                renameTarget = target
                renameText = target.name
                optionsTarget = null
            })
            SheetRow(label = "Delete playlist", icon = Icons.Outlined.Delete, danger = true, onClick = {
                viewModel.deletePlaylist(target.id)
                optionsTarget = null
            })
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── En-tête : « Library » + pilule Spotify ────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenTitle(text = "Library", modifier = Modifier.weight(1f))
            PillButton(
                text = "Spotify",
                icon = Icons.Outlined.CloudSync,
                onClick = onSpotifyImportClick,
                outlined = true,
                modifier = Modifier.height(40.dp),
            )
        }

        PillTabs(
            tabs = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
        )

        Spacer(Modifier.height(8.dp))

        when (selectedTab) {
            0 -> PlaylistsTab(
                playlists       = playlists,
                onPlaylistClick = onPlaylistClick,
                onOptions       = { optionsTarget = it },
                onCreate        = { showCreateDialog = true },
            )
            1 -> FavoritesTab(favorites, query, viewModel::onQueryChange, onSongClick, viewModel::removeFavorite)
            2 -> HistoryTab(history, query, viewModel::onQueryChange, onSongClick, viewModel::clearHistory)
            3 -> DownloadsTab(downloaded, onSongClick, viewModel::removeDownload)
        }
    }
}

private fun trackCount(n: Int) = "$n track${if (n > 1) "s" else ""}"

// ─── Dialog ───────────────────────────────────────────────────────────────────

@Composable
private fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
        text = {
            PillTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = "Playlist name",
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = value.isNotBlank(),
                colors = accentButtonColors(),
                shape = PillShape,
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

// ─── Playlists Tab : grille de disques ───────────────────────────────────────

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (Long) -> Unit,
    onOptions: (Playlist) -> Unit,
    onCreate: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistTile(
                playlist = playlist,
                onClick = { onPlaylistClick(playlist.id) },
                onOptions = { onOptions(playlist) },
            )
        }
        item(key = "new") { NewPlaylistTile(onClick = onCreate) }
    }
}

@Composable
private fun PlaylistTile(
    playlist: Playlist,
    onClick: () -> Unit,
    onOptions: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VinylDisc(
            artworkUrl = null,
            labelKey = playlist.name,
            labelFraction = 0.46f,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            labelContent = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${playlist.songCount}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SillonOnAccent,
                    )
                    Text(
                        text = if (playlist.songCount == 1) "track" else "tracks",
                        style = MaterialTheme.typography.labelSmall,
                        color = SillonOnAccent,
                    )
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            RoundIconButton(
                icon = Icons.Outlined.MoreHoriz,
                contentDescription = "Playlist options",
                onClick = onOptions,
                size = 32.dp,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NewPlaylistTile(onClick: () -> Unit) {
    val stroke = MaterialTheme.colorScheme.outline
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .drawBehind {
                    val w = 2.dp.toPx()
                    drawCircle(
                        color = stroke,
                        radius = size.minDimension / 2f - w,
                        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "New playlist",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}

// ─── Swipe-to-remove song row ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableSongRow(
    song: Song,
    onSongClick: (Song) -> Unit,
    onRemove: (Song) -> Unit,
) {
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
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = SillonOnAccent)
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        SongRow(song = song, onClick = onSongClick)
    }
}

// ─── Favorites Tab ────────────────────────────────────────────────────────────

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
                if (query.isBlank()) "No favorites yet\nTap Like in the player"
                else "No favorites match \"$query\""
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(favorites, key = { it.id }) { song ->
                    SwipeableSongRow(song, onSongClick, onRemove)
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
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibrarySearchField(query, onQueryChange, modifier = Modifier.weight(1f))
            RoundIconButton(
                icon = Icons.Outlined.DeleteSweep,
                contentDescription = "Clear history",
                onClick = onClearAll,
                size = 48.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.error,
            )
        }
        if (history.isEmpty()) {
            EmptyState(
                if (query.isBlank()) "No history yet\nPlay a song to get started"
                else "No history matches \"$query\""
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
                // v0.9.1 fix: removed key={it.id} to allow duplicates in full history without crash
                items(history) { song ->
                    SongRow(song = song, onClick = onSongClick)
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
    PillTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = "Search title or artist",
        leadingIcon = Icons.Outlined.Search,
        onClear = { onQueryChange("") },
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
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
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(downloaded, key = { it.id }) { song ->
                SwipeableSongRow(song, onSongClick, onRemove)
            }
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VinylDisc(artworkUrl = null, labelKey = text, modifier = Modifier.size(110.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

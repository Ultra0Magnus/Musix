package com.louis.musix.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.domain.model.Playlist
import com.louis.musix.domain.model.Song
import com.louis.musix.domain.util.NetworkMonitor
import com.louis.musix.player.PlayerController
import com.louis.musix.ui.components.PillButton
import com.louis.musix.ui.components.ScreenTitle
import com.louis.musix.ui.components.SectionTitle
import com.louis.musix.ui.components.SheetRow
import com.louis.musix.ui.components.SillonBottomSheet
import com.louis.musix.ui.components.SongRow
import com.louis.musix.ui.components.VinylDisc
import com.louis.musix.ui.theme.PillShape
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    onSongClick: (Song) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
) {
    val viewModel: SearchViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    val networkMonitor: NetworkMonitor = koinInject()
    val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle()

    // Needs to interact with Room (playlists, favorites) when the BottomSheet opens
    val libraryRepo: LibraryRepository = koinInject()
    val playerController: PlayerController = koinInject()

    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    // Song selected for the options menu
    var optionsSong by remember { mutableStateOf<Song?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    // Playlist selection sheet
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    // Favorite state of the song in the sheet
    var isFavoriteSong by remember { mutableStateOf(false) }

    // Options bottom sheet
    if (showSheet && optionsSong != null) {
        val song = optionsSong!!
        SillonBottomSheet(
            onDismiss = { showSheet = false },
            title = song.title,
            subtitle = song.artist,
            skipPartiallyExpanded = true,
        ) {
            SheetRow(
                label = if (isFavoriteSong) "Remove from favorites" else "Add to favorites",
                icon = if (isFavoriteSong) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                onClick = {
                    scope.launch {
                        libraryRepo.toggleFavorite(song)
                        isFavoriteSong = !isFavoriteSong
                    }
                },
            )
            SheetRow(
                label = "Add to queue",
                icon = Icons.Outlined.QueueMusic,
                onClick = {
                    playerController.addToQueue(song)
                    showSheet = false
                },
            )
            SheetRow(
                label = "Add to playlist",
                icon = Icons.Outlined.PlaylistAdd,
                onClick = {
                    scope.launch {
                        playlists = libraryRepo.playlists.first()
                        showSheet = false
                        showPlaylistPicker = true
                    }
                },
            )
        }
    }

    // Playlist selection sheet
    if (showPlaylistPicker && optionsSong != null) {
        SillonBottomSheet(
            onDismiss = { showPlaylistPicker = false },
            title = "Add to playlist",
            subtitle = optionsSong!!.title,
            skipPartiallyExpanded = true,
        ) {
            if (playlists.isEmpty()) {
                Text(
                    "No playlists. Create one from the Library tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                )
            } else {
                playlists.forEach { playlist ->
                    PlaylistPickRow(playlist) {
                        val song = optionsSong!!
                        scope.launch { libraryRepo.addSongToPlaylist(playlist.id, song) }
                        showPlaylistPicker = false
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        ScreenTitle(
            text = "Search",
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp),
        )

        // Barre de recherche en pilule
        TextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = {
                Text(
                    if (isOnline) "Songs or artists" else "Offline — search unavailable",
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            enabled = isOnline,
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        keyboard?.hide()
                        viewModel.clearSearch()
                    }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboard?.hide()
                if (isOnline) viewModel.onSearch()
            }),
            shape = PillShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        when (val state = uiState) {

            is SearchUiState.Idle -> {
                if (searchHistory.isEmpty()) {
                    EmptyState(
                        title = "Drop the needle",
                        message = "Search for an artist or a track to get started.",
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SectionTitle("Recent searches")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            searchHistory.forEach { histQuery ->
                                Row(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { viewModel.searchFromHistory(histQuery) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Outlined.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        histQuery,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is SearchUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    VinylDisc(
                        artworkUrl = null,
                        labelKey = query,
                        spinning = true,
                        modifier = Modifier.size(96.dp),
                    )
                }
            }

            is SearchUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    item {
                        SectionTitle(
                            text = "Results",
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                            trailing = {
                                Text(
                                    text = "${state.songs.size} tracks",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                    items(items = state.songs, key = { it.id }) { song ->
                        SongRow(
                            song          = song,
                            onClick       = onSongClick,
                            onArtistClick = onArtistClick,
                            onMoreClick   = { selected ->
                                scope.launch {
                                    isFavoriteSong = libraryRepo.isFavorite(selected.id).first()
                                }
                                optionsSong = selected
                                showSheet = true
                            },
                        )
                    }
                    // "Load more" button
                    if (state.nextPage != null || state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (state.isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    PillButton(
                                        text = "Load more results",
                                        onClick = viewModel::loadMore,
                                        outlined = true,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            is SearchUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistPickRow(playlist: Playlist, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VinylDisc(artworkUrl = null, labelKey = playlist.name, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(playlist.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "${playlist.songCount} song${if (playlist.songCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VinylDisc(artworkUrl = null, labelKey = title, modifier = Modifier.size(140.dp))
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

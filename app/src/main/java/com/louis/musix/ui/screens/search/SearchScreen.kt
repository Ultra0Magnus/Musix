package com.louis.musix.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.data.repo.LibraryRepository
import com.louis.musix.domain.model.Playlist
import com.louis.musix.domain.model.Song
import com.louis.musix.player.PlayerController
import com.louis.musix.ui.components.SongRow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onSongClick: (Song) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
) {
    val viewModel: SearchViewModel = koinViewModel()
    val libraryRepo: LibraryRepository = koinInject()
    val playerController: PlayerController = koinInject()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Chanson selectionnee pour le menu options
    var optionsSong by remember { mutableStateOf<Song?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    // Dialog selection playlist
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    // Etat favori de la chanson dans le sheet
    var isFavoriteSong by remember { mutableStateOf(false) }

    // Bottom sheet options
    if (showSheet && optionsSong != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = optionsSong!!.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text = optionsSong!!.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                // Bouton favori
                TextButton(
                    onClick = {
                        scope.launch {
                            libraryRepo.toggleFavorite(optionsSong!!)
                            isFavoriteSong = !isFavoriteSong
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = if (isFavoriteSong) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavoriteSong) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(if (isFavoriteSong) "Retirer des favoris" else "Ajouter aux favoris")
                }

                // Bouton ajouter à la file d'attente
                TextButton(
                    onClick = {
                        playerController.addToQueue(optionsSong!!)
                        showSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.QueueMusic, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Ajouter à la file d'attente")
                }

                // Bouton ajouter a une playlist
                TextButton(
                    onClick = {
                        scope.launch {
                            playlists = libraryRepo.playlists.first()
                            showPlaylistDialog = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.PlaylistAdd, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Ajouter a une playlist")
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Dialog selection de playlist
    if (showPlaylistDialog && optionsSong != null) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Choisir une playlist") },
            text = {
                if (playlists.isEmpty()) {
                    Text("Aucune playlist. Cree-en une depuis l'onglet Bibliotheque.")
                } else {
                    Column {
                        playlists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        libraryRepo.addSongToPlaylist(playlist.id, optionsSong!!)
                                    }
                                    showPlaylistDialog = false
                                    showSheet = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(playlist.name)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPlaylistDialog = false }) { Text("Annuler") }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Barre de recherche
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = {
                Text("Artiste, titre, album...", color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null)
            },
            trailingIcon = {
                IconButton(onClick = {
                    keyboard?.hide()
                    viewModel.onSearch()
                }) {
                    Icon(Icons.Outlined.Search, contentDescription = "Lancer la recherche")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboard?.hide()
                viewModel.onSearch()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            shape = MaterialTheme.shapes.medium,
        )

        when (val state = uiState) {

            is SearchUiState.Idle -> {
                if (searchHistory.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Tape un artiste ou un titre pour commencer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                        Text(
                            "Recherches récentes",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp),
                        )
                        searchHistory.forEach { histQuery ->
                            TextButton(
                                onClick  = { viewModel.searchFromHistory(histQuery) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.Outlined.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.size(12.dp))
                                Text(
                                    histQuery,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }

            is SearchUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            is SearchUiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        )
                    }
                    // Bouton "Charger plus"
                    if (state.nextPage != null || state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (state.isLoadingMore) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                } else {
                                    TextButton(
                                        onClick  = viewModel::loadMore,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Charger plus de résultats")
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            is SearchUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        }
    }
}

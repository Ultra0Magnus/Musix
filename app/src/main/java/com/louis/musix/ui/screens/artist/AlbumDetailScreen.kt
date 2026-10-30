package com.louis.musix.ui.screens.artist

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.domain.model.Song
import com.louis.musix.ui.components.PageHeader
import com.louis.musix.ui.components.PillButton
import com.louis.musix.ui.components.SongRow
import com.louis.musix.ui.components.VinylDisc
import org.koin.androidx.compose.koinViewModel

/**
 * Album detail screen: lists tracks from a YouTube Music playlist.
 *
 * @param albumName    Album name (displayed in the hero).
 * @param playlistUrl  YouTube Music playlist URL.
 * @param onSongClick  Starts playback of a track.
 * @param onBack       Navigates back.
 */
@Composable
fun AlbumDetailScreen(
    albumName: String,
    playlistUrl: String,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: AlbumDetailViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(playlistUrl) {
        viewModel.loadAlbum(albumName, playlistUrl)
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(onBack = onBack, label = "Album")

        when (val s = state) {

            is AlbumDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    VinylDisc(
                        artworkUrl = null,
                        labelKey = albumName,
                        spinning = true,
                        modifier = Modifier.size(96.dp),
                    )
                }
            }

            is AlbumDetailUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            VinylDisc(
                                artworkUrl = s.songs.firstOrNull()?.thumbnailUrl,
                                labelKey = albumName,
                                labelFraction = 0.5f,
                                modifier = Modifier.size(200.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = albumName,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "${s.songs.size} track${if (s.songs.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (s.songs.isNotEmpty()) {
                                Spacer(Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    PillButton(
                                        text = "Play all",
                                        icon = Icons.Outlined.PlayArrow,
                                        onClick = { onSongClick(s.songs, 0) },
                                    )
                                    PillButton(
                                        text = "Shuffle",
                                        icon = Icons.Outlined.Shuffle,
                                        outlined = true,
                                        onClick = { onSongClick(s.songs.shuffled(), 0) },
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    itemsIndexed(
                        items = s.songs,
                        key   = { i, song -> "${i}_${song.id}" },
                    ) { index, song ->
                        SongRow(
                            song    = song,
                            onClick = { onSongClick(s.songs, index) },
                        )
                    }
                }
            }

            is AlbumDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text      = s.message,
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

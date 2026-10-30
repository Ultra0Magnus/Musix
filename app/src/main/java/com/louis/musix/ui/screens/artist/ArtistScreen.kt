package com.louis.musix.ui.screens.artist

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.domain.model.ArtistAlbum
import com.louis.musix.domain.model.Song
import com.louis.musix.ui.components.PageHeader
import com.louis.musix.ui.components.PillButton
import com.louis.musix.ui.components.SectionTitle
import com.louis.musix.ui.components.ShimmerBox
import com.louis.musix.ui.components.SongRow
import com.louis.musix.ui.components.VinylDisc
import org.koin.androidx.compose.koinViewModel

/**
 * Artist page — hero record + two sections:
 *  1. Popular tracks (top 20 YouTube Music results)
 *  2. Albums (YouTube Music "music_albums" playlist)
 *
 * @param artistName   Artist name passed via navigation.
 * @param onSongClick  Starts playback of a track.
 * @param onAlbumClick Opens the album detail screen.
 * @param onBack       Navigates back.
 */
@Composable
fun ArtistScreen(
    artistName: String,
    onSongClick: (songs: List<Song>, index: Int) -> Unit,
    onAlbumClick: (ArtistAlbum) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: ArtistViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(artistName) {
        viewModel.loadArtist(artistName)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { PageHeader(onBack = onBack, label = "Artist") }

        // ── Hero : disque centré + nom + actions ──────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VinylDisc(
                    artworkUrl = state.topSongs.firstOrNull()?.thumbnailUrl,
                    labelKey = artistName,
                    labelFraction = 0.5f,
                    modifier = Modifier.size(200.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.topSongs.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PillButton(
                            text = "Play all",
                            icon = Icons.Outlined.PlayArrow,
                            onClick = { onSongClick(state.topSongs, 0) },
                        )
                        PillButton(
                            text = "Shuffle",
                            icon = Icons.Outlined.Shuffle,
                            outlined = true,
                            onClick = { onSongClick(state.topSongs.shuffled(), 0) },
                        )
                    }
                }
            }
        }

        // ── Section "Popular tracks" ──────────────────────────────────────────
        item {
            SectionTitle(
                "Popular tracks",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
            )
        }

        when {
            state.isLoadingSongs -> items(4) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(56.dp),
                    cornerRadius = 28.dp,
                )
            }
            state.songsError != null -> item {
                Text(
                    text     = state.songsError!!,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            state.topSongs.isEmpty() -> item {
                Text(
                    text     = "No tracks found",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            else -> {
                itemsIndexed(
                    items = state.topSongs,
                    key   = { i, s -> "${i}_${s.id}" },
                ) { index, song ->
                    SongRow(
                        song    = song,
                        onClick = { onSongClick(state.topSongs, index) },
                    )
                }
            }
        }

        // ── Section "Albums" ──────────────────────────────────────────────────
        item {
            SectionTitle(
                "Albums",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
            )
        }

        when {
            state.isLoadingAlbums -> item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            state.albums.isEmpty() -> item {
                Text(
                    text     = "No albums found",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            else -> item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(items = state.albums, key = { it.id }) { album ->
                        AlbumRecord(album = album, onClick = { onAlbumClick(album) })
                    }
                }
            }
        }
    }
}

// ── Local composables ─────────────────────────────────────────────────────────

/** Album en disque 132 dp : pochette en étiquette + nom + nombre de pistes. */
@Composable
private fun AlbumRecord(
    album: ArtistAlbum,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VinylDisc(
            artworkUrl = album.artworkUrl,
            labelKey = album.id,
            labelFraction = 0.56f,
            modifier = Modifier.size(132.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text       = album.name,
            style      = MaterialTheme.typography.titleSmall,
            color      = MaterialTheme.colorScheme.onSurface,
            textAlign  = TextAlign.Center,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis,
        )
        if (album.trackCount > 0) {
            Text(
                text  = "${album.trackCount} track${if (album.trackCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

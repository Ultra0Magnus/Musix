package com.louis.musix.ui.screens.artist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.louis.musix.domain.model.ArtistAlbum
import com.louis.musix.domain.model.Song
import com.louis.musix.ui.components.SongRow
import org.koin.androidx.compose.koinViewModel

/**
 * Page artiste — deux sections :
 *  1. Titres populaires (top 20 résultats YouTube Music)
 *  2. Albums (playlist YouTube Music de type "music_albums")
 *
 * @param artistName   Nom de l'artiste transmis par la navigation.
 * @param onSongClick  Lance la lecture d'un morceau.
 * @param onAlbumClick Ouvre le détail d'un album.
 * @param onBack       Retour arrière.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artistName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {

            // ── Section "Titres populaires" ───────────────────────────────────

            item {
                SectionTitle("Titres populaires")
            }

            when {
                state.isLoadingSongs -> item {
                    LinearProgressIndicator(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp))
                }
                state.songsError != null -> item {
                    Text(
                        text    = state.songsError!!,
                        style   = MaterialTheme.typography.bodySmall,
                        color   = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                state.topSongs.isEmpty() -> item {
                    Text(
                        text    = "Aucun titre trouvé",
                        style   = MaterialTheme.typography.bodyMedium,
                        color   = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
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
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        )
                    }
                }
            }

            // ── Section "Albums" ──────────────────────────────────────────────

            item { Spacer(Modifier.height(24.dp)) }

            item {
                SectionTitle("Albums")
            }

            when {
                state.isLoadingAlbums -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
                state.albums.isEmpty() -> item {
                    Text(
                        text    = "Aucun album trouvé",
                        style   = MaterialTheme.typography.bodyMedium,
                        color   = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                else -> item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = state.albums, key = { it.id }) { album ->
                            AlbumCard(album = album, onClick = { onAlbumClick(album) })
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Composants locaux ─────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

/**
 * Carte d'album cliquable : artwork carré 130dp + nom + nombre de pistes.
 */
@Composable
private fun AlbumCard(
    album: ArtistAlbum,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() }
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        // Artwork
        AsyncImage(
            model              = album.artworkUrl,
            contentDescription = album.name,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        Spacer(Modifier.height(6.dp))

        // Nom de l'album
        Text(
            text     = album.name,
            style    = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color    = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // Nombre de pistes
        if (album.trackCount > 0) {
            Text(
                text  = "${album.trackCount} titre${if (album.trackCount > 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

package com.louis.musix.ui.screens.home

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.louis.musix.domain.model.Song
import com.louis.musix.ui.components.ScreenTitle
import com.louis.musix.ui.components.SectionLabel
import com.louis.musix.ui.components.ShimmerBox
import com.louis.musix.ui.components.SongRow
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onSongClick: (Song) -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val vm: HomeViewModel = koinViewModel()
    val recentlyPlayed by vm.recentlyPlayed.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ─── Header : MUSIX (Anton) + sous-titre ──────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MUSIX",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "YOUR SOUND, NONSTOP",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // ─── Reprendre : bloc noir tranché (dernier écouté) ───────────────────
        recentlyPlayed.firstOrNull()?.let { song ->
            item { ResumeBlock(song = song, onClick = onSongClick) }
        }

        // ─── Favoris : grille 2 colonnes ──────────────────────────────────────
        if (favorites.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionLabel("Favorites")
                    FavoritesGrid(songs = favorites.take(6), onSongClick = onSongClick)
                }
            }
        }

        // ─── Suggestions : lignes ─────────────────────────────────────────────
        when (val state = suggestions) {
            is SuggestionsState.Loading -> item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Suggestions")
                    ShimmerCarousel()
                }
            }
            is SuggestionsState.Success -> item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionLabel("Suggestions")
                    Spacer(Modifier.height(8.dp))
                    state.songs.take(8).forEach { song ->
                        SongRow(song = song, onClick = onSongClick, horizontalPadding = 0.dp)
                    }
                }
            }
            is SuggestionsState.Error -> Unit
            SuggestionsState.Hidden -> Unit
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun ResumeBlock(song: Song, onClick: (Song) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onClick(song) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(92.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "RESUME",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = song.artist.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FavoritesGrid(songs: List<Song>, onSongClick: (Song) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        songs.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                pair.forEach { song ->
                    GridCell(song = song, onClick = onSongClick, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun GridCell(song: Song, onClick: (Song) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable { onClick(song) }) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artist.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ShimmerCarousel() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 0.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(5) {
            Column {
                ShimmerBox(modifier = Modifier.size(140.dp))
                Spacer(Modifier.height(8.dp))
                ShimmerBox(modifier = Modifier.size(width = 100.dp, height = 14.dp))
                Spacer(Modifier.height(4.dp))
                ShimmerBox(modifier = Modifier.size(width = 70.dp, height = 12.dp))
            }
        }
    }
}

package com.louis.musix.ui.screens.home

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.domain.model.Song
import com.louis.musix.ui.components.ShimmerBox
import com.louis.musix.ui.components.SongCard
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
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Hello",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // ─── Continue Listening ───────────────────────────────────────────────
        item {
            Section(
                title = "Continue Listening",
                emptyText = "No recent plays",
                songs = recentlyPlayed,
                onSongClick = onSongClick,
            )
        }

        // ─── Your Favorites ───────────────────────────────────────────────────
        item {
            Section(
                title = "Your Favorites",
                emptyText = "No favorites yet",
                songs = favorites,
                onSongClick = onSongClick,
            )
        }

        // ─── Suggestions ──────────────────────────────────────────────────────
        when (val state = suggestions) {
            is SuggestionsState.Loading -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle("Suggestions")
                        ShimmerCarousel()
                    }
                }
            }
            is SuggestionsState.Success -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val artistsText = state.artists.joinToString(", ")
                        SectionTitle("Because you listen to $artistsText")
                        SongCarousel(state.songs, onSongClick)
                    }
                }
            }
            is SuggestionsState.Error -> {
                // Optionally show error or hide
            }
            SuggestionsState.Hidden -> {
                // Do nothing
            }
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun Section(
    title: String,
    emptyText: String,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(title)
        if (songs.isEmpty()) {
            EmptySection(emptyText)
        } else {
            SongCarousel(songs, onSongClick)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun SongCarousel(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(songs, key = { it.id }) { song ->
            SongCard(song = song, onClick = onSongClick)
        }
    }
}

@Composable
private fun ShimmerCarousel() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
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

@Composable
private fun EmptySection(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

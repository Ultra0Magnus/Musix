package com.louis.musix.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.domain.model.Song
import com.louis.musix.ui.components.RoundIconButton
import com.louis.musix.ui.components.ScreenTitle
import com.louis.musix.ui.components.SectionTitle
import com.louis.musix.ui.components.ShimmerBox
import com.louis.musix.ui.components.SongRow
import com.louis.musix.ui.components.VinylDisc
import com.louis.musix.ui.theme.PillShape
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onSongClick: (Song) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSeeAllFavorites: () -> Unit = {},
) {
    val vm: HomeViewModel = koinViewModel()
    val recentlyPlayed by vm.recentlyPlayed.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        // ─── En-tête : « Musix » (Fraunces italique) + réglages rond ──────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ScreenTitle("Musix")
                    Text(
                        text = "Your sound, nonstop",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RoundIconButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    onClick = onSettingsClick,
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            }
        }

        // ─── Reprendre : carte avec disque qui déborde à gauche ───────────────
        recentlyPlayed.firstOrNull()?.let { song ->
            item { ResumeCard(song = song, onClick = onSongClick) }
        }

        // ─── Écoutés récemment : rangée de disques ────────────────────────────
        val recent = recentlyPlayed.distinctBy { it.id }.drop(1)
        if (recent.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Recently played")
                    RecordShelf(songs = recent.take(12), onSongClick = onSongClick)
                }
            }
        }

        // ─── Favoris : rangée de disques + « See all » ─────────────────────────
        if (favorites.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Favorites", actionLabel = "See all", onAction = onSeeAllFavorites)
                    RecordShelf(songs = favorites.take(12), onSongClick = onSongClick)
                }
            }
        }

        // ─── Suggestions : pilules ────────────────────────────────────────────
        when (val state = suggestions) {
            is SuggestionsState.Loading -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Suggestions")
                    repeat(3) {
                        ShimmerBox(
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            cornerRadius = 28.dp,
                        )
                    }
                }
            }
            is SuggestionsState.Success -> item {
                Column {
                    SectionTitle("Suggestions", modifier = Modifier.padding(bottom = 8.dp))
                    state.songs.take(8).forEach { song ->
                        SongRow(song = song, onClick = onSongClick, horizontalPadding = 0.dp)
                    }
                }
            }
            is SuggestionsState.Error -> Unit
            SuggestionsState.Hidden -> Unit
        }

        if (recentlyPlayed.isEmpty() && favorites.isEmpty() && suggestions == SuggestionsState.Hidden) {
            item { EmptyHome() }
        }
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun ResumeCard(song: Song, onClick: (Song) -> Unit) {
    val discSize = 168.dp
    val bleed = 46.dp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick(song) },
    ) {
        VinylDisc(
            artworkUrl = song.thumbnailUrl,
            labelKey = song.id,
            labelFraction = 0.5f,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = -bleed)
                .size(discSize),
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = discSize - bleed + 18.dp, end = 18.dp),
        ) {
            Text(
                text = "Jump back in",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.inverseSurface)
                    .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Play",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
    }
}

/** Rangée horizontale de disques 74 dp avec titre en dessous. */
@Composable
private fun RecordShelf(songs: List<Song>, onSongClick: (Song) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(songs, key = { it.id }) { song ->
            Column(
                modifier = Modifier
                    .width(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSongClick(song) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                VinylDisc(
                    artworkUrl = song.thumbnailUrl,
                    labelKey = song.id,
                    labelFraction = 0.56f,
                    modifier = Modifier.size(74.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun EmptyHome() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        VinylDisc(artworkUrl = null, labelKey = "musix", modifier = Modifier.size(160.dp).clip(CircleShape))
        Text(
            text = "Nothing on the turntable yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Search for a song to start listening.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

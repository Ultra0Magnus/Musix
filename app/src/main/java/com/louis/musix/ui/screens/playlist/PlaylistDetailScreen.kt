package com.louis.musix.ui.screens.playlist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.domain.model.Song
import com.louis.musix.ui.components.PageHeader
import com.louis.musix.ui.components.PillButton
import com.louis.musix.ui.components.SongRow
import com.louis.musix.ui.components.VinylDisc
import com.louis.musix.ui.theme.PillShape
import com.louis.musix.ui.theme.SillonOnAccent
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    /** [songs] = full playlist, [startIndex] = index of the tapped track. */
    onSongClick: (songs: List<Song>, startIndex: Int) -> Unit,
) {
    val viewModel: PlaylistDetailViewModel = koinViewModel(parameters = { parametersOf(playlistId) })
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val name = state.playlistName.ifEmpty { "Playlist" }

    Column(modifier = Modifier.fillMaxSize()) {
        PageHeader(onBack = onBack, label = "Playlist")

        // ── Hero : carte disque + nom + actions (hors LazyColumn → index 0-based) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VinylDisc(
                artworkUrl = state.songs.firstOrNull()?.thumbnailUrl,
                labelKey = name,
                labelFraction = 0.5f,
                modifier = Modifier.size(112.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${state.songs.size} song${if (state.songs.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.songs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Empty playlist\nAdd songs from the search tab",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PillButton(
                    text = "Play all",
                    icon = Icons.Outlined.PlayArrow,
                    onClick = { onSongClick(state.songs, 0) },
                    modifier = Modifier.weight(1f),
                )
                PillButton(
                    text = "Shuffle",
                    icon = Icons.Outlined.Shuffle,
                    outlined = true,
                    onClick = { onSongClick(state.songs.shuffled(), 0) },
                    modifier = Modifier.weight(1f),
                )
            }

            // ── Reorderable list ──────────────────────────────────────────────
            val lazyListState = rememberLazyListState()
            val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                viewModel.moveSong(from.index, to.index)
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                itemsIndexed(
                    items = state.songs,
                    key   = { _, song -> song.id },
                ) { index, song ->

                    ReorderableItem(reorderableState, key = song.id) { isDragging ->
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 0.dp,
                            label = "drag_shadow",
                        )

                        // Swipe-to-delete (horizontal) + drag handle (vertical) coexist
                        val dismissState = rememberSwipeToDismissBoxState(
                            positionalThreshold = { it * 0.5f },
                        )
                        LaunchedEffect(dismissState.currentValue) {
                            if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.removeSong(song.id)
                            }
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier.shadow(elevation, PillShape),
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
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        tint = SillonOnAccent,
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false,
                        ) {
                            SongRow(
                                song    = song,
                                onClick = { onSongClick(state.songs, index) },
                                trailing = {
                                    // Drag handle — vertical reorder only
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .draggableHandle(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Outlined.DragHandle,
                                            contentDescription = "Drag to reorder",
                                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier           = Modifier.size(22.dp),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.louis.musix.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.louis.musix.domain.model.Song
import com.louis.musix.domain.model.formatDuration
import com.louis.musix.player.RepeatMode
import com.louis.musix.ui.components.OutlineSquareButton
import com.louis.musix.ui.components.SquarePlayButton
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
) {
    val viewModel: PlayerViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val background = MaterialTheme.colorScheme.background

    // ── Bottom sheets ─────────────────────────────────────────────────────────
    var showQueue  by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }

    if (showSleepTimer) {
        SleepTimerBottomSheet(
            isActive          = state.sleepTimerEndMs != null || state.sleepTimerEndOfTrack,
            onPick            = { minutes -> viewModel.setSleepTimer(minutes); showSleepTimer = false },
            onPickEndOfTrack  = { viewModel.setSleepTimerEndOfTrack(); showSleepTimer = false },
            onCancel          = { viewModel.cancelSleepTimer(); showSleepTimer = false },
            onDismiss         = { showSleepTimer = false },
        )
    }

    if (showQueue) {
        QueueBottomSheet(
            queue        = state.queue,
            currentIndex = state.currentQueueIndex,
            onRemove     = viewModel::removeFromQueue,
            onDismiss    = { showQueue = false },
        )
    }

    if (showLyrics) {
        LyricsBottomSheet(
            lyricsState       = state.lyricsState,
            currentPositionMs = state.currentPositionMs,
            onDismiss         = { showLyrics = false },
        )
    }

    // ── Main screen (flat béton background) ─────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Top bar : ✕/▾  ·  EN LECTURE  ·  timer  ≡ ─────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlineSquareButton(
                icon = Icons.Outlined.ExpandMore,
                contentDescription = "Close",
                onClick = onBack,
                size = 34.dp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))

            // Live sleep-timer countdown
            if (state.sleepTimerEndMs != null) {
                var nowMs by remember { mutableStateOf(0L) }
                LaunchedEffect(state.sleepTimerEndMs) {
                    while (true) {
                        nowMs = System.currentTimeMillis()
                        kotlinx.coroutines.delay(1000)
                    }
                }
                val remaining = (((state.sleepTimerEndMs ?: 0L) - nowMs) / 1000).coerceAtLeast(0)
                if (nowMs > 0L) {
                    Text(
                        text = formatDuration(remaining),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            OutlineSquareButton(
                icon = if (state.sleepTimerEndMs != null || state.sleepTimerEndOfTrack)
                    Icons.Filled.Bedtime else Icons.Outlined.Bedtime,
                contentDescription = "Sleep timer",
                onClick = { showSleepTimer = true },
                size = 34.dp,
                active = state.sleepTimerEndMs != null || state.sleepTimerEndOfTrack,
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Artwork (carré) ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (state.song != null) {
                AsyncImage(
                    model = state.song!!.thumbnailUrl,
                    contentDescription = "Artwork",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (state.isLoadingAudio) {
                Box(
                    modifier = Modifier.fillMaxSize().background(background.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Title (Anton) + artist ────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = (state.song?.title ?: "Loading…").uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            val artistName = state.song?.artist ?: ""
            Text(
                text = artistName.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(enabled = artistName.isNotEmpty()) {
                    onArtistClick(artistName)
                },
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Favorite | Download (btnout carrés) ───────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlineSquareButton(
                icon = if (state.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (state.isFavorite) "Remove from favorites" else "Add to favorites",
                onClick = { if (state.song != null) viewModel.toggleFavorite() },
                size = 38.dp,
                active = state.isFavorite,
            )
            val isDownloaded = state.song?.isDownloaded == true
            OutlineSquareButton(
                icon = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Outlined.DownloadForOffline,
                contentDescription = if (isDownloaded) "Remove download" else "Download",
                onClick = { if (state.song != null) viewModel.toggleDownload() },
                size = 38.dp,
                active = isDownloaded,
            )
            Spacer(Modifier.weight(1f))
            if (state.song?.isDownloaded == true) {
                Text(
                    text = "DOWNLOADED ✓",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.error!!, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(16.dp))

        // ── Progress slider ───────────────────────────────────────────────────
        val progress = if (state.durationMs > 0)
            state.currentPositionMs.toFloat() / state.durationMs.toFloat() else 0f

        Slider(
            value = progress,
            onValueChange = { viewModel.seekTo((it * state.durationMs).toLong()) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.inverseSurface,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(state.currentPositionMs / 1000),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDuration(state.durationMs / 1000),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(12.dp))

        // ── Main controls ─────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::skipToPrevious) {
                Icon(Icons.Outlined.SkipPrevious, "Previous", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            SquarePlayButton(
                isPlaying = state.isPlaying,
                onClick = viewModel::togglePlayPause,
                size = 64.dp,
                enabled = !state.isLoadingAudio,
            )
            IconButton(onClick = viewModel::skipToNext) {
                Icon(Icons.Outlined.SkipNext, "Next", modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Shuffle | Queue | Repeat ──────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::toggleShuffle) {
                Icon(
                    Icons.Outlined.Shuffle,
                    "Shuffle",
                    modifier = Modifier.size(24.dp),
                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            TextButton(onClick = { showQueue = true }) {
                Icon(Icons.Outlined.QueueMusic, null, modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(4.dp))
                val remaining = (state.queueSize - state.currentQueueIndex - 1).coerceAtLeast(0)
                Text(
                    text = if (remaining > 0) "$remaining UP NEXT" else "QUEUE EMPTY",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            IconButton(onClick = viewModel::cycleRepeatMode) {
                Icon(
                    imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                    contentDescription = "Repeat",
                    modifier = Modifier.size(24.dp),
                    tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ── Lyrics ────────────────────────────────────────────────────────────
        TextButton(
            onClick = { showLyrics = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = when (state.lyricsState) {
                    is LyricsUiState.Synced       -> "SYNCED LYRICS ›"
                    is LyricsUiState.Plain        -> "LYRICS ›"
                    LyricsUiState.Instrumental    -> "INSTRUMENTAL TRACK"
                    LyricsUiState.Loading         -> "LOADING LYRICS…"
                    LyricsUiState.NotFound, LyricsUiState.Idle -> "LYRICS UNAVAILABLE"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when (state.lyricsState) {
                    is LyricsUiState.Synced, is LyricsUiState.Plain ->
                        MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ─── Sleep Timer Bottom Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerBottomSheet(
    isActive: Boolean,
    onPick: (Int) -> Unit,
    onPickEndOfTrack: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "SLEEP TIMER",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            listOf(15, 30, 45, 60).forEach { minutes ->
                SleepTimerOption("$minutes minutes") { onPick(minutes) }
            }
            SleepTimerOption("End of current track") { onPickEndOfTrack() }

            if (isActive) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
                SleepTimerOption("Turn off timer", tint = true) { onCancel() }
            }
        }
    }
}

@Composable
private fun SleepTimerOption(
    label: String,
    tint: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = if (tint) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

// ─── Queue Bottom Sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueBottomSheet(
    queue: List<Song>,
    currentIndex: Int,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "QUEUE — ${queue.size} TRACK${if (queue.size > 1) "S" else ""}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            LazyColumn {
                itemsIndexed(items = queue, key = { i, s -> "${i}_${s.id}" }) { index, song ->
                    val isCurrent = index == currentIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.surfaceVariant
                                else Color.Transparent
                            )
                            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artist.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (!isCurrent) {
                            IconButton(onClick = { onRemove(index) }) {
                                Icon(Icons.Outlined.Close, "Remove from queue",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Lyrics Bottom Sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsBottomSheet(
    lyricsState: LyricsUiState,
    currentPositionMs: Long,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            when (val s = lyricsState) {
                LyricsUiState.Loading -> {
                    Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                LyricsUiState.NotFound, LyricsUiState.Idle -> {
                    Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                        Text("No lyrics found for this track.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }

                LyricsUiState.Instrumental -> {
                    Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                        Text("🎵 Instrumental track",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                is LyricsUiState.Plain -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = s.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4f,
                        )
                    }
                }

                is LyricsUiState.Synced -> {
                    val lines = s.lines
                    val listState = rememberLazyListState()
                    val scope = rememberCoroutineScope()

                    val currentLineIndex by remember(currentPositionMs, lines) {
                        derivedStateOf {
                            lines.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
                        }
                    }

                    LaunchedEffect(currentLineIndex) {
                        scope.launch {
                            listState.animateScrollToItem(
                                index = (currentLineIndex - 2).coerceAtLeast(0),
                            )
                        }
                    }

                    LazyColumn(
                        state    = listState,
                        modifier = Modifier.fillMaxWidth().height(480.dp),
                    ) {
                        itemsIndexed(lines) { index, line ->
                            val isCurrent = index == currentLineIndex
                            if (isCurrent) {
                                // Surligneur vert (« hl ») sur la ligne active
                                Box(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Text(
                                        text = line.text,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            } else {
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

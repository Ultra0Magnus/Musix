package com.louis.musix.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.domain.model.Song
import com.louis.musix.domain.model.formatDuration
import com.louis.musix.player.RepeatMode
import com.louis.musix.ui.components.PillChip
import com.louis.musix.ui.components.PlayPauseButton
import com.louis.musix.ui.components.ProgressRing
import com.louis.musix.ui.components.RoundIconButton
import com.louis.musix.ui.components.SheetRow
import com.louis.musix.ui.components.SillonBottomSheet
import com.louis.musix.ui.components.VinylDisc
import com.louis.musix.ui.theme.CardShape
import com.louis.musix.ui.theme.PillShape
import com.louis.musix.ui.theme.SillonDim
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit = {},
) {
    val viewModel: PlayerViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // ── Bottom sheets ─────────────────────────────────────────────────────────
    var showQueue  by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }

    val sleepActive = state.sleepTimerEndMs != null || state.sleepTimerEndOfTrack

    if (showSleepTimer) {
        SleepTimerBottomSheet(
            isActive          = sleepActive,
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

    // Live sleep-timer countdown
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.sleepTimerEndMs) {
        while (state.sleepTimerEndMs != null) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }
    val timerLabel = when {
        state.sleepTimerEndMs != null ->
            formatDuration((((state.sleepTimerEndMs ?: 0L) - nowMs) / 1000).coerceAtLeast(0))
        state.sleepTimerEndOfTrack -> "End of track"
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))

        // ── Top bar : ▾ · « Now playing · n of N » · file ─────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(
                icon = Icons.Outlined.ExpandMore,
                contentDescription = "Close",
                onClick = onBack,
                containerColor = MaterialTheme.colorScheme.surface,
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Now playing",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (state.queueSize > 1) {
                    Text(
                        text = "${state.currentQueueIndex + 1} of ${state.queueSize}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            RoundIconButton(
                icon = Icons.Outlined.QueueMusic,
                contentDescription = "Queue",
                onClick = { showQueue = true },
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Disque qui tourne + anneau de progression ─────────────────────────
        val progress = if (state.durationMs > 0)
            state.currentPositionMs.toFloat() / state.durationMs.toFloat() else 0f

        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val ringSize = minOf(maxWidth * 0.84f, 320.dp)
            Box(Modifier.size(ringSize), contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = progress,
                    modifier = Modifier.size(ringSize),
                    strokeWidth = 5.dp,
                )
                VinylDisc(
                    artworkUrl = state.song?.thumbnailUrl,
                    labelKey = state.song?.id,
                    labelFraction = 0.46f,
                    spinning = state.isPlaying,
                    modifier = Modifier.size(ringSize - 18.dp),
                )
                if (state.isLoadingAudio) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary) }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // ── Titre (Fraunces) + artiste (moutarde, cliquable) ──────────────────
        Text(
            text = state.song?.title ?: "Loading…",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        val artistName = state.song?.artist ?: ""
        Text(
            text = artistName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(PillShape)
                .clickable(enabled = artistName.isNotEmpty()) { onArtistClick(artistName) }
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )

        if (state.error != null) {
            Spacer(Modifier.height(6.dp))
            Text(state.error!!, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(10.dp))

        // ── Slider + temps ────────────────────────────────────────────────────
        Slider(
            value = progress,
            onValueChange = { viewModel.seekTo((it * state.durationMs).toLong()) },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(state.currentPositionMs / 1000),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val remainingSec = ((state.durationMs - state.currentPositionMs) / 1000).coerceAtLeast(0)
            Text(if (state.durationMs > 0) "−${formatDuration(remainingSec)}" else "",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(14.dp))

        // ── Commandes : ⏮ (56) · ▶ (80 moutarde) · ⏭ (56) ─────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            RoundIconButton(
                icon = Icons.Outlined.SkipPrevious,
                contentDescription = "Previous",
                onClick = viewModel::skipToPrevious,
                size = 58.dp,
                containerColor = MaterialTheme.colorScheme.surface,
            )
            PlayPauseButton(
                isPlaying = state.isPlaying,
                onClick = viewModel::togglePlayPause,
                size = 80.dp,
                isLoading = state.isLoadingAudio,
            )
            RoundIconButton(
                icon = Icons.Outlined.SkipNext,
                contentDescription = "Next",
                onClick = viewModel::skipToNext,
                size = 58.dp,
                containerColor = MaterialTheme.colorScheme.surface,
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Chips : aléatoire · répéter · favori · télécharger · minuteur ─────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            PillChip(
                text = "Shuffle",
                icon = Icons.Outlined.Shuffle,
                selected = state.shuffleEnabled,
                onClick = viewModel::toggleShuffle,
            )
            PillChip(
                text = when (state.repeatMode) {
                    RepeatMode.ONE -> "Repeat one"
                    else -> "Repeat"
                },
                icon = if (state.repeatMode == RepeatMode.ONE) Icons.Outlined.RepeatOne else Icons.Outlined.Repeat,
                selected = state.repeatMode != RepeatMode.OFF,
                onClick = viewModel::cycleRepeatMode,
            )
            PillChip(
                text = if (state.isFavorite) "Liked" else "Like",
                icon = if (state.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                selected = state.isFavorite,
                onClick = { if (state.song != null) viewModel.toggleFavorite() },
            )
            val isDownloaded = state.song?.isDownloaded == true
            PillChip(
                text = if (isDownloaded) "Downloaded" else "Download",
                icon = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Outlined.DownloadForOffline,
                selected = isDownloaded,
                onClick = { if (state.song != null) viewModel.toggleDownload() },
            )
            PillChip(
                text = timerLabel ?: "Sleep",
                icon = if (sleepActive) Icons.Outlined.Timer else Icons.Outlined.Bedtime,
                selected = sleepActive,
                onClick = { showSleepTimer = true },
            )
        }

        Spacer(Modifier.height(18.dp))

        // ── Carte paroles ─────────────────────────────────────────────────────
        LyricsCard(
            lyricsState = state.lyricsState,
            currentPositionMs = state.currentPositionMs,
            onClick = { showLyrics = true },
        )

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Lyrics preview card ──────────────────────────────────────────────────────

@Composable
private fun LyricsCard(
    lyricsState: LyricsUiState,
    currentPositionMs: Long,
    onClick: () -> Unit,
) {
    val hasLyrics = lyricsState is LyricsUiState.Synced || lyricsState is LyricsUiState.Plain
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = hasLyrics, onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Lyrics",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (hasLyrics) {
                Text(
                    text = if (lyricsState is LyricsUiState.Synced) "Synced · Open" else "Open",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        when (lyricsState) {
            is LyricsUiState.Synced -> {
                val lines = lyricsState.lines
                val current = lines.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
                lines.getOrNull(current - 1)?.let { LyricPreviewLine(it.text, active = false) }
                lines.getOrNull(current)?.let { LyricPreviewLine(it.text, active = true) }
                lines.getOrNull(current + 1)?.let { LyricPreviewLine(it.text, active = false) }
            }
            is LyricsUiState.Plain -> {
                lyricsState.text.lineSequence().filter { it.isNotBlank() }.take(3).forEach {
                    LyricPreviewLine(it, active = false)
                }
            }
            LyricsUiState.Loading -> LyricStatus("Loading lyrics…")
            LyricsUiState.Instrumental -> LyricStatus("Instrumental track")
            LyricsUiState.NotFound, LyricsUiState.Idle -> LyricStatus("Lyrics unavailable for this track")
        }
    }
}

@Composable
private fun LyricPreviewLine(text: String, active: Boolean) {
    if (active) {
        Text(
            text = text.ifBlank { "♪" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    } else {
        Text(
            text = text.ifBlank { "♪" },
            style = MaterialTheme.typography.bodyMedium,
            color = SillonDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun LyricStatus(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ─── Sleep Timer Bottom Sheet ─────────────────────────────────────────────────

@Composable
private fun SleepTimerBottomSheet(
    isActive: Boolean,
    onPick: (Int) -> Unit,
    onPickEndOfTrack: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    SillonBottomSheet(onDismiss = onDismiss, title = "Sleep timer", subtitle = "Pause playback after…") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(15, 30, 45, 60).forEach { minutes ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(CardShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPick(minutes) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$minutes", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface)
                        Text("min", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        SheetRow(label = "End of current track", icon = Icons.Outlined.Bedtime, onClick = onPickEndOfTrack)
        if (isActive) {
            SheetRow(label = "Turn off timer", icon = Icons.Outlined.Close, danger = true, onClick = onCancel)
        }
    }
}

// ─── Queue Bottom Sheet ───────────────────────────────────────────────────────

@Composable
private fun QueueBottomSheet(
    queue: List<Song>,
    currentIndex: Int,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    SillonBottomSheet(
        onDismiss = onDismiss,
        title = "Up next",
        subtitle = "${queue.size} track${if (queue.size > 1) "s" else ""} in the queue",
    ) {
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex.coerceAtLeast(0))
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().height(440.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(items = queue, key = { i, s -> "${i}_${s.id}" }) { index, song ->
                val isCurrent = index == currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VinylDisc(
                        artworkUrl = song.thumbnailUrl,
                        labelKey = song.id,
                        labelFraction = 0.6f,
                        spinning = false,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (isCurrent) "Playing now · ${song.artist}" else song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!isCurrent) {
                        RoundIconButton(
                            icon = Icons.Outlined.Close,
                            contentDescription = "Remove from queue",
                            onClick = { onRemove(index) },
                            size = 36.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}

// ─── Lyrics Bottom Sheet ──────────────────────────────────────────────────────

@Composable
private fun LyricsBottomSheet(
    lyricsState: LyricsUiState,
    currentPositionMs: Long,
    onDismiss: () -> Unit,
) {
    SillonBottomSheet(onDismiss = onDismiss, title = "Lyrics", skipPartiallyExpanded = true) {
        when (val s = lyricsState) {
            LyricsUiState.Loading -> {
                Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                    Text("Instrumental track",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            is LyricsUiState.Plain -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
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

                val currentLineIndex by remember(currentPositionMs, lines) {
                    derivedStateOf {
                        lines.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
                    }
                }

                LaunchedEffect(currentLineIndex) {
                    listState.animateScrollToItem(index = (currentLineIndex - 2).coerceAtLeast(0))
                }

                LazyColumn(
                    state    = listState,
                    modifier = Modifier.fillMaxWidth().height(520.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(lines) { index, line ->
                        val isCurrent = index == currentLineIndex
                        if (isCurrent) {
                            Box(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = line.text.ifBlank { "♪" },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                )
                            }
                        } else {
                            Text(
                                text = line.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (index < currentLineIndex) SillonDim.copy(alpha = 0.6f) else SillonDim,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

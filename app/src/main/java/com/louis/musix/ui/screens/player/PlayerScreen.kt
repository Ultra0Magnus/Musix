package com.louis.musix.ui.screens.player

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.louis.musix.domain.model.Song
import com.louis.musix.domain.model.formatDuration
import com.louis.musix.player.RepeatMode
import com.louis.musix.ui.components.SongRow
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
    val context = LocalContext.current

    // ── Artwork color gradient (artwork → background) ─────────────────────────
    val background = MaterialTheme.colorScheme.background
    var dominantColor by remember { mutableStateOf(background) }
    val animatedDominant by animateColorAsState(targetValue = dominantColor, label = "dominant")

    LaunchedEffect(state.song?.thumbnailUrl) {
        val url = state.song?.thumbnailUrl ?: return@LaunchedEffect
        val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
        val result  = context.imageLoader.execute(request)
        if (result is SuccessResult) {
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
            Palette.from(bitmap).generate { palette ->
                val rgb = palette?.darkVibrantSwatch?.rgb
                    ?: palette?.darkMutedSwatch?.rgb
                    ?: palette?.dominantSwatch?.rgb
                if (rgb != null) dominantColor = Color(rgb)
            }
        }
    }

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

    // ── Main screen ───────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(animatedDominant.copy(alpha = 0.6f), background)))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBackIosNew, "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.weight(1f))

            // Live sleep-timer countdown chip
            val timerActive = state.sleepTimerEndMs != null || state.sleepTimerEndOfTrack
            if (state.sleepTimerEndMs != null) {
                var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
                LaunchedEffect(state.sleepTimerEndMs) {
                    while (true) {
                        nowMs = System.currentTimeMillis()
                        kotlinx.coroutines.delay(1000)
                    }
                }
                val remaining = ((state.sleepTimerEndMs!! - nowMs) / 1000).coerceAtLeast(0)
                Text(
                    text = formatDuration(remaining),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { showSleepTimer = true }) {
                Icon(
                    imageVector = if (timerActive) Icons.Filled.Bedtime else Icons.Outlined.Bedtime,
                    contentDescription = "Sleep timer",
                    tint = if (timerActive) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Artwork
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
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

        Spacer(Modifier.height(24.dp))

        // Title + artist + favorite + download
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.song?.title ?: "Loading...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                val artistName = state.song?.artist ?: ""
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (artistName.isNotEmpty()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(enabled = artistName.isNotEmpty()) {
                        onArtistClick(artistName)
                    },
                )
            }
            
            // Download Button
            IconButton(onClick = viewModel::toggleDownload, enabled = state.song != null) {
                val isDownloaded = state.song?.isDownloaded == true
                Icon(
                    imageVector = if (isDownloaded) Icons.Filled.DownloadDone else Icons.Outlined.DownloadForOffline,
                    contentDescription = if (isDownloaded) "Remove download" else "Download",
                    tint = if (isDownloaded) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }

            // Favorite Button
            IconButton(onClick = viewModel::toggleFavorite, enabled = state.song != null) {
                Icon(
                    imageVector = if (state.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (state.isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (state.isFavorite) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(state.error!!, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(16.dp))

        // Progress slider
        val progress = if (state.durationMs > 0)
            state.currentPositionMs.toFloat() / state.durationMs.toFloat() else 0f

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
            IconButton(
                onClick = viewModel::togglePlayPause,
                modifier = Modifier.size(72.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                enabled = !state.isLoadingAudio,
            ) {
                Icon(
                    if (state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    if (state.isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }
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
            // Shuffle
            IconButton(onClick = viewModel::toggleShuffle) {
                Icon(
                    Icons.Outlined.Shuffle,
                    "Shuffle",
                    modifier = Modifier.size(24.dp),
                    tint = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Queue (remaining tracks)
            TextButton(onClick = { showQueue = true }) {
                Icon(Icons.Outlined.QueueMusic, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                val remaining = (state.queueSize - state.currentQueueIndex - 1).coerceAtLeast(0)
                Text(
                    text = if (remaining > 0) "$remaining next" else "Queue empty",
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            // Repeat
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
                    is LyricsUiState.Synced       -> "Synced Lyrics ›"
                    is LyricsUiState.Plain        -> "Lyrics ›"
                    LyricsUiState.Instrumental    -> "Instrumental track"
                    LyricsUiState.Loading         -> "Loading lyrics…"
                    LyricsUiState.NotFound, LyricsUiState.Idle -> "Lyrics unavailable"
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

// ─── Queue Bottom Sheet ───────────────────────────────────────────────────────

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
                text = "Sleep timer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()

            listOf(15, 30, 45, 60).forEach { minutes ->
                SleepTimerOption("$minutes minutes") { onPick(minutes) }
            }
            SleepTimerOption("End of current track") { onPickEndOfTrack() }

            if (isActive) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
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
                text = "Queue — ${queue.size} song${if (queue.size > 1) "s" else ""}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()

            LazyColumn {
                itemsIndexed(items = queue, key = { i, s -> "${i}_${s.id}" }) { index, song ->
                    val isCurrent = index == currentIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else Color.Transparent
                            )
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,
                        )
                    }
                }

                is LyricsUiState.Synced -> {
                    val lines = s.lines
                    val listState = rememberLazyListState()
                    val scope = rememberCoroutineScope()

                    // Current line index based on playback position
                    val currentLineIndex by remember(currentPositionMs, lines) {
                        derivedStateOf {
                            lines.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
                        }
                    }

                    // Auto-scroll to current line
                    LaunchedEffect(currentLineIndex) {
                        scope.launch {
                            listState.animateScrollToItem(
                                index  = (currentLineIndex - 2).coerceAtLeast(0),
                            )
                        }
                    }

                    LazyColumn(
                        state    = listState,
                        modifier = Modifier.fillMaxWidth().height(480.dp),
                    ) {
                        itemsIndexed(lines) { index, line ->
                            val isCurrent = index == currentLineIndex
                            Text(
                                text = line.text,
                                style = if (isCurrent) MaterialTheme.typography.bodyLarge
                                        else MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.3f,
                            )
                        }
                    }
                }
            }
        }
    }
}

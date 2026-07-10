package com.louis.musix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.louis.musix.player.PlayerController
import org.koin.compose.koinInject

/**
 * Compact playback bar displayed above the BottomNavigation.
 *
 * Visible only when [PlayerControllerState.hasActiveMedia] is true
 * and the full player screen is not active.
 *
 * @param onTap  Navigates to the full-screen player.
 */
@Composable
fun MiniPlayer(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    playerController: PlayerController = koinInject(),
) {
    val state by playerController.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onTap),
    ) {
        // ── Progress bar (thin, green fill, at the top edge) ─────────────────
        if (state.durationMs > 0) {
            ThinProgressBar(
                fraction = (state.currentPositionMs.toFloat() / state.durationMs),
                height = 2.dp,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            // ── Artwork ───────────────────────────────────────────────────────
            AsyncImage(
                model = state.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            // ── Title + artist ────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = state.artist.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // ── Play / Pause button (square, ink) ─────────────────────────────
            Spacer(Modifier.width(4.dp))
            SquarePlayButton(
                isPlaying = state.isPlaying,
                onClick = playerController::togglePlayPause,
                size = 40.dp,
            )
        }
    }
}

package com.louis.musix.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.louis.musix.player.PlayerController
import com.louis.musix.ui.theme.PillShape
import com.louis.musix.ui.theme.SillonLine
import org.koin.compose.koinInject

/**
 * Mini-lecteur « Sillon » : pilule crème flottante au-dessus de la barre d'onglets.
 * Disque qui tourne entouré d'un anneau de progression, titre/artiste, bouton play rond sombre.
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
    val cream = MaterialTheme.colorScheme.inverseSurface
    val dark = MaterialTheme.colorScheme.inverseOnSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(PillShape)
            .background(cream)
            .clickable(onClick = onTap)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Disque + anneau de progression ────────────────────────────────────
        Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = if (state.durationMs > 0) state.currentPositionMs.toFloat() / state.durationMs else 0f,
                modifier = Modifier.size(50.dp),
                strokeWidth = 3.dp,
                color = dark,
                trackColor = SillonLine.copy(alpha = 0.25f),
            )
            VinylDisc(
                artworkUrl = state.artworkUri,
                labelKey = state.currentSong?.id,
                labelFraction = 0.55f,
                spinning = state.isPlaying,
                modifier = Modifier.size(42.dp),
            )
        }

        // ── Titre + artiste ───────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = dark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = state.artist,
                style = MaterialTheme.typography.bodySmall,
                color = SillonLine,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // ── Play / Pause rond sombre ──────────────────────────────────────────
        PlayPauseButton(
            isPlaying = state.isPlaying,
            onClick = playerController::togglePlayPause,
            size = 46.dp,
            containerColor = dark,
            contentColor = cream,
        )
    }
}

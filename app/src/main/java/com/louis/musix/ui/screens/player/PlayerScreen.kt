package com.louis.musix.ui.screens.player

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.louis.musix.domain.model.formatDuration
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
) {
    val viewModel: PlayerViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Couleur dominante extraite de l'artwork (transition douce)
    val background = MaterialTheme.colorScheme.background
    var dominantColor by remember { mutableStateOf(background) }
    val animatedDominant by animateColorAsState(
        targetValue = dominantColor,
        label = "dominant-color",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedDominant.copy(alpha = 0.6f),
                        background,
                    ),
                )
            )
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        // Bouton retour
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Outlined.ArrowBackIosNew,
                    contentDescription = "Retour",
                    tint = MaterialTheme.colorScheme.onBackground,
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
                    contentDescription = "Couverture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { successState ->
                        // Extrait la couleur dominante de l'artwork
                        val bitmap = (successState.result.drawable as? BitmapDrawable)?.bitmap
                        if (bitmap != null) {
                            Palette.from(bitmap).generate { palette ->
                                val rgb = palette?.darkVibrantSwatch?.rgb
                                    ?: palette?.darkMutedSwatch?.rgb
                                    ?: palette?.dominantSwatch?.rgb
                                if (rgb != null) {
                                    dominantColor = Color(rgb)
                                }
                            }
                        }
                    },
                )
            }
            if (state.isLoadingAudio) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Titre + artiste + bouton favori
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.song?.title ?: "Chargement...",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.song?.artist ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Bouton favori
            IconButton(
                onClick = viewModel::toggleFavorite,
                enabled = state.song != null,
            ) {
                Icon(
                    imageVector = if (state.isFavorite) Icons.Filled.Favorite
                                  else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (state.isFavorite) "Retirer des favoris"
                                         else "Ajouter aux favoris",
                    tint = if (state.isFavorite) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.error!!,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Slider de progression
        val progress = if (state.durationMs > 0) {
            state.currentPositionMs.toFloat() / state.durationMs.toFloat()
        } else 0f

        Slider(
            value = progress,
            onValueChange = { fraction ->
                viewModel.seekTo((fraction * state.durationMs).toLong())
            },
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(state.currentPositionMs / 1000),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(state.durationMs / 1000),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Controles de lecture
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {}) {
                Icon(
                    Icons.Outlined.SkipPrevious,
                    contentDescription = "Precedent",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            IconButton(
                onClick = viewModel::togglePlayPause,
                modifier = Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                enabled = !state.isLoadingAudio,
            ) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (state.isPlaying) "Pause" else "Lecture",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }

            IconButton(onClick = {}) {
                Icon(
                    Icons.Outlined.SkipNext,
                    contentDescription = "Suivant",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

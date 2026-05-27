package com.louis.musix.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.louis.musix.domain.model.Song
import com.louis.musix.domain.model.formatDuration

/**
 * Ligne representant un morceau dans une liste.
 *
 * @param song          Le morceau a afficher.
 * @param onClick       Appele quand l'utilisateur tape sur la ligne.
 * @param onMoreClick   Si non-null, affiche un bouton "..." en fin de ligne.
 */
@Composable
fun SongRow(
    song: Song,
    onClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: ((Song) -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(song) }
            .padding(start = 16.dp, end = if (onMoreClick != null) 4.dp else 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Miniature
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = "Couverture de ${song.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(6.dp)),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Titre + artiste
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Duree
        val duration = formatDuration(song.durationSeconds)
        if (duration.isNotEmpty() && onMoreClick == null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = duration,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Bouton options "..."
        if (onMoreClick != null) {
            IconButton(onClick = { onMoreClick(song) }) {
                Icon(
                    Icons.Outlined.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

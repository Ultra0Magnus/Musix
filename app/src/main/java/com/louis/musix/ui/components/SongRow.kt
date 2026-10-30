package com.louis.musix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.louis.musix.domain.model.Song
import com.louis.musix.domain.model.formatDuration
import com.louis.musix.ui.theme.PillShape

/**
 * Morceau en pilule « Sillon » : petit disque (pochette en étiquette) + titre + « artiste · durée »
 * + bouton d'options rond. Le titre passe en moutarde si le morceau est en cours de lecture.
 *
 * @param song           The track to display.
 * @param onClick        Called when the user taps the row.
 * @param onMoreClick    If non-null, shows a round "…" button at the end of the row.
 * @param onArtistClick  If non-null, the artist name becomes tappable.
 * @param trailing       Optional extra content after the options button (e.g. drag handle).
 */
@Composable
fun SongRow(
    song: Song,
    onClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
    onMoreClick: ((Song) -> Unit)? = null,
    onArtistClick: ((String) -> Unit)? = null,
    horizontalPadding: Dp = 16.dp,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val isCurrent = LocalCurrentSongId.current == song.id
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 4.dp)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick(song) }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VinylDisc(
            artworkUrl = song.thumbnailUrl,
            labelKey = song.id,
            labelFraction = 0.62f,
            modifier = Modifier.size(44.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            val duration = formatDuration(song.durationSeconds)
            Text(
                text = buildAnnotatedString {
                    append(song.artist)
                    if (duration.isNotEmpty()) append(" · $duration")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (onArtistClick != null)
                    Modifier.clickable { onArtistClick(song.artist) }
                else
                    Modifier,
            )
        }

        if (onMoreClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            RoundIconButton(
                icon = Icons.Outlined.MoreHoriz,
                contentDescription = "More options",
                onClick = { onMoreClick(song) },
                size = 40.dp,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (trailing == null) {
            Spacer(modifier = Modifier.width(10.dp))
        }
        trailing?.invoke(this)
    }
}

package com.louis.musix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Composants signature de la direction « Affiche ».
 * Angles droits partout ; le vert (primary) reste réservé aux filets/soulignements/tints.
 */

/** Titre d'écran héros — Anton géant, pleine largeur. */
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
) {
    Text(
        text = text.uppercase(),
        style = style,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Label de section — majuscules espacées + soulignement vert de 2dp (le « slab »). */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.drawBehind {
            val stroke = 2.dp.toPx()
            drawRect(
                color = accent,
                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height + 5.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width, stroke),
            )
        },
    )
}

/** Bouton play/pause carré « encre » (plein clair sur fond sombre). Jamais vert. */
@Composable
fun SquarePlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.inverseSurface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = MaterialTheme.colorScheme.inverseOnSurface,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** Bouton carré à contour (« btnout ») ; actif → contour + tint vert. */
@Composable
fun OutlineSquareButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    active: Boolean = false,
) {
    val stroke = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = modifier
            .size(size)
            .border(1.5.dp, stroke, RectangleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.42f),
        )
    }
}

/** Couleurs « encre » pour un Button M3 plein (jamais vert). À passer via `colors = inkButtonColors()`. */
@Composable
fun inkButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.inverseSurface,
    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
)

/** Barre de progression fine à bouts carrés : piste (outline) + fill (vert). Non interactive. */
@Composable
fun ThinProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

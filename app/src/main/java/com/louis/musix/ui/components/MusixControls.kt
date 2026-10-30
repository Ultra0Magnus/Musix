package com.louis.musix.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.louis.musix.ui.theme.PillShape
import com.louis.musix.ui.theme.SillonGrooveDark
import com.louis.musix.ui.theme.SillonGrooveLight
import com.louis.musix.ui.theme.sillonLabelFor

/**
 * Composants signature de la direction « Sillon ».
 * Tout est rond : disques vinyle, pilules, boutons circulaires, anneaux de progression.
 * La moutarde (primary) marque ce qui est actif ou en cours.
 */

/** Id du morceau en cours de lecture — fourni par MainActivity, lu par [SongRow]. */
val LocalCurrentSongId = compositionLocalOf<String?> { null }

// ─── Disque vinyle ──────────────────────────────────────────────────────────────

/**
 * Disque vinyle : sillons concentriques + étiquette centrale.
 * L'étiquette affiche [artworkUrl] recadrée en cercle, sur fond [labelColor].
 * Si [spinning], le disque tourne (33 tours ≈ 1,8 s par tour) et s'arrête là où il est en pause.
 */
@Composable
fun VinylDisc(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    labelKey: Any? = artworkUrl,
    labelFraction: Float = 0.44f,
    spinning: Boolean = false,
    labelContent: (@Composable () -> Unit)? = null,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (spinning) {
            while (true) {
                val start = rotation.value % 360f
                rotation.snapTo(start)
                rotation.animateTo(start + 360f, tween(1800, easing = LinearEasing))
            }
        }
    }
    val labelColor = sillonLabelFor(labelKey)

    BoxWithConstraints(
        modifier = modifier
            .clip(CircleShape)
            .background(SillonGrooveDark),
        contentAlignment = Alignment.Center,
    ) {
        val discSize = maxWidth
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation.value }) {
            val outer = size.minDimension / 2f
            val labelR = outer * labelFraction
            // Sillons : anneaux fins alternés, pas proportionnel à la taille du disque
            val period = (outer / 14f).coerceIn(1.5.dp.toPx(), 4.dp.toPx())
            var r = labelR + period
            while (r < outer - period / 2f) {
                drawCircle(SillonGrooveLight, radius = r, style = Stroke(width = period / 2f))
                r += period
            }
            // Reflet : balayage clair très léger
            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent, Color.White.copy(alpha = 0.07f), Color.Transparent,
                        Color.Transparent, Color.White.copy(alpha = 0.05f), Color.Transparent,
                    )
                ),
                radius = outer,
            )
        }
        Box(
            modifier = Modifier
                .size(discSize * labelFraction)
                .graphicsLayer { rotationZ = rotation.value }
                .clip(CircleShape)
                .background(labelColor),
            contentAlignment = Alignment.Center,
        ) {
            when {
                labelContent != null -> labelContent()
                !artworkUrl.isNullOrBlank() -> AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Trou central
            Box(
                Modifier
                    .size((discSize * 0.035f).coerceAtLeast(3.dp))
                    .clip(CircleShape)
                    .background(SillonGrooveDark),
            )
        }
    }
}

/** Anneau de progression circulaire (piste + arc moutarde à bouts ronds). */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 3.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    Canvas(modifier) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2f
        val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        val sweep = 360f * progress.coerceIn(0f, 1f)
        if (sweep > 0f) {
            drawArc(color, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

// ─── Boutons ────────────────────────────────────────────────────────────────────

/** Bouton rond plein (fond surfaceVariant par défaut). */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) contentColor else contentColor.copy(alpha = 0.4f),
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

/** Bouton retour rond (en-têtes de pages secondaires). */
@Composable
fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    RoundIconButton(
        icon = Icons.AutoMirrored.Outlined.ArrowBack,
        contentDescription = "Back",
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

/** Gros bouton play/pause circulaire moutarde. Affiche un spinner si [isLoading]. */
@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    isLoading: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size * 0.4f),
                color = contentColor,
                strokeWidth = 2.5.dp,
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = contentColor,
                modifier = Modifier.size(size * 0.5f),
            )
        }
    }
}

/**
 * Pilule d'action : pleine moutarde (« Play all ») ou à contour (« Shuffle »).
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    outlined: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = if (outlined) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onPrimary,
) {
    val base = modifier
        .height(48.dp)
        .clip(PillShape)
    val styled = if (outlined) {
        base.border(1.5.dp, MaterialTheme.colorScheme.outline, PillShape)
    } else {
        base.background(if (enabled) containerColor else containerColor.copy(alpha = 0.4f))
    }
    Row(
        modifier = styled
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor, maxLines = 1)
    }
}

/** Chip pilule à contour ; sélectionné → fond crème, texte sombre. */
@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val container = if (selected) MaterialTheme.colorScheme.inverseSurface else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onBackground
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(PillShape)
            .background(container)
            .border(1.5.dp, if (selected) container else MaterialTheme.colorScheme.outline, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}

/** Onglets en pilules défilantes : actif → crème, inactif → surface. */
@Composable
fun PillTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(PillShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.inverseSurface
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.inverseOnSurface
                            else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Couleurs moutarde pour un Button M3 plein. À passer via `colors = accentButtonColors()`. */
@Composable
fun accentButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary,
)

// ─── Titres ─────────────────────────────────────────────────────────────────────

/** Titre d'écran — Fraunces italique. */
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
) {
    Text(
        text = text,
        style = style,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** Titre de section — Fraunces 22, avec lien moutarde optionnel à droite (« See all »). */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(PillShape)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        trailing?.invoke(this)
    }
}

/**
 * En-tête de page secondaire : bouton retour rond + petit libellé + actions à droite.
 */
@Composable
fun PageHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackButton(onClick = onBack)
        Text(
            text = label.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

/** Ligne fine arrondie de progression (stockage, import). */
@Composable
fun PillProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/** Champ texte en pilule (fond surface, sans soulignement). */
@Composable
fun PillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    onClear: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    androidx.compose.material3.TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = placeholder?.let { { Text(it, style = MaterialTheme.typography.bodyLarge) } },
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        trailingIcon = if (onClear != null && value.isNotEmpty()) {
            {
                androidx.compose.material3.IconButton(onClick = onClear) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear")
                }
            }
        } else null,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = PillShape,
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

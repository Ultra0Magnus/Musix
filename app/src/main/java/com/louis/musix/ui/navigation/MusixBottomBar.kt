package com.louis.musix.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Barre d'onglets « Affiche » : onglets texte majuscules, sans indicateur pilule.
 * L'onglet actif reçoit un court soulignement vert centré ; angles droits.
 */
@Composable
fun MusixBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            val accent = MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { if (!selected) onNavigate(item.route) }
                    .padding(top = 13.dp, bottom = 24.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = item.label.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    color = if (selected)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (selected) {
                        Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                val barW = 24.dp.toPx()
                                val barH = 2.dp.toPx()
                                drawRect(
                                    color = accent,
                                    topLeft = Offset((size.width - barW) / 2f, size.height + 6.dp.toPx()),
                                    size = Size(barW, barH),
                                )
                            }
                    } else {
                        Modifier.fillMaxWidth()
                    },
                )
            }
        }
    }
}

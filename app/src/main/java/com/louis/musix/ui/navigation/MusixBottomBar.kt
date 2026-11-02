package com.louis.musix.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.louis.musix.ui.theme.PillShape

/**
 * Barre d'onglets « Sillon » : pilule flottante (surface) contenant trois pilules
 * icône + libellé ; l'onglet actif est plein moutarde, texte sombre.
 */
@Composable
fun MusixBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            val container by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                label = "tab_bg",
            )
            val content = if (selected) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(PillShape)
                    .background(container)
                    .clickable { if (!selected) onNavigate(item.route) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(item.icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(item.label, style = MaterialTheme.typography.labelLarge, color = content)
            }
        }
    }
}

package com.louis.musix.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.louis.musix.ui.components.BackButton
import com.louis.musix.ui.components.ScreenTitle
import com.louis.musix.ui.theme.PillShape

/** A single third-party dependency credit. */
private data class License(
    val name: String,
    val author: String,
    val license: String,
)

private val licenses = listOf(
    License("NewPipeExtractor", "TeamNewPipe",        "GPL-3.0"),
    License("Media3 / ExoPlayer", "Google / AndroidX", "Apache-2.0"),
    License("Jetpack Compose",  "Google / AndroidX",  "Apache-2.0"),
    License("Koin",             "InsertKoin",         "Apache-2.0"),
    License("Coil",             "Coil Contributors",  "Apache-2.0"),
    License("OkHttp",           "Square",             "Apache-2.0"),
    License("kotlinx.coroutines", "JetBrains",        "Apache-2.0"),
    License("kotlinx.serialization", "JetBrains",     "Apache-2.0"),
    License("Room",             "Google / AndroidX",  "Apache-2.0"),
    License("Reorderable",      "Calvin Liang",       "Apache-2.0"),
    License("LRCLIB",           "lrclib.net",         "Free API (no key)"),
    License("Fraunces",         "Undercase Type",     "SIL OFL 1.1"),
    License("Figtree",          "Erik Kennedy",       "SIL OFL 1.1"),
)

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                BackButton(onClick = onBack)
                Spacer(Modifier.width(14.dp))
                ScreenTitle("Licenses")
            }
        }
        items(licenses) { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(
                    text  = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text  = "${item.author} · ${item.license}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

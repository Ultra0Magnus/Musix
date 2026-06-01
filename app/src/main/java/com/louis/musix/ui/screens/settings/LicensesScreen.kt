package com.louis.musix.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Source Licenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(licenses) { item ->
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text  = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text  = "${item.author} — ${item.license}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                )
            }
        }
    }
}

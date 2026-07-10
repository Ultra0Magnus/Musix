package com.louis.musix.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MusixDarkColorScheme = darkColorScheme(
    primary           = MusixAccent,
    onPrimary         = MusixAccentOn,
    secondary         = MusixAccentAlt,
    onSecondary       = MusixAccentOn,
    background        = MusixBeton,
    onBackground      = MusixInk,
    surface           = MusixSurface,
    onSurface         = MusixInk,
    surfaceVariant    = MusixSurface,
    onSurfaceVariant  = MusixMuted,
    surfaceContainer  = MusixBloc,
    inverseSurface    = MusixInk,      // fond des boutons pleins « encre » (play, FAB)
    inverseOnSurface  = MusixBeton,    // icône/texte sur bouton encre
    error             = MusixError,
    outline           = MusixDivider,
)

/**
 * Thème de l'application — direction « Affiche », sombre forcé.
 * Béton chaud, angles droits (MusixShapes), typo Anton + Barlow Condensed.
 */
@Composable
fun MusixTheme(content: @Composable () -> Unit) {
    val colorScheme = MusixDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = MusixTypography,
        shapes      = MusixShapes,
        content     = content
    )
}

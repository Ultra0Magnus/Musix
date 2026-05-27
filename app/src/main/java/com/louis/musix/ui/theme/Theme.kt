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
    background        = MusixBlack,
    onBackground      = MusixOnSurface,
    surface           = MusixSurface,
    onSurface         = MusixOnSurface,
    surfaceVariant    = MusixSurfaceVar,
    onSurfaceVariant  = MusixOnSurfaceMuted,
    surfaceContainer  = MusixSurfaceHigh,
    error             = MusixError,
    outline           = MusixDivider,
)

/**
 * Thème de l'application — sombre forcé (musical app, UX classique).
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
        content     = content
    )
}

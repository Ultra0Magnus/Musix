package com.louis.musix.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Angles droits partout (rayon 0) — direction « Affiche ».
 * M3 Shapes exige des CornerBasedShape : on utilise RoundedCornerShape(0.dp).
 * Tous les composants M3 qui lisent MaterialTheme.shapes héritent d'un rectangle net.
 */
private val Square = RoundedCornerShape(0.dp)

val MusixShapes = Shapes(
    extraSmall = Square,
    small      = Square,
    medium     = Square,
    large      = Square,
    extraLarge = Square,
)
